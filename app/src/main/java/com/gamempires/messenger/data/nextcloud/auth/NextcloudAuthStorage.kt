package com.gamempires.messenger.data.nextcloud.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gamempires.messenger.data.nextcloud.crypto.KeyStoreCryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore by preferencesDataStore(name = "gamempires_talk_auth")

data class NextcloudCredentials(
    val serverUrl: String,
    val loginName: String,
    val appPassword: String
)

@Singleton
class NextcloudAuthStorage @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val cryptoManager: KeyStoreCryptoManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _credentials = MutableStateFlow<NextcloudCredentials?>(null)
    val credentials: StateFlow<NextcloudCredentials?> = _credentials.asStateFlow()

    init {
        scope.launch {
            context.authDataStore.data.collect { prefs ->
                val server = prefs[KEY_SERVER_URL] ?: DEFAULT_SERVER_URL
                val encryptedLogin = prefs[KEY_ENCRYPTED_LOGIN]
                val encryptedPass = prefs[KEY_ENCRYPTED_PASSWORD]

                if (!encryptedLogin.isNullOrBlank() && !encryptedPass.isNullOrBlank()) {
                    try {
                        val loginName = cryptoManager.decrypt(encryptedLogin)
                        val appPassword = cryptoManager.decrypt(encryptedPass)
                        if (loginName.isNotBlank() && appPassword.isNotBlank()) {
                            _credentials.value = NextcloudCredentials(server, loginName, appPassword)
                            _isLoggedIn.value = true
                            return@collect
                        }
                    } catch (e: Exception) {
                        clearCredentials()
                    }
                }
                _credentials.value = null
                _isLoggedIn.value = false
            }
        }
    }

    suspend fun saveCredentials(serverUrl: String, loginName: String, appPassword: String) {
        val cleanServer = serverUrl.trim().removeSuffix("/")
        val encryptedLogin = cryptoManager.encrypt(loginName.trim())
        val encryptedPass = cryptoManager.encrypt(appPassword.trim())

        context.authDataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = cleanServer
            prefs[KEY_ENCRYPTED_LOGIN] = encryptedLogin
            prefs[KEY_ENCRYPTED_PASSWORD] = encryptedPass
        }

        val creds = NextcloudCredentials(cleanServer, loginName.trim(), appPassword.trim())
        _credentials.value = creds
        _isLoggedIn.value = true
    }

    suspend fun clearCredentials() {
        context.authDataStore.edit { prefs ->
            prefs.remove(KEY_ENCRYPTED_LOGIN)
            prefs.remove(KEY_ENCRYPTED_PASSWORD)
        }
        _credentials.value = null
        _isLoggedIn.value = false
    }

    companion object {
        const val DEFAULT_SERVER_URL = "https://talk.gamempires.com"
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_ENCRYPTED_LOGIN = stringPreferencesKey("encrypted_login")
        private val KEY_ENCRYPTED_PASSWORD = stringPreferencesKey("encrypted_password")
    }
}
