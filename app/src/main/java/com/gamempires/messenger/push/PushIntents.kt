package com.gamempires.messenger.push

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What a tapped notification asked the app to do. MainActivity reads it from the launch intent. */
sealed interface PushAction {
    data class OpenChat(val token: String) : PushAction
    /** Full-screen alert or notification tap: show the in-app incoming-call screen. */
    data class ShowIncomingCall(val token: String, val name: String) : PushAction
    /** "Answer" button: connect straight away. */
    data class AnswerCall(val token: String, val name: String) : PushAction
}

object PushIntents {
    const val EXTRA_ACTION = "com.gamempires.messenger.push.ACTION"
    const val EXTRA_TOKEN = "com.gamempires.messenger.push.TOKEN"
    const val EXTRA_NAME = "com.gamempires.messenger.push.NAME"
    const val ACTION_OPEN_CHAT = "open_chat"
    const val ACTION_SHOW_CALL = "show_call"
    const val ACTION_ANSWER_CALL = "answer_call"

    /** Set by MainActivity (onStart/onStop). Used to avoid double ringing while the app is open. */
    @Volatile var appInForeground: Boolean = false

    private val pending = MutableStateFlow<PushAction?>(null)
    val action: StateFlow<PushAction?> = pending.asStateFlow()

    fun handle(intent: Intent?) {
        if (intent == null) return
        val token = intent.getStringExtra(EXTRA_TOKEN)?.takeIf { it.isNotBlank() } ?: return
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty()
        pending.value = when (intent.getStringExtra(EXTRA_ACTION)) {
            ACTION_OPEN_CHAT -> PushAction.OpenChat(token)
            ACTION_SHOW_CALL -> PushAction.ShowIncomingCall(token, name)
            ACTION_ANSWER_CALL -> PushAction.AnswerCall(token, name)
            else -> return
        }
        // Consume once: a recreated activity must not repeat the action.
        intent.removeExtra(EXTRA_ACTION)
    }

    fun consume(action: PushAction) {
        if (pending.value == action) pending.value = null
    }

    fun isCallLaunch(intent: Intent?): Boolean = intent?.getStringExtra(EXTRA_ACTION) in setOf(ACTION_SHOW_CALL, ACTION_ANSWER_CALL)
}
