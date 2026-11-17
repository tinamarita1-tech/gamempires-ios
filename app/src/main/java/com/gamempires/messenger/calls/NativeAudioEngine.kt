package com.gamempires.messenger.calls

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal fun createAudioPeerFactory(module: org.webrtc.audio.AudioDeviceModule): PeerConnectionFactory =
    PeerConnectionFactory.builder().setAudioDeviceModule(module)
        // HPB/browser SDP can include video even when we publish only audio. An empty
        // video codec list crashes native WebRTC during remote-offer negotiation.
        .setVideoEncoderFactory(SoftwareVideoEncoderFactory())
        .setVideoDecoderFactory(SoftwareVideoDecoderFactory())
        .createPeerConnectionFactory()

internal fun disableRemoteVideo(connection: PeerConnection) {
    connection.transceivers.forEach { transceiver ->
        val track = transceiver.receiver.track()
        if (track?.kind() == MediaStreamTrack.VIDEO_TRACK_KIND) {
            track.setEnabled(false)
            check(transceiver.setDirection(RtpTransceiver.RtpTransceiverDirection.INACTIVE)) { "Unable to disable remote video" }
        }
    }
}

internal class NativeAudioEngine(
    context: Context,
    private val servers: List<PeerConnection.IceServer>,
    private val dispatch: (() -> Unit) -> Unit,
    private val candidate: (String, String, IceCandidate) -> Unit,
    private val connected: (String) -> Unit,
    private val failed: (String) -> Unit
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val previousMode = audioManager.mode
    private val previousSpeaker = audioManager.isSpeakerphoneOn
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        .setOnAudioFocusChangeListener { if (it == AudioManager.AUDIOFOCUS_LOSS) dispatch { failed("Audio focus was lost") } }.build()
    private val module: JavaAudioDeviceModule
    private val factory: PeerConnectionFactory
    private val source: AudioSource
    private val track: AudioTrack
    private data class Peer(val connection: PeerConnection, val sid: String, var ready: Boolean = false,
        val localCandidates: MutableList<IceCandidate> = mutableListOf(), val remoteCandidates: MutableList<IceCandidate> = mutableListOf())
    private val peers = mutableMapOf<String, Peer>()
    private var closed = false

    init {
        synchronized(NativeAudioEngine::class.java) {
            if (!initialized) {
                PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context.applicationContext).createInitializationOptions())
                initialized = true
            }
        }
        val rollback = mutableListOf<() -> Unit>()
        try {
            val createdModule = JavaAudioDeviceModule.builder(context.applicationContext).createAudioDeviceModule()
            rollback.add { createdModule.release() }
            val createdFactory = createAudioPeerFactory(createdModule)
            rollback.add { createdFactory.dispose() }
            val createdSource = createdFactory.createAudioSource(MediaConstraints())
            rollback.add { createdSource.dispose() }
            val createdTrack = createdFactory.createAudioTrack("gamempires-microphone", createdSource)
            rollback.add { createdTrack.dispose() }
            check(audioManager.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { "Audio is busy in another app" }
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = false
            module = createdModule; factory = createdFactory; source = createdSource; track = createdTrack
        } catch (error: Throwable) {
            rollback.asReversed().forEach { runCatching { it() } }
            audioManager.isSpeakerphoneOn = previousSpeaker
            audioManager.mode = previousMode
            audioManager.abandonAudioFocusRequest(focus)
            throw IOException("Unable to start native audio: ${error.message}", error)
        }
    }

    private fun peer(key: String, sid: String, publish: Boolean): Peer {
        peers[key]?.let { return it }
        val config = PeerConnection.RTCConfiguration(servers).apply { sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN }
        val connection = factory.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                dispatch { if (!closed) when (state) {
                    PeerConnection.IceConnectionState.CONNECTED, PeerConnection.IceConnectionState.COMPLETED -> connected(key)
                    PeerConnection.IceConnectionState.FAILED -> failed("Audio could not connect. Check TURN and firewall connectivity.")
                    else -> Unit
                } }
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
            override fun onIceCandidate(value: IceCandidate) { dispatch {
                peers[key]?.takeIf { !closed }?.let { if (it.ready) candidate(key, sid, value) else it.localCandidates.add(value) }
            } }
            override fun onIceCandidatesRemoved(values: Array<out IceCandidate>) {}
            override fun onAddStream(stream: MediaStream) { stream.audioTracks.forEach { it.setEnabled(true) } }
            override fun onRemoveStream(stream: MediaStream) {}
            override fun onDataChannel(channel: DataChannel) { channel.close(); channel.dispose() }
            override fun onRenegotiationNeeded() {}
        }) ?: throw IOException("Unable to initialize audio connection")
        if (publish) connection.addTrack(track, listOf("gamempires-audio"))
        return Peer(connection, sid).also { peers[key] = it }
    }

    suspend fun offer(key: String): Pair<String, String> {
        val value = peer(key, UUID.randomUUID().toString(), true)
        val description = create(value.connection, true)
        set(value.connection, description, true)
        return value.sid to description.description
    }

    suspend fun receiveOffer(key: String, sid: String, sdp: String): String {
        val value = peer(key, sid, false)
        set(value.connection, SessionDescription(SessionDescription.Type.OFFER, sdp), false)
        disableRemoteVideo(value.connection)
        drain(value)
        val answer = create(value.connection, false)
        set(value.connection, answer, true)
        return answer.description
    }

    suspend fun receiveAnswer(key: String, sdp: String) {
        val value = peers[key] ?: throw IOException("Missing audio session")
        set(value.connection, SessionDescription(SessionDescription.Type.ANSWER, sdp), false)
        drain(value)
    }

    fun remoteCandidate(key: String, value: IceCandidate) {
        peers[key]?.let { if (it.connection.remoteDescription == null) it.remoteCandidates.add(value) else it.connection.addIceCandidate(value) }
    }
    private fun drain(peer: Peer) { peer.remoteCandidates.forEach { peer.connection.addIceCandidate(it) }; peer.remoteCandidates.clear() }
    fun signalingReady(key: String) { peers[key]?.let { peer ->
        peer.ready = true
        peer.localCandidates.forEach { candidate(key, peer.sid, it) }; peer.localCandidates.clear()
    } }
    fun hasPeer(key: String) = peers.containsKey(key)
    fun mute(muted: Boolean) { track.setEnabled(!muted) }
    fun speaker(enabled: Boolean) { audioManager.isSpeakerphoneOn = enabled }
    fun close() {
        if (closed) return
        closed = true
        peers.values.forEach { it.connection.close(); it.connection.dispose() }; peers.clear()
        track.dispose(); source.dispose(); factory.dispose(); module.release()
        audioManager.isSpeakerphoneOn = previousSpeaker
        audioManager.mode = previousMode
        audioManager.abandonAudioFocusRequest(focus)
    }

    private suspend fun create(connection: PeerConnection, offer: Boolean): SessionDescription = suspendCancellableCoroutine { cont ->
        val observer = object : SdpObserver {
            override fun onCreateSuccess(value: SessionDescription) { if (cont.isActive) cont.resume(value) }
            override fun onCreateFailure(error: String) { if (cont.isActive) cont.resumeWithException(IOException(error)) }
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String) {}
        }
        if (offer) connection.createOffer(observer, MediaConstraints()) else connection.createAnswer(observer, MediaConstraints())
    }
    private suspend fun set(connection: PeerConnection, value: SessionDescription, local: Boolean): Unit = suspendCancellableCoroutine { cont ->
        val observer = object : SdpObserver {
            override fun onCreateSuccess(value: SessionDescription) {}
            override fun onCreateFailure(error: String) {}
            override fun onSetSuccess() { if (cont.isActive) cont.resume(Unit) }
            override fun onSetFailure(error: String) { if (cont.isActive) cont.resumeWithException(IOException(error)) }
        }
        if (local) connection.setLocalDescription(observer, value) else connection.setRemoteDescription(observer, value)
    }
    companion object { private var initialized = false }
}
