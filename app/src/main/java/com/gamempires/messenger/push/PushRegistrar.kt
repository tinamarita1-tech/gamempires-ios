package com.gamempires.messenger.push

import android.util.Log
import com.gamempires.messenger.BuildConfig
import com.gamempires.messenger.data.nextcloud.auth.NextcloudAuthStorage
import com.gamempires.messenger.data.nextcloud.auth.NextcloudCredentials
import com.gamempires.messenger.data.nextcloud.interceptor.NextcloudAuthInterceptor
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Registers this phone for Nextcloud push notifications (push-v2) through the GamEmpires push gateway.
 *
 *   Nextcloud ──encrypted push──▶ gateway (https://<server>/push-gateway) ──FCM──▶ this app
 *
 * Runs on every app start and whenever Firebase issues a new token. Nextcloud answers 200 when
 * nothing changed, so repeated calls are cheap.
 */
@Singleton
class PushRegistrar @Inject constructor(
    private val auth: NextcloudAuthStorage,
    private val store: PushStore,
    baseClient: OkHttpClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var started = false

    // Own client without the global auth interceptor: credentials are only sent to the Nextcloud server.
    private val client: OkHttpClient = baseClient.newBuilder().apply {
        interceptors().removeAll { it is NextcloudAuthInterceptor }
    }.build()

    /** Called once from the Application. Registers whenever someone is signed in. */
    fun start() {
        if (started) return
        started = true
        scope.launch {
            // StateFlow only emits when the signed-in account changes.
            auth.credentials.collect { creds ->
                if (creds != null) runCatching { register(creds) }
                    .onFailure { Log.w(TAG, "Push registration failed: ${it.message}") }
            }
        }
    }

    /** Firebase issued a new token (first install, reinstall, data cleared or token rotation). */
    fun onNewToken() {
        scope.launch {
            val creds = auth.credentials.value ?: return@launch
            runCatching { register(creds) }.onFailure { Log.w(TAG, "Push re-registration failed: ${it.message}") }
        }
    }

    suspend fun register(creds: NextcloudCredentials) = mutex.withLock {
        val server = creds.serverUrl.trimEnd('/')
        val gateway = gatewayUrl(server)
        val keys = store.deviceKeyPair()
        val pushToken = firebaseToken()
        val tokenHash = PushCrypto.sha512Hex(pushToken)

        val form = FormBody.Builder()
            .add("pushTokenHash", tokenHash)
            .add("devicePublicKey", PushCrypto.publicKeyToPem(keys.public))
            .add("proxyServer", gateway)
            .build()
        val request = Request.Builder()
            .url("$server/ocs/v2.php/apps/notifications/api/v2/push")
            .post(form)
            .header("Authorization", Credentials.basic(creds.loginName, creds.appPassword))
            .header("OCS-APIRequest", "true")
            .header("Accept", "application/json")
            // Marks this device as a Talk client so Nextcloud routes Talk (chat/call) pushes to it.
            .header("User-Agent", "Mozilla/5.0 (Android) Nextcloud-Talk v${BuildConfig.VERSION_NAME}")
            .build()

        val (code, data) = withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val body = response.body.string()
                val parsed = runCatching { JSONObject(body).optJSONObject("ocs")?.optJSONObject("data") }.getOrNull()
                response.code to parsed
            }
        }
        if (code != 200 && code != 201) {
            throw IOException("Nextcloud push registration HTTP $code ${data?.optString("message").orEmpty()}")
        }
        val deviceIdentifier = data?.optString("deviceIdentifier").orEmpty()
        val signature = data?.optString("signature").orEmpty()
        val userPublicKey = data?.optString("publicKey").orEmpty()
        if (deviceIdentifier.isBlank() || signature.isBlank() || userPublicKey.isBlank()) {
            throw IOException("Nextcloud push registration returned incomplete data")
        }
        store.saveServerRegistration(server, tokenHash, deviceIdentifier, signature, userPublicKey)

        val current = store.registration()
        if (code == 201 || current?.proxyRegistered != true) {
            registerAtGateway(gateway, pushToken, deviceIdentifier, signature, userPublicKey, cloudId = "${creds.loginName}@${server.removePrefix("https://")}")
            store.markProxyRegistered(tokenHash)
        }
        Log.i(TAG, "Push registration complete (server HTTP $code)")
    }

    private suspend fun registerAtGateway(gateway: String, pushToken: String, deviceIdentifier: String,
                                          signature: String, userPublicKey: String, cloudId: String) {
        suspend fun post(withCloudId: Boolean): Int {
            val form = FormBody.Builder()
                .add("pushToken", pushToken)
                .add("deviceIdentifier", deviceIdentifier)
                .add("deviceIdentifierSignature", signature)
                .add("userPublicKey", userPublicKey)
                .apply { if (withCloudId) add("cloudId", cloudId) }
                .build()
            return withContext(Dispatchers.IO) {
                client.newCall(Request.Builder().url("$gateway/devices").post(form).build()).execute().use { it.code }
            }
        }
        var code = post(withCloudId = false)
        if (code == 409) code = post(withCloudId = true)
        if (code != 200) throw IOException("Push gateway registration HTTP $code")
    }

    /** Call before the app password is deleted on logout. Best effort: never blocks logout for long. */
    suspend fun unregister() {
        val creds = auth.credentials.value
        val reg = store.registration()
        runCatching {
            withTimeout(8000) {
                withContext(Dispatchers.IO) {
                    if (creds != null) {
                        val request = Request.Builder()
                            .url("${creds.serverUrl.trimEnd('/')}/ocs/v2.php/apps/notifications/api/v2/push")
                            .delete()
                            .header("Authorization", Credentials.basic(creds.loginName, creds.appPassword))
                            .header("OCS-APIRequest", "true")
                            .header("Accept", "application/json")
                            .build()
                        client.newCall(request).execute().close()
                    }
                    if (reg != null) {
                        val form = FormBody.Builder()
                            .add("deviceIdentifier", reg.deviceIdentifier)
                            .add("deviceIdentifierSignature", reg.deviceIdentifierSignature)
                            .add("userPublicKey", reg.userPublicKey)
                            .build()
                        client.newCall(Request.Builder().url("${gatewayUrl(reg.serverUrl)}/devices").delete(form).build()).execute().close()
                    }
                }
            }
        }.onFailure { Log.w(TAG, "Push unregistration incomplete: ${it.message}") }
        store.clearRegistration()
        runCatching { FirebaseMessaging.getInstance().deleteToken().await() }
    }

    private suspend fun firebaseToken(): String = FirebaseMessaging.getInstance().token.await()

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            val error = task.exception
            if (error != null) cont.resumeWithException(error)
            else if (task.isCanceled) cont.resumeWithException(IOException("Firebase task cancelled"))
            else cont.resume(task.result)
        }
    }

    companion object {
        private const val TAG = "Push"
        fun gatewayUrl(serverUrl: String) = serverUrl.trimEnd('/') + "/push-gateway"
    }
}
