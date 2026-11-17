package com.gamempires.messenger.model

enum class ChatType {
    DIRECT,
    GROUP
}

data class Chat(
    val id: String,
    val type: ChatType,
    val title: String,
    val avatarUrl: String? = null,
    val participantIds: List<String>,
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
