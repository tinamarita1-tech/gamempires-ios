package com.gamempires.messenger.calls

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.gamempires.messenger.MainActivity
import com.gamempires.messenger.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** "Hang up" pressed on the ongoing-call notification; the call screen's ViewModel listens for it. */
object CallControl {
    private val _hangups = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val hangups: SharedFlow<Unit> = _hangups.asSharedFlow()
    fun requestHangup() { _hangups.tryEmit(Unit) }
}

/**
 * Keeps an audio call running while the app is in the background or the phone is locked:
 * a foreground service (type microphone) with an "Ongoing call" notification and a Hang up button.
 * Started when a call starts connecting (always from the open app), stopped when the call ends.
 */
class OngoingCallService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HANGUP -> {
                CallControl.requestHangup()
                finish()
                return START_NOT_STICKY
            }
            ACTION_STOP -> {
                // Android requires startForeground() after startForegroundService(), even when the call
                // already ended; show the notification for an instant, then remove it.
                goForeground(lastName)
                finish()
                return START_NOT_STICKY
            }
        }
        lastName = intent?.getStringExtra(EXTRA_NAME).orEmpty().ifBlank { "Audio call" }
        if (!goForeground(lastName)) finish()
        return START_NOT_STICKY
    }

    private var lastName = "Audio call"

    private fun finish() {
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    /** Shows the ongoing-call notification as a foreground service. Returns false if Android refused. */
    private fun goForeground(name: String): Boolean {
        ensureChannel(this)
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val hangup = PendingIntent.getService(
            this, 1, Intent(this, OngoingCallService::class.java).setAction(ACTION_HANGUP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle("Ongoing call")
            .setContentText(name)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)
            .setContentIntent(open)
            .addAction(0, "Hang up", hangup)
            .build()
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }.onFailure {
            CallDiagnostics.record(this, "Ongoing-call service could not start: ${it.message}")
        }.isSuccess
    }

    companion object {
        private const val CHANNEL = "ongoing_call"
        private const val NOTIFICATION_ID = 4242
        private const val EXTRA_NAME = "name"
        private const val ACTION_HANGUP = "com.gamempires.messenger.HANG_UP"
        private const val ACTION_STOP = "com.gamempires.messenger.STOP_CALL_SERVICE"
        @Volatile private var running = false

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(CHANNEL, "Ongoing call", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Shown while you are in a call"
                    setShowBadge(false)
                }
            )
        }

        fun start(context: Context, name: String) {
            running = true
            runCatching {
                ContextCompat.startForegroundService(
                    context, Intent(context, OngoingCallService::class.java).putExtra(EXTRA_NAME, name)
                )
            }.onFailure { CallDiagnostics.record(context, "Ongoing-call service start refused: ${it.message}") }
        }

        fun stop(context: Context) {
            if (!running) return
            running = false
            // Never stopService() here: if the service has not reached startForeground() yet, Android
            // kills the app (ForegroundServiceDidNotStartInTimeException). The service stops itself.
            runCatching {
                context.startService(Intent(context, OngoingCallService::class.java).setAction(ACTION_STOP))
            }.onFailure { CallDiagnostics.record(context, "Ongoing-call service stop failed: ${it.message}") }
        }
    }
}
