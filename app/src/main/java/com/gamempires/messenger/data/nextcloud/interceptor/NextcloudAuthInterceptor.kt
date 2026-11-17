package com.gamempires.messenger.data.nextcloud.interceptor

import android.util.Base64
import com.gamempires.messenger.data.nextcloud.auth.NextcloudAuthStorage
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NextcloudAuthInterceptor @Inject constructor(
    private val authStorage: NextcloudAuthStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requestUrl = original.url
        val creds = authStorage.credentials.value

        val savedServerUrl = creds?.serverUrl?.ifBlank { null }
            ?: NextcloudAuthStorage.DEFAULT_SERVER_URL
        val savedHost = savedServerUrl.toHttpUrlOrNull()?.host ?: "talk.gamempires.com"

        val requestBuilder = original.newBuilder()

        // SECURITY: Add Authorization and OCS headers ONLY when host equals the saved server host and scheme is https
        val isTargetServer = requestUrl.isHttps && requestUrl.host.equals(savedHost, ignoreCase = true)

        if (isTargetServer) {
            requestBuilder
                .header("OCS-APIRequest", "true")
                .header("Accept", "application/json")

            if (creds != null && creds.loginName.isNotBlank() && creds.appPassword.isNotBlank()) {
                val authString = "${creds.loginName}:${creds.appPassword}"
                val basicAuth = "Basic " + Base64.encodeToString(
                    authString.toByteArray(Charsets.UTF_8),
                    Base64.NO_WRAP
                )
                requestBuilder.header("Authorization", basicAuth)
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}

