package com.gamempires.messenger.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamempires.messenger.data.nextcloud.NextcloudTalkRepository
import com.gamempires.messenger.data.nextcloud.api.NextcloudTalkApi
import com.gamempires.messenger.data.nextcloud.auth.NextcloudAuthStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Initializing : LoginUiState
    data object SigningIn : LoginUiState
    data class AwaitingBrowserAuth(val loginUrl: String) : LoginUiState
    data object Success : LoginUiState
    data class Error(val message: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val api: NextcloudTalkApi,
    private val authStorage: NextcloudAuthStorage,
    private val talkRepository: NextcloudTalkRepository,
    private val pushRegistrar: com.gamempires.messenger.push.PushRegistrar,
    private val sessionGuard: com.gamempires.messenger.push.SessionGuard,
    baseClient: okhttp3.OkHttpClient
) : ViewModel() {

    // Plain client (no stored-login interceptor) for exchanging the typed password once.
    private val loginClient: okhttp3.OkHttpClient = baseClient.newBuilder().apply {
        interceptors().removeAll { it is com.gamempires.messenger.data.nextcloud.interceptor.NextcloudAuthInterceptor }
    }.build()

    val isLoggedIn: StateFlow<Boolean> = authStorage.isLoggedIn

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        viewModelScope.launch {
            talkRepository.isSessionRevoked.collect { revoked ->
                if (revoked) {
                    _uiState.value = LoginUiState.Error("Your login session was revoked on the server. Please log in again.")
                }
            }
        }
        viewModelScope.launch {
            sessionGuard.signedOutElsewhere.collect { elsewhere ->
                if (elsewhere) {
                    _uiState.value = LoginUiState.Error(
                        "You were signed out because your account was signed in on another phone. " +
                            "Only one phone can use your account at a time."
                    )
                }
            }
        }
    }

    fun startLoginFlow(serverUrl: String = NextcloudAuthStorage.DEFAULT_SERVER_URL) {
        pollJob?.cancel()
        sessionGuard.clearSignedOutNotice()
        _uiState.value = LoginUiState.Initializing

        viewModelScope.launch {
            try {
                val initResponse = api.initLoginV2()
                val loginUrl = initResponse.login
                val pollUrl = initResponse.poll.endpoint
                val token = initResponse.poll.token

                _uiState.value = LoginUiState.AwaitingBrowserAuth(loginUrl)

                // Start polling Nextcloud Login Flow v2
                pollJob = launch {
                    while (isActive) {
                        delay(2000)
                        try {
                            val pollResp = api.pollLoginV2(pollUrl, token)
                            if (pollResp.isSuccessful && pollResp.body() != null) {
                                val successData = pollResp.body()!!
                                authStorage.saveCredentials(
                                    serverUrl = successData.server.ifBlank { serverUrl },
                                    loginName = successData.loginName,
                                    appPassword = successData.appPassword
                                )
                                // This phone becomes the account's only mobile session; the previous phone is signed out.
                                sessionGuard.claim()
                                talkRepository.refreshRooms()
                                _uiState.value = LoginUiState.Success
                                break
                            }
                        } catch (_: Exception) {
                            // 404 or connection pending, continue polling
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "Failed to connect to Nextcloud")
            }
        }
    }

    /**
     * In-app login: trades the username + password for a Nextcloud app password once
     * (GET /ocs/v2.php/core/getapppassword). Only the app password is stored; the typed password
     * is never saved. Accounts with two-step login must use the browser login instead.
     */
    fun loginWithPassword(username: String, password: String) {
        val user = username.trim()
        if (user.isEmpty() || password.isEmpty()) return
        pollJob?.cancel()
        sessionGuard.clearSignedOutNotice()
        _uiState.value = LoginUiState.SigningIn
        viewModelScope.launch {
            val server = NextcloudAuthStorage.DEFAULT_SERVER_URL
            val result = runCatching {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val request = okhttp3.Request.Builder()
                        .url("$server/ocs/v2.php/core/getapppassword?format=json")
                        .header("Authorization", okhttp3.Credentials.basic(user, password, Charsets.UTF_8))
                        .header("OCS-APIRequest", "true")
                        .header("Accept", "application/json")
                        // Names the session in Nextcloud's "Devices & sessions" list.
                        .header("User-Agent", "GamEmpires Messenger (Android ${android.os.Build.MODEL})")
                        .build()
                    loginClient.newCall(request).execute().use { response ->
                        val body = response.body.string()
                        val appPassword = runCatching {
                            org.json.JSONObject(body).getJSONObject("ocs").getJSONObject("data").getString("apppassword")
                        }.getOrNull()
                        response.code to appPassword
                    }
                }
            }
            val (code, appPassword) = result.getOrElse {
                _uiState.value = LoginUiState.Error("Can't reach the chat server. Check your internet connection and try again.")
                return@launch
            }
            if (code == 200 && !appPassword.isNullOrBlank()) {
                authStorage.saveCredentials(serverUrl = server, loginName = user, appPassword = appPassword)
                sessionGuard.claim()
                talkRepository.refreshRooms()
                _uiState.value = LoginUiState.Success
            } else {
                _uiState.value = LoginUiState.Error(
                    when (code) {
                        401, 403 -> "Wrong username or password. If two-step login is turned on for your account, use \"Log in with browser\" below."
                        429 -> "Too many attempts. Please wait a minute and try again."
                        else -> "Login failed (server answered $code). Please try again."
                    }
                )
            }
        }
    }

    fun logout() {
        pollJob?.cancel()
        viewModelScope.launch {
            // Stop notifications for this phone before the app password is deleted.
            pushRegistrar.unregister()
            talkRepository.logout()
            _uiState.value = LoginUiState.Idle
        }
    }

    fun dismiss() {
        pollJob?.cancel()
        _uiState.value = LoginUiState.Idle
    }
}
