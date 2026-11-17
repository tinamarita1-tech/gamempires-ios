package com.gamempires.messenger.calls

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.Ringtone
import android.media.RingtoneManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamempires.messenger.data.nextcloud.auth.NextcloudAuthStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import java.io.IOException
import javax.inject.Inject

enum class AudioCallPhase { IDLE, INCOMING, PERMISSION, CONNECTING, WAITING, ACTIVE, ENDED }
data class AudioCallState(
    val phase: AudioCallPhase = AudioCallPhase.IDLE,
    val token: String = "", val name: String = "", val message: String = "",
    val muted: Boolean = false, val speaker: Boolean = false
)

@HiltViewModel
class AudioCallViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val auth: NextcloudAuthStorage,
    private val http: OkHttpClient,
    private val pushNotifier: com.gamempires.messenger.push.PushNotifier
) : ViewModel() {
    private val mutableState = MutableStateFlow(AudioCallState())
    val state = mutableState.asStateFlow()
    private var monitor: Job? = null
    private var startup: Job? = null
    private var messageJob: Job? = null
    private var inbox: Channel<String>? = null
    private var maintenance: Job? = null
    private var timeout: Job? = null
    private var cleanupJob: Job? = null
    private var foreground = false
    private var generation = 0
    private var transport: TalkCallTransport? = null
    private var socket: WebSocket? = null
    private var engine: NativeAudioEngine? = null
    private var ownSession = ""
    private var joinedToken = ""
    private var joinedCall = false
    private var ringtone: Ringtone? = null
    private val dismissed = mutableSetOf<String>()
    private val remoteSessions = mutableSetOf<String>()
    private var remoteWasPresent = false
    /** A call opened from a push notification, handled once the app is in the foreground. */
    private var pendingPushCall: Triple<String, String, Boolean>? = null

    init {
        viewModelScope.launch {
            auth.credentials.drop(1).collect {
                endCall()
                dismissed.clear()
            }
        }
        // Hang up pressed on the ongoing-call notification.
        viewModelScope.launch { CallControl.hangups.collect { endCall() } }
        // Keep the app (and microphone) alive in the background while a call is connecting or active.
        viewModelScope.launch {
            state.map { if (it.phase in LIVE_PHASES) it.name else null }
                .distinctUntilChanged()
                .collect { name ->
                    if (name != null) OngoingCallService.start(context, name) else OngoingCallService.stop(context)
                }
        }
    }

    fun setForeground(value: Boolean) {
        if (foreground == value) return
        foreground = value
        monitor?.cancel()
        if (!value) {
            // A call that is connecting or active keeps running in the background (ongoing-call service);
            // anything else (ringing in the app, ended-call message) is cleared as before.
            if (mutableState.value.phase !in LIVE_PHASES) endCall()
            return
        }
        pendingPushCall?.let { (token, name, autoAnswer) -> pendingPushCall = null; openFromPush(token, name, autoAnswer) }
        monitor = viewModelScope.launch {
            val transitions = IncomingCallTransitions()
            val pendingIncoming = mutableSetOf<String>()
            while (isActive) {
                try {
                    val credentials = auth.credentials.value
                    if (credentials != null && mutableState.value.phase in listOf(AudioCallPhase.IDLE, AudioCallPhase.INCOMING)) {
                        val incomingApi = TalkCallTransport(http, credentials)
                        val rooms = incomingApi.request("GET", "ocs/v2.php/apps/spreed/api/v4/room",
                            mapOf("noStatusUpdate" to "1")) as? JSONArray ?: JSONArray()
                        val active = (0 until rooms.length()).map { rooms.getJSONObject(it) }
                        val activeTokens = active.filter { it.optBoolean("hasCall") }.map { it.optString("token") }.toSet()
                        pendingIncoming.addAll(transitions.update(activeTokens))
                        pendingIncoming.retainAll(activeTokens)
                        dismissed.retainAll(activeTokens)
                        val incoming = active.firstOrNull { shouldRing(it.optInt("type"), it.optBoolean("hasCall"),
                            it.optInt("participantFlags"), it.optString("token") in dismissed ||
                                (it.optString("token") !in pendingIncoming &&
                                    !(state.value.phase == AudioCallPhase.INCOMING && state.value.token == it.optString("token")))) }
                        val participants = if (incoming != null) incomingApi.request("GET",
                            "ocs/v2.php/apps/spreed/api/v4/call/${incoming.optString("token")}") as? JSONArray else null
                        val remoteInCall = participants != null && (0 until participants.length()).any {
                            val user = participants.getJSONObject(it)
                            user.optString("actorId", user.optString("userId")) != credentials.loginName
                        }
                        if (mutableState.value.phase == AudioCallPhase.INCOMING &&
                            (mutableState.value.token !in activeTokens || !remoteInCall)) {
                            stopRinging(); pushNotifier.cancelCallAlerts(); mutableState.value = AudioCallState()
                        } else if (mutableState.value.phase == AudioCallPhase.IDLE && incoming != null && remoteInCall) {
                            pendingIncoming.remove(incoming.optString("token"))
                            mutableState.value = AudioCallState(AudioCallPhase.INCOMING, incoming.optString("token"), incoming.optString("displayName"))
                            ringtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))?.apply {
                                isLooping = true; play()
                            }
                        }
                    }
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { android.util.Log.w("AudioCall", "Incoming call check failed: ${e.javaClass.simpleName}") }
                delay(3000)
            }
        }
    }

    fun dial(token: String, name: String, direct: Boolean) {
        if (mutableState.value.phase != AudioCallPhase.IDLE) return
        if (!direct) {
            mutableState.value = AudioCallState(AudioCallPhase.ENDED, message = "This first version supports one-to-one audio calls.")
            return
        }
        mutableState.value = AudioCallState(AudioCallPhase.CONNECTING, token, name)
        checkMicrophone()
    }
    /**
     * Incoming call opened from the push notification. The call may have been ringing for a while, so the
     * normal "new call" detection would ignore it; check it is still live, then ring (or answer) in the app.
     */
    fun openFromPush(token: String, name: String, autoAnswer: Boolean) {
        if (!foreground) { pendingPushCall = Triple(token, name, autoAnswer); return }
        val phase = mutableState.value.phase
        if (phase == AudioCallPhase.INCOMING && mutableState.value.token == token) { if (autoAnswer) answer(); return }
        if (phase != AudioCallPhase.IDLE) return
        val credentials = auth.credentials.value ?: return
        viewModelScope.launch {
            val room = try {
                TalkCallTransport(http, credentials).request("GET", "ocs/v2.php/apps/spreed/api/v4/room/$token") as? JSONObject
            } catch (e: CancellationException) { throw e } catch (e: Exception) { null }
            val live = room?.optBoolean("hasCall") == true
            if (mutableState.value.phase != AudioCallPhase.IDLE) return@launch
            if (live && room?.optInt("type") != 1) {
                mutableState.value = AudioCallState(AudioCallPhase.ENDED, token, name, message = "This first version supports one-to-one audio calls.")
                return@launch
            }
            if (!live) {
                pushNotifier.cancelCallAlerts()
                mutableState.value = AudioCallState(AudioCallPhase.ENDED, token, name, message = "This call has already ended.")
                return@launch
            }
            dismissed.remove(token)
            val title = name.ifBlank { "Incoming call" }
            if (autoAnswer) {
                mutableState.value = AudioCallState(AudioCallPhase.CONNECTING, token, title)
                checkMicrophone()
            } else {
                // The call notification keeps ringing (also while the phone is locked) until Answer/Decline.
                mutableState.value = AudioCallState(AudioCallPhase.INCOMING, token, title)
            }
        }
    }

    fun answer() { if (mutableState.value.phase == AudioCallPhase.INCOMING) { stopRinging(); pushNotifier.cancelCallAlerts(); checkMicrophone() } }
    private fun checkMicrophone() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) connect()
        else mutableState.update { it.copy(phase = AudioCallPhase.PERMISSION) }
    }
    fun permissionResult(granted: Boolean) {
        if (mutableState.value.phase != AudioCallPhase.PERMISSION) return
        if (granted && foreground) connect() else endCall("Microphone permission is required for audio calls.")
    }
    fun dismiss() {
        val current = mutableState.value
        if (current.phase == AudioCallPhase.INCOMING) {
            pushNotifier.cancelCallAlerts()
            // Decline: end the one-to-one call so the caller stops ringing too.
            auth.credentials.value?.let { creds ->
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    runCatching { withTimeout(8000) { declineDirectCall(TalkCallTransport(http, creds), current.token) } }
                        .onFailure { CallDiagnostics.record(context, "Decline could not end the call: ${it.message}") }
                }
            }
        }
        dismissed.add(current.token); endCall(); mutableState.value = AudioCallState()
    }
    fun mute() { val muted = !state.value.muted; engine?.mute(muted); mutableState.update { it.copy(muted = muted) } }
    fun speaker() { val speaker = !state.value.speaker; engine?.speaker(speaker); mutableState.update { it.copy(speaker = speaker) } }
    private fun stopRinging() { ringtone?.stop(); ringtone = null }

    private fun connect() {
        val credentials = auth.credentials.value ?: run { endCall("Sign in before calling."); return }
        val token = state.value.token
        if (!foreground || token.isBlank()) return
        dismissed.add(token)
        mutableState.update { it.copy(phase = AudioCallPhase.CONNECTING, message = "Connecting…") }
        val current = ++generation
        CallDiagnostics.record(context, "call=$current starting foreground audio")
        val api = TalkCallTransport(http, credentials)
        transport = api
        val previousCleanup = cleanupJob
        startup = viewModelScope.launch {
            try {
                previousCleanup?.join()
                val settings = api.request("GET", "ocs/v2.php/apps/spreed/api/v3/signaling/settings", mapOf("token" to token)) as JSONObject
                val server = settings.optString("server")
                if (server.isBlank()) throw IOException("The Talk high-performance calling backend is required by this build.")
                val ice = mutableListOf<PeerConnection.IceServer>()
                for (field in listOf("stunservers", "turnservers")) {
                    val list = settings.optJSONArray(field) ?: continue
                    for (i in 0 until list.length()) {
                        val item = list.getJSONObject(i)
                        val urls = item.optJSONArray("urls")
                        val values = if (urls != null) (0 until urls.length()).map { urls.getString(it) }
                            else listOfNotNull(item.optString("urls").takeIf { it.isNotBlank() })
                        if (values.isNotEmpty()) ice.add(PeerConnection.IceServer.builder(values)
                            .setUsername(item.optString("username")).setPassword(item.optString("credential")).createIceServer())
                    }
                }
                val room = api.request("POST", "ocs/v2.php/apps/spreed/api/v4/room/$token/participants/active", mapOf("force" to "false")) as JSONObject
                joinedToken = token
                val nextcloudSession = room.optString("sessionId")
                if (nextcloudSession.isBlank() || nextcloudSession == "0") throw IOException("Server did not create a call session")
                engine = NativeAudioEngine(context, ice, dispatch = { block -> viewModelScope.launch {
                    if (current == generation) {
                        try { block() }
                        catch (e: CancellationException) { throw e }
                        catch (e: Exception) {
                            android.util.Log.e("AudioCall", "Audio callback failed", e)
                            CallDiagnostics.record(context, "call=$current callback failure\n${e.stackTraceToString()}")
                            endCall("Audio connection stopped: ${e.message ?: e.javaClass.simpleName}")
                        }
                    }
                } },
                    candidate = { peer, sid, candidate -> signal(peer, "candidate", sid, JSONObject().put("candidate", JSONObject()
                        .put("candidate", candidate.sdp).put("sdpMid", candidate.sdpMid).put("sdpMLineIndex", candidate.sdpMLineIndex))) },
                    connected = { peer ->
                        CallDiagnostics.record(context, "call=$current ICE connected role=${if (peer == ownSession) "publisher" else "receiver"}")
                        if (peer != ownSession) mutableState.update { it.copy(phase = AudioCallPhase.ACTIVE, message = "Connected") }
                    },
                    failed = { endCall(it) })
                val hello = CompletableDeferred<JSONObject>()
                val joined = CompletableDeferred<Unit>()
                var socketUrl = server.trimEnd('/')
                if (!socketUrl.endsWith("/spreed")) socketUrl += "/spreed"
                socketUrl = socketUrl.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://")
                if (!socketUrl.startsWith("wss://")) throw IOException("Calling backend must use secure WebSocket (wss)")
                val messages = Channel<String>(Channel.UNLIMITED)
                inbox = messages
                messageJob = viewModelScope.launch {
                    for (text in messages) {
                        if (current != generation) break
                        try {
                            val message = JSONObject(text)
                            when (message.optString("type")) {
                                "hello" -> hello.complete(message.getJSONObject("hello"))
                                "room" -> joined.complete(Unit)
                                "error" -> {
                                    val error = message.getJSONObject("error")
                                    // An existing participant may not have published audio yet; retry its subscription later.
                                    if (error.optString("code") !in listOf("client_not_found", "no_such_publisher"))
                                        throw IOException("Calling backend: ${error.optString("code")}")
                                }
                                "message" -> receiveSignal(message.getJSONObject("message"))
                                "event" -> receiveEvent(message.getJSONObject("event"))
                                "bye" -> throw IOException("Call session closed by server")
                            }
                            CallDiagnostics.record(context, "call=$current received=${message.optString("type")}")
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) { hello.completeExceptionally(e); joined.completeExceptionally(e); endCall(e.message ?: "Call signaling failed") }
                    }
                }
                socket = api.client.newWebSocket(Request.Builder().url(socketUrl).build(), object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        val v2 = settings.optJSONObject("helloAuthParams")?.optJSONObject("2.0")
                        val params = v2 ?: JSONObject().put("userid", settings.optString("userId", credentials.loginName)).put("ticket", settings.optString("ticket"))
                        webSocket.send(JSONObject().put("type", "hello").put("hello", JSONObject()
                            .put("version", if (v2 != null) "2.0" else "1.0")
                            .put("auth", JSONObject().put("url", api.base + "ocs/v2.php/apps/spreed/api/v3/signaling/backend").put("params", params))).toString())
                    }
                    override fun onMessage(webSocket: WebSocket, text: String) { messages.trySend(text) }
                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { viewModelScope.launch {
                        if (current == generation) { hello.completeExceptionally(t); joined.completeExceptionally(t); endCall("Calling backend disconnected") }
                    } }
                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { viewModelScope.launch {
                        if (current == generation) endCall("Call connection closed")
                    } }
                })
                ownSession = withTimeout(15000) { hello.await() }.getString("sessionid")
                send(JSONObject().put("type", "room").put("room", JSONObject().put("roomid", token).put("sessionid", nextcloudSession)))
                withTimeout(15000) { joined.await() }
                api.request("POST", "ocs/v2.php/apps/spreed/api/v4/call/$token", mapOf("flags" to AUDIO_CALL_FLAGS.toString()))
                joinedCall = true
                mutableState.update { it.copy(phase = AudioCallPhase.WAITING, message = "Waiting for the other person…") }
                val offer = engine!!.offer(ownSession)
                signal(ownSession, "offer", offer.first, JSONObject().put("type", "offer").put("sdp", offer.second))
                engine?.signalingReady(ownSession)
                maintenance = viewModelScope.launch {
                    try {
                        while (isActive && current == generation) {
                            remoteSessions.toList().filter { engine?.hasPeer(it) == false }.forEach { signal(it, "requestoffer") }
                            delay(4000)
                        }
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { if (current == generation) endCall("Audio signaling stopped") }
                }
                timeout = viewModelScope.launch { delay(60000); if (state.value.phase != AudioCallPhase.ACTIVE) endCall("No answer or audio could not connect.") }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (current == generation) endCall(e.message ?: "Unable to start audio call") }
        }
    }

    private fun send(message: JSONObject) { if (socket?.send(message.toString()) != true) throw IOException("Call signaling connection is unavailable") }
    private fun signal(to: String, type: String, sid: String? = null, payload: JSONObject? = null) {
        val data = JSONObject().put("to", to).put("roomType", "video").put("type", type)
        if (sid != null) data.put("sid", sid)
        if (payload != null) data.put("payload", payload)
        send(JSONObject().put("type", "message").put("message", JSONObject()
            .put("recipient", JSONObject().put("type", "session").put("sessionid", to)).put("data", data)))
    }
    private suspend fun receiveSignal(message: JSONObject) {
        val from = message.getJSONObject("sender").optString("sessionid")
        val data = message.optJSONObject("data") ?: return
        if (from.isBlank() || data.optString("roomType", "video") != "video") return
        val payload = data.optJSONObject("payload") ?: JSONObject()
        CallDiagnostics.record(context, "call=$generation media signal=${data.optString("type")}")
        when (data.optString("type")) {
            "offer" -> {
                val sid = data.getString("sid")
                val sdp = engine?.receiveOffer(from, sid, payload.getString("sdp")) ?: return
                signal(from, "answer", sid, JSONObject().put("type", "answer").put("sdp", sdp))
                engine?.signalingReady(from)
            }
            "answer" -> engine?.receiveAnswer(from, payload.getString("sdp"))
            "candidate" -> payload.optJSONObject("candidate")?.let {
                engine?.remoteCandidate(from, IceCandidate(it.optString("sdpMid"), it.optInt("sdpMLineIndex"), it.getString("candidate")))
            }
        }
    }
    private fun receiveEvent(event: JSONObject) {
        if (event.optString("target") != "participants" || event.optString("type") != "update") return
        val update = event.optJSONObject("update") ?: return
        if (update.optBoolean("all") && update.optInt("incall", -1) == 0) { endCall("Call ended"); return }
        val users = update.optJSONArray("users") ?: return
        remoteSessions.clear()
        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            val id = user.optString("sessionId")
            if (id.isNotBlank() && id != ownSession && (user.optInt("inCall") and 2) != 0) remoteSessions.add(id)
        }
        if (remoteSessions.isNotEmpty()) remoteWasPresent = true
        else if (remoteWasPresent) endCall("The other person left the call.")
    }

    fun endCall(message: String = "") {
        CallDiagnostics.record(context, "call=$generation ending phase=${state.value.phase} reason=$message")
        ++generation
        stopRinging()
        startup?.cancel(); startup = null
        messageJob?.cancel(); messageJob = null
        inbox?.close(); inbox = null
        maintenance?.cancel(); maintenance = null
        timeout?.cancel(); timeout = null
        val api = transport
        val token = joinedToken
        val endForBoth = joinedCall
        joinedCall = false
        if (token.isNotBlank()) dismissed.add(token)
        joinedToken = ""; transport = null
        val closingSocket = socket
        socket = null
        engine?.close(); engine = null
        ownSession = ""; remoteSessions.clear(); remoteWasPresent = false
        if (api != null && token.isNotBlank()) cleanupJob = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
            if (endForBoth) runCatching {
                withTimeout(10000) {
                    // Calls in this first version are strictly one-to-one. Hang up
                    // both sides, rather than leave the web participant in the room.
                    api.request("DELETE", "ocs/v2.php/apps/spreed/api/v4/call/$token", leaveAudioCallFields(direct = true))
                }
            }.onFailure {
                CallDiagnostics.record(context, "Direct call hang-up failed: ${it.message}")
                runCatching { withTimeout(5000) { api.request("DELETE", "ocs/v2.php/apps/spreed/api/v4/call/$token") } }
            }
            runCatching { withTimeout(10000) { api.request("DELETE", "ocs/v2.php/apps/spreed/api/v4/room/$token/participants/active") } }
            } finally {
                // HPB bye can delete the PHP participant session. Keep it alive
                // until the authenticated HTTP hang-up has ended the direct call.
                runCatching { closingSocket?.send(JSONObject().put("type", "bye").put("bye", JSONObject()).toString()) }
                closingSocket?.close(1000, "Call ended")
                coroutineContext.cancel()
            }
        } else {
            closingSocket?.close(1000, "Call ended")
        }
        mutableState.value = if (message.isBlank()) AudioCallState() else state.value.copy(phase = AudioCallPhase.ENDED, message = message)
    }
    override fun onCleared() { endCall(); OngoingCallService.stop(context); super.onCleared() }

    private companion object {
        val LIVE_PHASES = setOf(AudioCallPhase.CONNECTING, AudioCallPhase.WAITING, AudioCallPhase.ACTIVE)
    }
}

