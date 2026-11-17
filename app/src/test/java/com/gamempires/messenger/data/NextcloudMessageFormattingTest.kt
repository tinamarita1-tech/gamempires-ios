package com.gamempires.messenger.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NextcloudMessageFormattingTest {

    private fun formatMessageContent(rawMessage: String, messageParameters: Any?): String {
        if (rawMessage.isBlank() || messageParameters == null) return rawMessage

        val paramsMap = messageParameters as? Map<*, *> ?: return rawMessage
        if (paramsMap.isEmpty()) return rawMessage

        var formatted = rawMessage
        for ((key, value) in paramsMap) {
            if (key !is String) continue
            val replacementName = when (value) {
                is Map<*, *> -> value["name"]?.toString()
                is String -> value
                else -> null
            }
            if (!replacementName.isNullOrEmpty()) {
                formatted = formatted.replace("{$key}", replacementName)
            }
        }
        return formatted
    }

    @Test
    fun testUserPlaceholderReplacement() {
        val raw = "An administrator added {user}"
        val params = mapOf(
            "user" to mapOf("type" to "user", "id" to "sagher", "name" to "Sagher HR")
        )
        val result = formatMessageContent(raw, params)
        assertEquals("An administrator added Sagher HR", result)
    }

    @Test
    fun testActorAndFilePlaceholderReplacement() {
        val raw = "{actor} uploaded {file} to the project"
        val params = mapOf(
            "actor" to mapOf("type" to "user", "id" to "admin", "name" to "Administrator"),
            "file" to mapOf("type" to "file", "id" to "123", "name" to "QuarterlyReport.pdf")
        )
        val result = formatMessageContent(raw, params)
        assertEquals("Administrator uploaded QuarterlyReport.pdf to the project", result)
    }

    @Test
    fun testUnmodifiedWhenNoParameters() {
        val raw = "Welcome to GamEmpires Chat!"
        val result = formatMessageContent(raw, emptyList<Any>())
        assertEquals("Welcome to GamEmpires Chat!", result)
    }
}
