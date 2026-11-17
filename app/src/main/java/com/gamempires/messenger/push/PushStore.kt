package com.gamempires.messenger.push

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gamempires.messenger.data.nextcloud.crypto.KeyStoreCryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import javax.inject.Inject
import javax.inject.Singleton

private val Context.pushDataStore by preferencesDataStore(name = "gamempires_push")

/** Push registration state. The device private key is stored encrypted with the Android Keystore AES key. */
data class PushRegistration(
    val serverUrl: String,
    val tokenHash: String,
    val deviceIdentifier: String,
    val deviceIdentifierSignature: String,
    val userPublicKey: String,
    val proxyRegistered: Boolean
)

@Singleton
class PushStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val crypto: KeyStoreCryptoManager
) {
    suspend fun deviceKeyPair(): KeyPair {
        val prefs = context.pushDataStore.data.first()
        val storedPrivate = prefs[KEY_PRIVATE]
        val storedPublic = prefs[KEY_PUBLIC]
        if (!storedPrivate.isNullOrBlank() && !storedPublic.isNullOrBlank()) {
            runCatching {
                val privKey = PushCrypto.privateKeyFromBase64(crypto.decrypt(storedPrivate))
                val pubKey = PushCrypto.parsePublicKeyPem(storedPublic)
                return KeyPair(pubKey, privKey)
            }
        }
        val pair = PushCrypto.generateDeviceKeyPair()
        context.pushDataStore.edit {
            it[KEY_PRIVATE] = crypto.encrypt(PushCrypto.privateKeyToBase64(pair.private))
            it[KEY_PUBLIC] = PushCrypto.publicKeyToPem(pair.public)
            // A new key pair invalidates any earlier server registration.
            clearRegistration(it)
        }
        return pair
    }

    suspend fun devicePrivateKey(): PrivateKey? {
        val stored = context.pushDataStore.data.first()[KEY_PRIVATE] ?: return null
        return runCatching { PushCrypto.privateKeyFromBase64(crypto.decrypt(stored)) }.getOrNull()
    }

    suspend fun userPublicKey(): PublicKey? {
        val pem = context.pushDataStore.data.first()[KEY_USER_PUBLIC] ?: return null
        return runCatching { PushCrypto.parsePublicKeyPem(pem) }.getOrNull()
    }

    suspend fun registration(): PushRegistration? {
        val p = context.pushDataStore.data.first()
        return PushRegistration(
            serverUrl = p[KEY_SERVER] ?: return null,
            tokenHash = p[KEY_TOKEN_HASH] ?: return null,
            deviceIdentifier = p[KEY_DEVICE_ID] ?: return null,
            deviceIdentifierSignature = p[KEY_DEVICE_SIG] ?: return null,
            userPublicKey = p[KEY_USER_PUBLIC] ?: return null,
            proxyRegistered = p[KEY_PROXY_HASH] == p[KEY_TOKEN_HASH]
        )
    }

    suspend fun saveServerRegistration(serverUrl: String, tokenHash: String, deviceIdentifier: String, signature: String, userPublicKey: String) {
        context.pushDataStore.edit {
            if (it[KEY_TOKEN_HASH] != tokenHash || it[KEY_DEVICE_ID] != deviceIdentifier) it.remove(KEY_PROXY_HASH)
            it[KEY_SERVER] = serverUrl
            it[KEY_TOKEN_HASH] = tokenHash
            it[KEY_DEVICE_ID] = deviceIdentifier
            it[KEY_DEVICE_SIG] = signature
            it[KEY_USER_PUBLIC] = userPublicKey
        }
    }

    suspend fun markProxyRegistered(tokenHash: String) {
        context.pushDataStore.edit { it[KEY_PROXY_HASH] = tokenHash }
    }

    suspend fun clearRegistration() {
        context.pushDataStore.edit { clearRegistration(it) }
    }

    private fun clearRegistration(prefs: androidx.datastore.preferences.core.MutablePreferences) {
        listOf<Preferences.Key<String>>(KEY_SERVER, KEY_TOKEN_HASH, KEY_DEVICE_ID, KEY_DEVICE_SIG, KEY_USER_PUBLIC, KEY_PROXY_HASH)
            .forEach { prefs.remove(it) }
    }

    private companion object {
        val KEY_PRIVATE = stringPreferencesKey("device_private_key")
        val KEY_PUBLIC = stringPreferencesKey("device_public_pem")
        val KEY_SERVER = stringPreferencesKey("server_url")
        val KEY_TOKEN_HASH = stringPreferencesKey("push_token_hash")
        val KEY_DEVICE_ID = stringPreferencesKey("device_identifier")
        val KEY_DEVICE_SIG = stringPreferencesKey("device_identifier_signature")
        val KEY_USER_PUBLIC = stringPreferencesKey("user_public_key")
        val KEY_PROXY_HASH = stringPreferencesKey("proxy_registered_hash")
    }
}
