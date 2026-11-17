package com.gamempires.messenger.push

import android.content.Context
import android.util.Log
import com.gamempires.messenger.data.nextcloud.NextcloudTalkRepository
import com.gamempires.messenger.data.nextcloud.auth.NextcloudAuthStorage
import com.gamempires.messenger.data.nextcloud.auth.NextcloudCredentials
import com.gamempires.messenger.data.nextcloud.interceptor.NextcloudAuthInterceptor
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * One mobile session per account. A new phone login "claims" the session at the push gateway; the
 * gateway then sends the previous phone a sign-out push. Every phone also checks on start/resume, so a
 * phone that was off or offline signs out the next time it is opened.
 */
@Singleton
class SessionGuard @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val auth: NextcloudAuthStorage,
    private val registrar: PushRegistrar,
    private val repository: NextcloudTalkRepository,
    private val notifier: PushNotifier,
    baseClient: OkHttpClient
) {
    private val mutex = Mutex()
    private val prefs = context.getSharedPreferences("gamempires_session", Context.MODE_PRIVATE)
    private val _signedOutElsewhere = MutableStateFlow(false)
    /** True after this phone was signed out because the account was opened on another phone. */
    val signedOutElsewhere: StateFlow<Boolean> = _signedOutElsewhere.asStateFlow()

    private val client: OkHttpClient = baseClient.newBuilder().apply {
        interceptors().removeAll { it is NextcloudAuthInterceptor }
    }.build()

    private val installId: String
        get() = prefs.getString(KEY_INSTALL, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_INSTALL, it).apply()
        }

    fun clearSignedOutNotice() { _signedOutElsewhere.value = false }

    /** After a successful login on this phone: make it the account's only mobile session. */
    suspend fun claim() {
        val creds = auth.credentials.value ?: return
        _signedOutElsewhere.value = false
        runCatching { post(creds, "session", withToken = true) }
            .onFailure { Log.w(TAG, "Session claim failed: ${it.message}") }
    }

    /** On app start/resume and on a sign-out push: sign this phone out if another phone took over. */
    suspend fun check() = mutex.withLock {
        val creds = auth.credentials.value ?: return@withLock
        val result = runCatching { post(creds, "session-check", withToken = false) }.getOrNull() ?: return@withLock
        when {
            // First check after updating the app: no phone has claimed this account yet, so this one does.
            !result.optBoolean("claimed") -> runCatching { post(creds, "session", withToken = true) }
            !result.optBoolean("active") -> signOutHere()
        }
    }

    private suspend fun signOutHere() {
        Log.i(TAG, "Account opened on another phone: signing out here")
        registrar.unregister()
        repository.logout() // also deletes this phone's app password on the server
        _signedOutElsewhere.value = true
        notifier.showMessage(
            nid = SIGNED_OUT_NID,
            title = "Signed out",
            text = "Your account was signed in on another phone, so this phone was signed out.",
            roomToken = null
        )
    }

    private suspend fun post(creds: NextcloudCredentials, path: String, withToken: Boolean): JSONObject =
        withContext(Dispatchers.IO) {
            val form = FormBody.Builder().add("installId", installId)
            if (withToken) firebaseToken()?.let { form.add("pushToken", it) }
            val request = Request.Builder()
                .url("${PushRegistrar.gatewayUrl(creds.serverUrl)}/$path")
                .post(form.build())
                .header("Authorization", Credentials.basic(creds.loginName, creds.appPassword))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw java.io.IOException("Session $path HTTP ${response.code}")
                JSONObject(response.body.string())
            }
        }

    private suspend fun firebaseToken(): String? = withTimeoutOrNull(10_000) {
        suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                cont.resume(if (task.isSuccessful) task.result else null)
            }
        }
    }

    companion object {
        private const val TAG = "Session"
        private const val KEY_INSTALL = "install_id"
        private const val SIGNED_OUT_NID = 990_001
    }
}
