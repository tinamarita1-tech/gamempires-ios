package com.gamempires.messenger.calls

import org.junit.Assert.*
import org.junit.Test

class CallProtocolTest {
    @Test fun `ending a direct audio call ends both sides without ending group calls`() {
        assertEquals(mapOf("all" to "true"), leaveAudioCallFields(direct = true))
        assertTrue(leaveAudioCallFields(direct = false).isEmpty())
    }
    @Test fun `existing call on app startup does not ring as a new incoming call`() {
        val incoming = IncomingCallTransitions()
        assertTrue(incoming.update(setOf("old-call")).isEmpty())
        assertTrue(incoming.update(setOf("old-call")).isEmpty())
        assertTrue(incoming.update(emptySet()).isEmpty())
        assertEquals(setOf("new-call"), incoming.update(setOf("new-call")))
        assertTrue(incoming.update(setOf("new-call")).isEmpty())
    }

    @Test fun `only remote active direct calls should ring`() {
        assertTrue(shouldRing(1, true, 0, false))
        assertFalse(shouldRing(1, false, 0, false))
        assertFalse(shouldRing(1, true, 3, false))
        assertFalse(shouldRing(1, true, 0, true))
        assertFalse(shouldRing(2, true, 0, false))
    }

    @Test fun `audio calls publish audio and never camera flags`() {
        assertEquals(3, AUDIO_CALL_FLAGS)
        assertEquals(0, AUDIO_CALL_FLAGS and 4)
    }
}
