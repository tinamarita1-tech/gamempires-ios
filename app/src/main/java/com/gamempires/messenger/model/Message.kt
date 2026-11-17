package com.gamempires.messenger.model

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.READ,
    val attachments: List<Attachment> = emptyList(),
    val replyToMessageId: String? = null,
    val reactions: Map<String, String> = emptyMap(), // map of userId -> emoji string
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val isSystemMessage: Boolean = false,
    val senderName: String? = null
)
