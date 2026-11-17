package com.gamempires.messenger.data.nextcloud

import com.gamempires.messenger.data.nextcloud.dto.NextcloudParticipantDto
import com.gamempires.messenger.model.PresenceStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class NextcloudPresenceTest {
    @Test fun `direct chat retains the other user even without recent room activity`() {
        val participants = listOf(
            NextcloudParticipantDto(actorType = "users", actorId = "alice"),
            NextcloudParticipantDto(actorType = "users", actorId = "bob", lastPing = 0)
        )
        assertEquals(listOf("alice", "bob"), participantUserIds("alice", participants))
    }

    @Test fun `guests are not treated as account users`() {
        val participants = listOf(
            NextcloudParticipantDto(actorType = "guests", actorId = "guest-session"),
            NextcloudParticipantDto(actorType = "users", actorId = "bob")
        )
        assertEquals(listOf("alice", "bob"), participantUserIds("alice", participants))
    }

    @Test fun `account online status wins over stale room ping`() {
        assertEquals(PresenceStatus.ONLINE, resolvePresence("online", null, 0, 1000))
    }

    @Test fun `explicit offline and invisible win over recent room activity`() {
        assertEquals(PresenceStatus.OFFLINE, resolvePresence("offline", "online", 999, 1000))
        assertEquals(PresenceStatus.OFFLINE, resolvePresence("invisible", "online", 999, 1000))
    }

    @Test fun `busy and away are preserved`() {
        assertEquals(PresenceStatus.BUSY, resolvePresence("dnd", "online", 999, 1000))
        assertEquals(PresenceStatus.AWAY, resolvePresence("away", null, 0, 1000))
    }

    @Test fun `room ping is fallback only when account status is unavailable`() {
        assertEquals(PresenceStatus.ONLINE, resolvePresence(null, null, 999, 1000))
        assertEquals(PresenceStatus.OFFLINE, resolvePresence(null, null, 500, 1000))
        assertEquals(PresenceStatus.OFFLINE, resolvePresence(null, null, 1100, 1000))
    }
}
