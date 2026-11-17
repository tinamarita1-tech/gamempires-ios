package com.gamempires.messenger.calls

internal class IncomingCallTransitions {
    private var previous: Set<String>? = null
    fun update(active: Set<String>): Set<String> {
        val fresh = previous?.let { active - it }.orEmpty()
        previous = active.toSet()
        return fresh
    }
}

internal const val AUDIO_CALL_FLAGS = 3
internal fun leaveAudioCallFields(direct: Boolean): Map<String, String> =
    if (direct) mapOf("all" to "true") else emptyMap()
internal fun shouldRing(roomType: Int, hasCall: Boolean, ownFlags: Int, dismissed: Boolean): Boolean =
    roomType == 1 && hasCall && (ownFlags and 1) == 0 && !dismissed
