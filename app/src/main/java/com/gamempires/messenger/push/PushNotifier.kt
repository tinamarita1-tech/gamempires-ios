package com.gamempires.messenger.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.app.KeyguardManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gamempires.messenger.MainActivity
import com.gamempires.messenger.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val manager = NotificationManagerCompat.from(context)

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val system = context.getSystemService(NotificationManager::class.java) ?: return
        system.createNotificationChannel(NotificationChannel(CHANNEL_MESSAGES, "Messages", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "New chat messages and mentions"
            enableVibration(true)
        })
        system.createNotificationChannel(NotificationChannel(CHANNEL_CALLS, "Incoming calls", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Rings when someone calls you"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 800, 600, 800, 600)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        })
    }

    private fun canPost(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @android.annotation.SuppressLint("MissingPermission") // checked in canPost()
    fun showMessage(nid: Int, title: String, text: String, roomToken: String?) {
        if (!canPost()) return
        ensureChannels()
        val open = activityIntent(PushIntents.ACTION_OPEN_CHAT, roomToken.orEmpty(), "", requestCode = nid)
        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .apply { if (roomToken != null) setGroup("room-$roomToken") }
            .build()
        runCatching { manager.notify(nid, notification) }
    }

    @android.annotation.SuppressLint("MissingPermission") // checked in canPost()
    fun showIncomingCall(nid: Int, roomToken: String, callerText: String): String {
        if (!canPost()) return "notification permission missing"
        ensureChannels()
        val show = activityIntent(PushIntents.ACTION_SHOW_CALL, roomToken, callerText, requestCode = nid * 10 + 1)
        val answer = activityIntent(PushIntents.ACTION_ANSWER_CALL, roomToken, callerText, requestCode = nid * 10 + 2)
        val decline = PendingIntent.getBroadcast(
            context, nid * 10 + 3,
            Intent(context, CallActionReceiver::class.java).setAction(CallActionReceiver.ACTION_DECLINE)
                .putExtra(CallActionReceiver.EXTRA_NID, nid)
                .putExtra(CallActionReceiver.EXTRA_TOKEN, roomToken),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_CALLS)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle("Incoming audio call")
            .setContentText(callerText)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(true)
            .setTimeoutAfter(CALL_RING_MS)
            .setContentIntent(show)
            // Lock screen / screen off: opens the call screen directly (Android may show a heads-up instead
            // when "full-screen notifications" is not allowed for this app).
            .setFullScreenIntent(show, true)
            .addAction(0, "Decline", decline)
            .addAction(0, "Answer", answer)
            .build()
        notification.flags = notification.flags or android.app.Notification.FLAG_INSISTENT
        val posted = runCatching { manager.notify(roomToken, nid, notification) }.isSuccess
        val woke = wakeScreen()
        return callAlertState(posted, woke)
    }

    /**
     * Light the screen for an incoming call. Xiaomi/MIUI and Android 14+ often block the full-screen
     * intent for sideloaded apps, so the notification alone may arrive with the screen still dark.
     */
    @Suppress("DEPRECATION")
    private fun wakeScreen(): String {
        val pm = context.getSystemService(PowerManager::class.java) ?: return "no power service"
        if (pm.isInteractive) return "screen already on"
        return runCatching {
            pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "GamEmpires:incomingCall"
            ).acquire(15_000L)
            "screen woken"
        }.getOrElse { "wake failed: ${it.javaClass.simpleName}" }
    }

    private fun callAlertState(posted: Boolean, woke: String): String {
        val system = context.getSystemService(NotificationManager::class.java)
        val fullScreen = if (Build.VERSION.SDK_INT >= 34) runCatching { system?.canUseFullScreenIntent() }.getOrNull()?.toString() ?: "?" else "n/a"
        val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) system?.getNotificationChannel(CHANNEL_CALLS)?.importance?.toString() ?: "?" else "n/a"
        val locked = context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked
        return "posted=$posted enabled=${manager.areNotificationsEnabled()} channelImportance=$channel fullScreenAllowed=$fullScreen locked=$locked $woke"
    }

    /** Room token of the call that is ringing under this notification id, or null if it isn't a ringing call. */
    fun ringingCallToken(nid: Int): String? {
        val system = context.getSystemService(NotificationManager::class.java) ?: return null
        return runCatching {
            system.activeNotifications.firstOrNull { it.id == nid && it.notification.channelId == CHANNEL_CALLS }?.tag
        }.getOrNull()
    }

    /** All currently ringing calls: notification id to room token. */
    fun ringingCalls(): Map<Int, String> {
        val system = context.getSystemService(NotificationManager::class.java) ?: return emptyMap()
        return runCatching {
            system.activeNotifications.filter { it.notification.channelId == CHANNEL_CALLS && it.tag != null }
                .associate { it.id to it.tag }
        }.getOrDefault(emptyMap())
    }

    /** Cancel a notification by id, whether or not it has a tag (call notifications are tagged). */
    fun cancel(nid: Int) {
        manager.cancel(nid)
        val system = context.getSystemService(NotificationManager::class.java) ?: return
        runCatching { system.activeNotifications.filter { it.id == nid }.forEach { system.cancel(it.tag, it.id) } }
    }

    /** Stop any ringing call notification (the in-app call screen takes over). */
    fun cancelCallAlerts() {
        val system = context.getSystemService(NotificationManager::class.java) ?: return
        runCatching {
            system.activeNotifications.filter { it.notification.channelId == CHANNEL_CALLS }.forEach { system.cancel(it.tag, it.id) }
        }
    }
    fun cancelAll() = manager.cancelAll()

    private fun activityIntent(action: String, token: String, name: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(PushIntents.EXTRA_ACTION, action)
            .putExtra(PushIntents.EXTRA_TOKEN, token)
            .putExtra(PushIntents.EXTRA_NAME, name)
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val CHANNEL_MESSAGES = "messages"
        const val CHANNEL_CALLS = "incoming_calls"
        const val CALL_RING_MS = 45_000L
    }
}
