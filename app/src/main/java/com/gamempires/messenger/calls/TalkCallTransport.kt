package com.gamempires.messenger.calls

import com.gamempires.messenger.data.nextcloud.auth.NextcloudCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Call-scoped cookies keep a Talk session alive without affecting chat HTTP requests. */
internal class TalkCallTransport(baseClient: OkHttpClient, val credentials: NextcloudCredentials) {
    val base = credentials.serverUrl.trimEnd('/') + "/"
    private val cookies = mutableListOf<Cookie>()
    val client = baseClient.newBuilder().apply {
        // Each call retains its own credentials for teardown, even if the user signs out.
        interceptors().removeAll { it is com.gamempires.messenger.data.nextcloud.interceptor.NextcloudAuthInterceptor }
    }.cookieJar(object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, values: List<Cookie>) = synchronized(cookies) {
            values.forEach { cookie ->
                cookies.removeAll { it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path }
                cookies.add(cookie)
            }
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> = synchronized(cookies) {
            cookies.filter { it.expiresAt > System.currentTimeMillis() && it.matches(url) }
        }
    }).build()

    suspend fun request(method: String, path: String, fields: Map<String, String> = emptyMap()): Any = withContext(Dispatchers.IO) {
        val builder = (base + path).toHttpUrl().newBuilder()
        if (method == "GET" || method == "DELETE") fields.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        val body = if (method == "POST" || method == "PUT") FormBody.Builder().apply {
            fields.forEach { (key, value) -> add(key, value) }
        }.build() else null
        val request = Request.Builder().url(builder.build()).method(method, body)
            .header("Authorization", Credentials.basic(credentials.loginName, credentials.appPassword))
            .header("OCS-APIRequest", "true").header("Accept", "application/json").build()
        client.newCall(request).awaitResponse().use { response ->
            if (!response.isSuccessful) throw IOException(when(response.code) {
                401 -> "Your session expired. Please sign in again."
                403 -> "The server does not allow this call."
                409 -> "This account already has another call session. End it first."
                else -> "Call server returned HTTP ${response.code}"
            })
            val root = JSONObject(response.body.string()).optJSONObject("ocs") ?: throw IOException("Invalid call server response")
            val status = root.optJSONObject("meta")?.optInt("statuscode", 200) ?: 200
            if (status != 100 && status != 200) throw IOException("Call request rejected ($status)")
            root.opt("data") ?: JSONObject()
        }
    }

    private suspend fun Call.awaitResponse(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { if (continuation.isActive) continuation.resumeWithException(e) }
            override fun onResponse(call: Call, response: Response) {
                if (continuation.isActive) continuation.resume(response, onCancellation = { _, value, _ -> value.close() }) else response.close()
            }
        })
    }
}

/**
 * Decline an incoming one-to-one call so the caller stops ringing too.
 * Talk has no "reject" endpoint, so this joins the conversation briefly, ends the call for
 * everyone (the same request the app uses to hang up a one-to-one call) and leaves again.
 */
internal suspend fun declineDirectCall(api: TalkCallTransport, token: String) {
    try {
        api.request("POST", "ocs/v2.php/apps/spreed/api/v4/room/$token/participants/active", mapOf("force" to "false"))
        api.request("DELETE", "ocs/v2.php/apps/spreed/api/v4/call/$token", leaveAudioCallFields(direct = true))
    } finally {
        runCatching { api.request("DELETE", "ocs/v2.php/apps/spreed/api/v4/room/$token/participants/active") }
    }
}
