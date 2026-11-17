package com.gamempires.messenger.data

import com.gamempires.messenger.model.Attachment
import com.gamempires.messenger.model.Chat
import com.gamempires.messenger.model.Message
import com.gamempires.messenger.model.PresenceStatus
import com.gamempires.messenger.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface ChatRepository {
    val currentUser: StateFlow<User>
    val users: StateFlow<List<User>>
    val chats: StateFlow<List<Chat>>
    val filteredChats: StateFlow<List<Chat>>
    val searchQuery: StateFlow<String>
    val userPresences: StateFlow<Map<String, PresenceStatus>>
    val typingUsers: StateFlow<Map<String, Set<String>>>
    val isLoggedIn: StateFlow<Boolean>
    val isOffline: StateFlow<Boolean>
    val isSessionRevoked: StateFlow<Boolean>

    fun getMessages(chatId: String): Flow<List<Message>>
    fun getChatById(chatId: String): Flow<Chat?>
    fun getUserById(userId: String): User?
    fun getChatHistoryError(chatId: String): Flow<String?>
    fun retryLoadHistory(chatId: String)

    fun setSearchQuery(query: String)

    suspend fun refreshRooms()
    suspend fun searchUsers(query: String): List<User>
    suspend fun createOneToOneChat(userId: String): Chat?
    suspend fun logout()

    suspend fun sendMessage(
        chatId: String,
        content: String,
        replyToMessageId: String? = null,
        attachments: List<Attachment> = emptyList()
    ): Message

    suspend fun attachFile(
        chatId: String,
        attachment: Attachment
    ): Message

    suspend fun editMessage(chatId: String, messageId: String, newContent: String)
    suspend fun deleteMessage(chatId: String, messageId: String)
    suspend fun addReaction(chatId: String, messageId: String, reactionEmoji: String)
    suspend fun removeReaction(messageId: String, userId: String)

    suspend fun downloadFile(
        attachment: Attachment,
        targetFile: File,
        onProgress: (Float) -> Unit = {}
    ): Result<File>

    suspend fun syncRoomMessages(chatId: String)

    /** Latest real (non-system, not deleted) message of a room, read from the server; null if none or offline. */
    suspend fun latestUserMessage(chatId: String): com.gamempires.messenger.model.Message?

    suspend fun uploadAndShareFile(
        chatId: String,
        fileUri: android.net.Uri,
        fileName: String,
        mimeType: String,
        fileSize: Long,
        context: android.content.Context,
        onProgress: (Float) -> Unit = {}
    ): Result<Message>

    suspend fun markChatAsRead(chatId: String)
    suspend fun togglePinChat(chatId: String)
    suspend fun toggleMuteChat(chatId: String)

    suspend fun updateCurrentUserPresence(status: PresenceStatus, customStatus: String? = null)
    suspend fun setTypingStatus(chatId: String, isTyping: Boolean)

    suspend fun forwardMessage(targetChatId: String, message: Message): Result<Message>
    suspend fun refreshUserPresence(userId: String, roomToken: String? = null): PresenceStatus
}
