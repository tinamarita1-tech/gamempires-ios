package com.gamempires.messenger.push

import android.util.Log
import com.gamempires.messenger.data.nextcloud.auth.NextcloudAuthStorage
import com.gamempires.messenger.data.nextcloud.interceptor.NextcloudAuthInterceptor
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Receives the encrypted Nextcloud push forwarded by the GamEmpires push gateway, checks it was
 * signed by our Nextcloud account key, decrypts it with this phone's private key and shows it.
 */
@AndroidEntryPoint
class PushMessagingService : FirebaseMessagingService() {
    @Inject lateinit var registrar: PushRegistrar
    @Inject lateinit var store: PushStore
    @Inject lateinit var notifier: PushNotifier
    @Inject lateinit var auth: NextcloudAuthStorage
    @Inject lateinit var baseClient: OkHttpClient
    @Inject lateinit var sessionGuard: SessionGuard

    override fun onNewToken(token: String) {
        registrar.onNewToken()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        diag("push arrived: fcmPriority=${message.priority} (original ${message.originalPriority}) " +
            "sentAgo=${(System.currentTimeMillis() - message.sentTime) / 1000}s gatewayType=${message.data["type"]}")
        // The account was signed in on another phone: confirm with the gateway, then sign out here.
        if (message.data["gamempires"] == "logout") {
            runBlocking { withTimeoutOrNull(20_000) { sessionGuard.check() } }
            return
        }
        val subject = message.data["subject"] ?: return
        val signature = message.data["signature"] ?: return
        // onMessageReceived already runs on a background thread; short blocking work is allowed here.
        runBlocking {
            val userKey = store.userPublicKey()
            val deviceKey = store.devicePrivateKey()
            if (userKey == null || deviceKey == null) {
                Log.w(TAG, "Push received but this phone is not registered")
                return@runBlocking
            }
            val json = runCatching { PushCrypto.verifyAndDecrypt(subject, signature, userKey, deviceKey) }
                .onFailure { Log.w(TAG, "Push decrypt failed: ${it.javaClass.simpleName}") }
                .getOrNull()
            if (json == null) {
                Log.w(TAG, "Push rejected: signature does not match this account")
                return@runBlocking
            }
            handle(JSONObject(json))
        }
    }

    private suspend fun handle(push: JSONObject) {
        when {
            push.optBoolean("delete-all") -> {
                diag("delete-all")
                val ringing = notifier.ringingCalls()
                notifier.cancelAll()
                // Re-check ringing calls instead of silencing them blindly.
                ringing.forEach { (nid, token) -> keepRingingIfLive(nid, token) }
            }
            push.optBoolean("delete-multiple") -> {
                val ids = push.optJSONArray("nids")
                if (ids != null) for (i in 0 until ids.length()) deleteOne(ids.optInt(i))
            }
            push.optBoolean("delete") -> deleteOne(push.optInt("nid"))
            else -> show(push)
        }
    }

    /**
     * Nextcloud clears call notifications for several reasons (re-ring, call answered elsewhere, call ended).
     * Only stop ringing when the server confirms the call is really over; if the phone cannot check
     * (no network in the background), keep ringing until the caller hangs up or the 45 s ring timeout.
     */
    private suspend fun deleteOne(nid: Int) {
        val token = notifier.ringingCallToken(nid)
        if (token == null) { diag("delete nid=$nid"); notifier.cancel(nid); return }
        val live = withTimeoutOrNull(8000) { callIsLive(token) }
        diag("delete for ringing call nid=$nid: live=$live -> ${if (live == false) "stop ringing" else "keep ringing"}")
        if (live == false) notifier.cancel(nid)
    }

    private suspend fun keepRingingIfLive(nid: Int, token: String) {
        val live = withTimeoutOrNull(8000) { callIsLive(token) }
        diag("re-check after delete-all nid=$nid: live=$live")
        if (live != false) notifier.showIncomingCall(nid, token, "Incoming call")
    }

    private suspend fun show(push: JSONObject) {
        val nid = push.optInt("nid", (System.currentTimeMillis() % Int.MAX_VALUE).toInt())
        val app = push.optString("app")
        val type = push.optString("type")
        val roomToken = push.optString("id").takeIf { it.isNotBlank() }
        val subject = push.optString("subject").ifBlank { "New notification" }

        if (app == "spreed" && type == "call" && roomToken != null) {
            val looksMissed = subject.contains("missed", ignoreCase = true)
            // 1) Ring straight away (no network wait first) unless it is clearly a "missed call" notice.
            val rangNow = !looksMissed && !PushIntents.appInForeground
            if (rangNow) diag("call alert nid=$nid: " + notifier.showIncomingCall(nid, roomToken, subject))
            diag("call push nid=$nid: missedText=$looksMissed appOpen=${PushIntents.appInForeground} -> ${if (rangNow) "RINGING" else "no ring yet"}")
            // 2) Then confirm with the server. Stop only when it says the call is over.
            val live = withTimeoutOrNull(8000) { callIsLive(roomToken) }
            diag("call check nid=$nid: live=$live")
            if (live == false) {
                notifier.cancelCallAlerts()
                // fall through: show it as a normal (missed call) notification
            } else {
                // live or unknown: an already-ringing alert keeps ringing (45 s timeout or until the call ends)
                if (!rangNow && live == true && !PushIntents.appInForeground) notifier.showIncomingCall(nid, roomToken, subject)
                return
            }
        } else {
            diag("push: app=$app type=$type")
        }

        // Fetch the full notification (message text) from Nextcloud; fall back to the push subject.
        val details = withTimeoutOrNull(5000) { fetchDetails(nid) }
        val title = details?.optString("subject")?.takeIf { it.isNotBlank() } ?: subject
        val text = details?.optString("message")?.takeIf { it.isNotBlank() } ?: if (title == subject) "" else subject
        notifier.showMessage(nid, title, text.ifBlank { "Open GamEmpires Messenger to read it." },
            roomToken.takeIf { app == "spreed" })
    }

    /**
     * true = one-to-one call running (ring), false = no call or a group call (this version only answers
     * one-to-one calls, so those show as a normal notification), null = could not check (ring to be safe).
     */
    private suspend fun callIsLive(token: String): Boolean? {
        val creds = auth.credentials.value
            ?: withTimeoutOrNull(6000) { auth.credentials.filterNotNull().first() }
            ?: run { diag("call check: saved login not loaded in time"); return null }
        return withContext(Dispatchers.IO) {
            runCatching {
                val room = ocsGet(creds, "ocs/v2.php/apps/spreed/api/v4/room/$token") ?: return@runCatching null
                room.optBoolean("hasCall") && room.optInt("type") == ONE_TO_ONE
            }.onFailure { diag("call check failed: ${it.javaClass.simpleName}: ${it.message?.take(120)}") }.getOrNull()
        }
    }

    private suspend fun fetchDetails(nid: Int): JSONObject? {
        // In a cold start the saved login is still loading; wait briefly for it.
        val creds = auth.credentials.value
            ?: withTimeoutOrNull(6000) { auth.credentials.filterNotNull().first() }
            ?: return null
        return withContext(Dispatchers.IO) {
            runCatching { ocsGet(creds, "ocs/v2.php/apps/notifications/api/v2/notifications/$nid") }.getOrNull()
        }
    }

    private fun ocsGet(creds: com.gamempires.messenger.data.nextcloud.auth.NextcloudCredentials, path: String): JSONObject? {
        val client = baseClient.newBuilder().apply {
            interceptors().removeAll { it is NextcloudAuthInterceptor }
        }.callTimeout(5, TimeUnit.SECONDS).build()
        val request = Request.Builder()
            .url("${creds.serverUrl.trimEnd('/')}/$path")
            .header("Authorization", Credentials.basic(creds.loginName, creds.appPassword))
            .header("OCS-APIRequest", "true")
            .header("Accept", "application/json")
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) { diag("server answered HTTP ${response.code} for ${path.substringBefore('?').takeLast(40)}"); null }
            else JSONObject(response.body.string()).optJSONObject("ocs")?.optJSONObject("data")
        }
    }

    private fun diag(event: String) {
        Log.i(TAG, event)
        runCatching { com.gamempires.messenger.calls.CallDiagnostics.record(this, "PUSH $event") }
    }

    private companion object {
        const val TAG = "Push"
        const val ONE_TO_ONE = 1
    }
}
