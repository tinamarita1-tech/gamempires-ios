package com.gamempires.messenger.calls

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnectionFactory
import org.webrtc.audio.JavaAudioDeviceModule
import org.webrtc.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class NativeCodecTest {
    @Test fun browserAudioVideoOfferCanBeAnsweredWithAudioEnabledAndVideoInactive() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions())
        val module = JavaAudioDeviceModule.builder(context).createAudioDeviceModule()
        val factory = createAudioPeerFactory(module)
        val config = PeerConnection.RTCConfiguration(emptyList()).apply { sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN }
        val browser = factory.createPeerConnection(config, Observer())!!
        val phone = factory.createPeerConnection(config, Observer())!!
        try {
            browser.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
                RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_ONLY))
            browser.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
                RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_ONLY))
            val offer = description(browser, true)
            assertTrue(offer.description.contains("m=video"))
            set(browser, offer, true)
            set(phone, offer, false)
            disableRemoteVideo(phone)
            val answer = description(phone, false)
            set(phone, answer, true)
            val transceivers = phone.transceivers
            assertTrue(transceivers.any { it.receiver.track()?.kind() == MediaStreamTrack.AUDIO_TRACK_KIND &&
                it.direction != RtpTransceiver.RtpTransceiverDirection.INACTIVE })
            assertTrue(transceivers.any { it.receiver.track()?.kind() == MediaStreamTrack.VIDEO_TRACK_KIND &&
                it.direction == RtpTransceiver.RtpTransceiverDirection.INACTIVE })
            set(browser, answer, false)
        } finally { browser.dispose(); phone.dispose(); factory.dispose(); module.release() }
    }

    private fun description(peer: PeerConnection, offer: Boolean): SessionDescription {
        val done = CountDownLatch(1)
        val value = AtomicReference<SessionDescription>()
        val error = AtomicReference<String>()
        val observer = object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) { value.set(sdp); done.countDown() }
            override fun onCreateFailure(message: String) { error.set(message); done.countDown() }
            override fun onSetSuccess() {}
            override fun onSetFailure(message: String) {}
        }
        if (offer) peer.createOffer(observer, MediaConstraints()) else peer.createAnswer(observer, MediaConstraints())
        assertTrue("SDP creation timed out", done.await(15, TimeUnit.SECONDS))
        if (error.get() != null) throw AssertionError(error.get())
        return value.get() ?: throw AssertionError("Missing SDP")
    }
    private fun set(peer: PeerConnection, sdp: SessionDescription, local: Boolean) {
        val done = CountDownLatch(1)
        val error = AtomicReference<String>()
        val observer = object : SdpObserver {
            override fun onCreateSuccess(value: SessionDescription) {}
            override fun onCreateFailure(message: String) {}
            override fun onSetSuccess() { done.countDown() }
            override fun onSetFailure(message: String) { error.set(message); done.countDown() }
        }
        if (local) peer.setLocalDescription(observer, sdp) else peer.setRemoteDescription(observer, sdp)
        assertTrue("SDP application timed out", done.await(15, TimeUnit.SECONDS))
        if (error.get() != null) throw AssertionError(error.get())
    }
    private class Observer : PeerConnection.Observer {
        override fun onSignalingChange(state: PeerConnection.SignalingState) {}
        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {}
        override fun onIceConnectionReceivingChange(receiving: Boolean) {}
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
        override fun onIceCandidate(candidate: IceCandidate) {}
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
        override fun onAddStream(stream: MediaStream) {}
        override fun onRemoveStream(stream: MediaStream) {}
        override fun onDataChannel(channel: DataChannel) {}
        override fun onRenegotiationNeeded() {}
    }

    @Test fun audioFactoryCanNegotiateBrowserVideoOfferWithoutAnEmptyCodecList() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions())
        val module = JavaAudioDeviceModule.builder(context).createAudioDeviceModule()
        val factory = createAudioPeerFactory(module)
        try {
            assertTrue("Audio codecs are required", factory.getRtpReceiverCapabilities(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO).codecs.isNotEmpty())
            assertTrue("Browser/HPB offers can contain video even for audio-only clients; an empty codec list aborts native negotiation",
                factory.getRtpReceiverCapabilities(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO).codecs.isNotEmpty())
        } finally { factory.dispose(); module.release() }
    }
}
