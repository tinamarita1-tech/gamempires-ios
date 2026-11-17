package com.gamempires.messenger.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadedFiles @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val auth: com.gamempires.messenger.data.nextcloud.auth.NextcloudAuthStorage
) {
    private val preferences = context.getSharedPreferences("downloaded_files", Context.MODE_PRIVATE)
    private val records = MutableStateFlow(preferences.all.mapNotNull { (key, value) ->
        (value as? String)?.let { key to it }
    }.toMap())
    fun accountKey(): String = accountPrefix(auth.credentials.value)
    private fun accountPrefix(credentials: com.gamempires.messenger.data.nextcloud.auth.NextcloudCredentials?): String {
        if (credentials == null) return "logged-out:"
        val identity = "${credentials.serverUrl.trimEnd('/')}|${credentials.loginName}"
        return java.security.MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
            .joinToString("") { "%02x".format(it) } + ":"
    }
    val files = combine(records, auth.credentials) { saved, credentials ->
        val prefix = accountPrefix(credentials)
        saved.filterKeys { it.startsWith(prefix) }.mapKeys { it.key.removePrefix(prefix) }
    }.stateIn(CoroutineScope(SupervisorJob() + Dispatchers.IO), SharingStarted.Eagerly, emptyMap())

    fun markSaved(id: String, uri: Uri, account: String = accountKey()) {
        val key = account + id
        preferences.edit().putString(key, uri.toString()).apply()
        records.update { it + (key to uri.toString()) }
    }

    suspend fun existingUri(id: String, account: String = accountKey()): Uri? = withContext(Dispatchers.IO) {
        val key = account + id
        val value = records.value[key] ?: return@withContext null
        val uri = Uri.parse(value)
        val exists = try { context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false }
            catch (_: java.io.FileNotFoundException) { false }
            catch (_: SecurityException) { false }
        if (exists) uri else {
            preferences.edit().remove(key).apply()
            records.update { it - key }
            null
        }
    }
}
