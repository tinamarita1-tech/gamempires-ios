package com.gamempires.messenger.data.nextcloud

import com.gamempires.messenger.data.nextcloud.dto.NextcloudParticipantDto
import com.gamempires.messenger.model.PresenceStatus

internal fun participantUserIds(currentUserId: String, participants: List<NextcloudParticipantDto>): List<String> =
    (listOf(currentUserId) + participants.filter { it.actorType == "users" }
        .map { it.actorId }).filter { it.isNotBlank() }.distinct()

internal fun resolvePresence(accountStatus: String?, roomStatus: String?, lastPing: Long, nowSeconds: Long): PresenceStatus =
    parsePresence(accountStatus) ?: parsePresence(roomStatus) ?: if (
        lastPing > 0 && nowSeconds - lastPing in 0..120
    ) PresenceStatus.ONLINE else PresenceStatus.OFFLINE

internal fun parsePresence(status: String?): PresenceStatus? = when (status?.lowercase()) {
    "online" -> PresenceStatus.ONLINE
    "away" -> PresenceStatus.AWAY
    "dnd", "busy" -> PresenceStatus.BUSY
    "offline", "invisible" -> PresenceStatus.OFFLINE
    else -> null
}
