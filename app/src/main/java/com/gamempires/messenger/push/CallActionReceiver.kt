package com.gamempires.messenger.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.gamempires.messenger.calls.TalkCallTransport
import com.gamempires.messenger.calls.declineDirectCall
import com.gamempires.messenger.data.nextcloud.auth.NextcloudAuthStorage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CallActionEntryPoint {
    fun auth(): NextcloudAuthStorage
    fun http(): OkHttpClient
}

/** "Decline" on the incoming-call notification: stop ringing here and end the call for the caller. */
class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DECLINE) return
        val deps = EntryPointAccessors.fromApplication(context.applicationContext, CallActionEntryPoint::class.java)
        val auth = deps.auth()
        val http = deps.http()
        val nid = intent.getIntExtra(EXTRA_NID, -1)
        val token = intent.getStringExtra(EXTRA_TOKEN)?.takeIf { it.isNotBlank() }
        // Call notifications are tagged with the room token.
        if (nid != -1) NotificationManagerCompat.from(context).apply { cancel(token, nid); cancel(nid) }
        if (token == null) return

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val creds = auth.credentials.value
                    ?: withTimeoutOrNull(3000) { auth.credentials.filterNotNull().first() }
                if (creds != null) withTimeout(8000) { declineDirectCall(TalkCallTransport(http, creds), token) }
            } catch (e: Exception) {
                Log.w("Push", "Decline could not end the call: ${e.message}")
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DECLINE = "com.gamempires.messenger.push.DECLINE_CALL"
        const val EXTRA_NID = "nid"
        const val EXTRA_TOKEN = "token"
    }
}
