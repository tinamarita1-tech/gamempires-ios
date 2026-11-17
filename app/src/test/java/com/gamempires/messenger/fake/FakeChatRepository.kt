package com.gamempires.messenger.fake

import com.gamempires.messenger.data.ChatRepository
import com.gamempires.messenger.model.Attachment
import com.gamempires.messenger.model.Chat
import com.gamempires.messenger.model.ChatType
import com.gamempires.messenger.model.Message
import com.gamempires.messenger.model.MessageStatus
import com.gamempires.messenger.model.PresenceStatus
import com.gamempires.messenger.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import java.io.File

class FakeChatRepository : ChatRepository {

    private val _currentUser = MutableStateFlow(
        User(
            id = "user_me",
            name = "Test User",
            avatarUrl = "",
            status = PresenceStatus.ONLINE,
            customStatus = "Testing"
        )
    )
    override val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    private val _users = MutableStateFlow(
        listOf(
            _currentUser.value,
            User(id = "user_bob", name = "Bob", avatarUrl = "", status = PresenceStatus.ONLINE)
        )
    )
    override val users: StateFlow<List<User>> = _users.asStateFlow()

    private val initialChats = listOf(
        Chat(
            id = "chat_1",
            type = ChatType.DIRECT,
            title = "Bob",
            avatarUrl = null,
            participantIds = listOf("user_me", "user_bob"),
            unreadCount = 1,
            isPinned = false,
            isMuted = false,
            updatedAt = System.currentTimeMillis()
        ),
        Chat(
            id = "chat_group_1",
            type = ChatType.GROUP,
            title = "Team",
            avatarUrl = null,
            participantIds = listOf("user_me", "user_bob"),
            unreadCount = 0,
            isPinned = true,
            isMuted = false,
            updatedAt = System.currentTimeMillis()
        )
    )

    private val _chats = MutableStateFlow(initialChats)
    override val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    override val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredChats = MutableStateFlow(initialChats)
    override val filteredChats: StateFlow<List<Chat>> = _filteredChats.asStateFlow()

    private val _userPresences = MutableStateFlow<Map<String, PresenceStatus>>(emptyMap())
    override val userPresences: StateFlow<Map<String, PresenceStatus>> = _userPresences.asStateFlow()

    private val _typingUsers = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    override val typingUsers: StateFlow<Map<String, Set<String>>> = _typingUsers.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(true)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    override val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _isSessionRevoked = MutableStateFlow(false)
    override val isSessionRevoked: StateFlow<Boolean> = _isSessionRevoked.asStateFlow()

    private val _historyError = MutableStateFlow<String?>(null)

    override fun getMessages(chatId: String): Flow<List<Message>> = flowOf(emptyList())

    override fun getChatById(chatId: String): Flow<Chat?> =
        flowOf(_chats.value.find { it.id == chatId })

    override fun getUserById(userId: String): User? =
        _users.value.find { it.id == userId }

    override fun getChatHistoryError(chatId: String): Flow<String?> = _historyError.asStateFlow()

    override fun retryLoadHistory(chatId: String) {
        _historyError.value = null
    }

    override fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _filteredChats.value = _chats.value
        } else {
            _filteredChats.value = _chats.value.filter { it.title.contains(query, ignoreCase = true) }
        }
    }

    override suspend fun refreshRooms() {}

    override suspend fun searchUsers(query: String): List<User> {
        return _users.value.filter { it.name.contains(query, ignoreCase = true) }
    }

    override suspend fun createOneToOneChat(userId: String): Chat? {
        val user = getUserById(userId) ?: return null
        val newChat = Chat(
            id = "chat_new_${System.currentTimeMillis()}",
            type = ChatType.DIRECT,
            title = user.name,
            avatarUrl = user.avatarUrl,
            participantIds = listOf(_currentUser.value.id, user.id),
            unreadCount = 0,
            isPinned = false,
            isMuted = false,
            updatedAt = System.currentTimeMillis()
        )
        val list = _chats.value.toMutableList()
        list.add(0, newChat)
        _chats.value = list
        return newChat
    }

    override suspend fun logout() {
        _isLoggedIn.value = false
    }

    override suspend fun sendMessage(
        chatId: String,
        content: String,
        replyToMessageId: String?,
        attachments: List<Attachment>
    ): Message {
        return Message(
            id = "msg_${System.currentTimeMillis()}",
            chatId = chatId,
            senderId = _currentUser.value.id,
            content = content,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENT,
            replyToMessageId = replyToMessageId
        )
    }

    override suspend fun attachFile(chatId: String, attachment: Attachment): Message =
        sendMessage(chatId, "Attachment")

    override suspend fun editMessage(chatId: String, messageId: String, newContent: String) {}
    override suspend fun deleteMessage(chatId: String, messageId: String) {}
    override suspend fun addReaction(chatId: String, messageId: String, reactionEmoji: String) {}
    override suspend fun removeReaction(messageId: String, userId: String) {}

    override suspend fun downloadFile(
        attachment: Attachment,
        targetFile: File,
        onProgress: (Float) -> Unit
    ): Result<File> {
        return Result.success(targetFile)
    }

    override suspend fun syncRoomMessages(chatId: String) {}

    override suspend fun uploadAndShareFile(
        chatId: String,
        fileUri: android.net.Uri,
        fileName: String,
        mimeType: String,
        fileSize: Long,
        context: android.content.Context,
        onProgress: (Float) -> Unit
    ): Result<Message> {
        val att = Attachment(
            id = "att_${System.currentTimeMillis()}",
            type = if (mimeType.startsWith("image/")) com.gamempires.messenger.model.AttachmentType.IMAGE else com.gamempires.messenger.model.AttachmentType.FILE,
            url = fileUri.toString(),
            name = fileName,
            sizeBytes = fileSize,
            mimeType = mimeType,
            thumbnailUrl = fileUri.toString(),
            path = "Talk/$fileName"
        )
        return Result.success(
            Message(
                id = "msg_upload_${System.currentTimeMillis()}",
                chatId = chatId,
                senderId = _currentUser.value.id,
                content = fileName,
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.SENT,
                attachments = listOf(att)
            )
        )
    }

    override suspend fun markChatAsRead(chatId: String) {}
    override suspend fun togglePinChat(chatId: String) {}
    override suspend fun toggleMuteChat(chatId: String) {}

    override suspend fun updateCurrentUserPresence(status: PresenceStatus, customStatus: String?) {
        _currentUser.value = _currentUser.value.copy(
            status = status,
            customStatus = customStatus ?: _currentUser.value.customStatus
        )
    }

    override suspend fun setTypingStatus(chatId: String, isTyping: Boolean) {}

    override suspend fun forwardMessage(targetChatId: String, message: Message): Result<Message> {
        val forwarded = message.copy(
            id = "fwd_${System.currentTimeMillis()}",
            chatId = targetChatId,
            senderId = _currentUser.value.id,
            timestamp = System.currentTimeMillis()
        )
        return Result.success(forwarded)
    }

    override suspend fun refreshUserPresence(userId: String, roomToken: String?): PresenceStatus {
        return _userPresences.value[userId] ?: PresenceStatus.OFFLINE
    }
}
