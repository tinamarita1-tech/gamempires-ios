package com.gamempires.messenger.data.nextcloud

import android.util.Log
import com.gamempires.messenger.data.ChatRepository
import com.gamempires.messenger.data.nextcloud.api.NextcloudTalkApi
import com.gamempires.messenger.data.nextcloud.auth.NextcloudAuthStorage
import com.gamempires.messenger.data.nextcloud.dto.NextcloudMessageDto
import com.gamempires.messenger.data.nextcloud.dto.NextcloudRoomDto
import com.gamempires.messenger.model.Attachment
import com.gamempires.messenger.model.AttachmentType
import com.gamempires.messenger.model.Chat
import com.gamempires.messenger.model.ChatType
import com.gamempires.messenger.model.Message
import com.gamempires.messenger.model.MessageStatus
import com.gamempires.messenger.model.PresenceStatus
import com.gamempires.messenger.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okio.BufferedSink
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NextcloudTalkRepository @Inject constructor(
    private val api: NextcloudTalkApi,
    private val authStorage: NextcloudAuthStorage,
    private val okHttpClient: OkHttpClient
) : ChatRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    // Current logged-in user: starts empty when logged out
    private val _currentUser = MutableStateFlow(
        User(
            id = "",
            name = "",
            avatarUrl = "",
            status = PresenceStatus.OFFLINE,
            customStatus = ""
        )
    )
    override val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    override val isLoggedIn: StateFlow<Boolean> = authStorage.isLoggedIn

    private val _isOffline = MutableStateFlow(false)
    override val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _isSessionRevoked = MutableStateFlow(false)
    override val isSessionRevoked: StateFlow<Boolean> = _isSessionRevoked.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    override val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _userPresences = MutableStateFlow<Map<String, PresenceStatus>>(emptyMap())
    override val userPresences: StateFlow<Map<String, PresenceStatus>> = _userPresences.asStateFlow()

    private val _typingUsers = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    override val typingUsers: StateFlow<Map<String, Set<String>>> = _typingUsers.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    override val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Chats map (room token -> Chat)
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    override val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    // Messages cache map (room token -> List<Message>)
    private val _messages = MutableStateFlow<Map<String, List<Message>>>(emptyMap())

    // History error tracking per room
    private val _chatHistoryErrors = MutableStateFlow<Map<String, String>>(emptyMap())

    // Active long-polling jobs per room token
    private val pollingJobs = mutableMapOf<String, Job>()
    private val lastKnownMessageIds = mutableMapOf<String, Long>()

    override val filteredChats: StateFlow<List<Chat>> = combine(_chats, _searchQuery) { chatList, query ->
        if (query.isBlank()) {
            chatList
        } else {
            val q = query.trim().lowercase()
            chatList.filter { it.title.lowercase().contains(q) }
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    init {
        scope.launch {
            authStorage.credentials.collect { creds ->
                if (creds != null) {
                    val avatar = "${creds.serverUrl}/index.php/avatar/${creds.loginName}/128"
                    var realDisplayName = creds.loginName

                    try {
                        val userResp = api.getCurrentUser()
                        if (userResp.isSuccessful) {
                            val userDto = userResp.body()?.ocs?.data
                            val name = userDto?.displayname?.ifBlank { null }
                                ?: userDto?.displayName?.ifBlank { null }
                            if (!name.isNullOrBlank()) {
                                realDisplayName = name
                            }
                            _isOffline.value = false
                        } else if (userResp.code() == 401) {
                            handleSessionRevoked()
                            return@collect
                        }
                    } catch (e: Exception) {
                        handleNetworkError(e)
                    }

                    // Like the Windows app: show an uploaded photo, otherwise draw the initials of the display name.
                    // The server's automatic letter picture can be stale (it showed "S" from the login "sagher").
                    val hasPhoto = try {
                        api.avatarInfo(creds.loginName).headers()["X-NC-IsCustomAvatar"] == "1"
                    } catch (e: Exception) {
                        false
                    }
                    val user = User(
                        id = creds.loginName,
                        name = realDisplayName,
                        avatarUrl = if (hasPhoto) "$avatar?v=" + Integer.toHexString(realDisplayName.hashCode()) else "",
                        status = PresenceStatus.ONLINE,
                        customStatus = ""
                    )
                    _currentUser.value = user
                    _users.value = listOf(user)
                    refreshRooms()
                } else {
                    _currentUser.value = User(
                        id = "",
                        name = "",
                        avatarUrl = "",
                        status = PresenceStatus.OFFLINE,
                        customStatus = ""
                    )
                    _users.value = emptyList()
                    _chats.value = emptyList()
                    _messages.value = emptyMap()
                    _chatHistoryErrors.value = emptyMap()
                    _userPresences.value = emptyMap()
                    cancelAllPolling()
                }
            }
        }
    }

    override suspend fun refreshRooms() {
        if (!authStorage.isLoggedIn.value) return
        try {
            val response = api.getRooms()
            val roomDtos = response.ocs.data
            val mappedChats = roomDtos.map { dto -> mapRoomToChat(dto) }
            _chats.value = mappedChats.sortedWith(
                compareByDescending<Chat> { it.isPinned }.thenByDescending { it.updatedAt }
            )
            _isOffline.value = false
        } catch (e: HttpException) {
            if (e.code() == 401) {
                handleSessionRevoked()
            }
        } catch (e: Exception) {
            handleNetworkError(e)
        }
    }

    override suspend fun searchUsers(query: String): List<User> {
        if (!authStorage.isLoggedIn.value || query.isBlank()) return emptyList()
        return try {
            val resp = api.searchUsers(search = query.trim())
            if (resp.isSuccessful) {
                val data = resp.body()?.ocs?.data ?: emptyList()
                val serverUrl = authStorage.credentials.value?.serverUrl ?: NextcloudAuthStorage.DEFAULT_SERVER_URL
                data.map { dto ->
                    User(
                        id = dto.id,
                        name = dto.label.ifBlank { dto.id },
                        avatarUrl = "$serverUrl/index.php/avatar/${dto.id}/128",
                        status = PresenceStatus.OFFLINE,
                        customStatus = dto.subline ?: ""
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            handleNetworkError(e)
            emptyList()
        }
    }

    override suspend fun createOneToOneChat(userId: String): Chat? {
        if (!authStorage.isLoggedIn.value) return null
        return try {
            val resp = api.createOneToOneRoom(roomType = 1, invite = userId)
            if (resp.isSuccessful) {
                val roomDto = resp.body()?.ocs?.data
                if (roomDto != null) {
                    val newChat = mapRoomToChat(roomDto)
                    val list = _chats.value.toMutableList()
                    if (list.none { it.id == newChat.id }) {
                        list.add(0, newChat)
                        _chats.value = list
                    }
                    newChat
                } else null
            } else null
        } catch (e: Exception) {
            handleNetworkError(e)
            null
        }
    }

    override suspend fun logout() {
        try {
            api.deleteAppPassword()
        } catch (_: Exception) {}
        cancelAllPolling()
        _chats.value = emptyList()
        _messages.value = emptyMap()
        _chatHistoryErrors.value = emptyMap()
        authStorage.clearCredentials()
    }

    override fun getMessages(chatId: String): Flow<List<Message>> {
        if (authStorage.isLoggedIn.value && chatId.isNotBlank()) {
            startLongPollingForRoom(chatId)
        }
        return _messages.map { map -> map[chatId] ?: emptyList() }
    }

    override fun getChatById(chatId: String): Flow<Chat?> {
        return _chats.map { list -> list.find { it.id == chatId } }
    }

    override fun getUserById(userId: String): User? {
        return _users.value.find { it.id == userId }
    }

    override fun getChatHistoryError(chatId: String): Flow<String?> {
        return _chatHistoryErrors.map { it[chatId] }
    }

    override fun retryLoadHistory(chatId: String) {
        _chatHistoryErrors.value = _chatHistoryErrors.value - chatId
        startLongPollingForRoom(chatId, forceReload = true)
    }

    override fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    override suspend fun sendMessage(
        chatId: String,
        content: String,
        replyToMessageId: String?,
        attachments: List<Attachment>
    ): Message {
        val currentUserId = _currentUser.value.id
        val localMessage = Message(
            id = "temp_${System.currentTimeMillis()}",
            chatId = chatId,
            senderId = currentUserId,
            content = content,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENDING,
            replyToMessageId = replyToMessageId,
            attachments = attachments
        )
        appendMessage(chatId, localMessage)

        if (authStorage.isLoggedIn.value) {
            return try {
                val response = api.sendMessage(token = chatId, message = content, replyTo = replyToMessageId)
                val serverMsg = mapDtoToMessage(response.ocs.data, roomToken = chatId)
                val finalMsg = if (serverMsg.attachments.isEmpty() && attachments.isNotEmpty()) {
                    serverMsg.copy(attachments = attachments)
                } else {
                    serverMsg
                }
                replaceMessage(chatId, localMessage.id, finalMsg)
                _isOffline.value = false
                finalMsg
            } catch (e: Exception) {
                handleNetworkError(e)
                val failedMsg = localMessage.copy(status = MessageStatus.FAILED)
                replaceMessage(chatId, localMessage.id, failedMsg)
                failedMsg
            }
        } else {
            val sentMsg = localMessage.copy(status = MessageStatus.READ)
            replaceMessage(chatId, localMessage.id, sentMsg)
            return sentMsg
        }
    }

    override suspend fun attachFile(chatId: String, attachment: Attachment): Message {
        return sendMessage(chatId, attachment.name, attachments = listOf(attachment))
    }

    override suspend fun editMessage(chatId: String, messageId: String, newContent: String) {
        try {
            val resp = api.editMessage(token = chatId, messageId = messageId, message = newContent)
            if (resp.isSuccessful) {
                val data = resp.body()?.ocs?.data
                // Requirement 4: The PUT response is a system message; the edited message is in its "parent"
                val parentDto = data?.parent
                if (parentDto != null) {
                    val editedMsg = mapDtoToMessage(parentDto, roomToken = chatId, isEdited = true)
                    replaceMessage(chatId, parentDto.id.toString(), editedMsg)
                } else if (data != null) {
                    val fallbackMsg = mapDtoToMessage(data, roomToken = chatId, isEdited = true)
                    replaceMessage(chatId, messageId, fallbackMsg)
                }
                _isOffline.value = false
            }
        } catch (e: Exception) {
            handleNetworkError(e)
        }
    }

    override suspend fun deleteMessage(chatId: String, messageId: String) {
        try {
            val resp = api.deleteMessage(token = chatId, messageId = messageId)
            if (resp.isSuccessful) {
                // Requirement 4: A deleted message shows as one muted italic line
                markMessageDeleted(chatId, messageId)
                _isOffline.value = false
            }
        } catch (e: Exception) {
            handleNetworkError(e)
        }
    }

    override suspend fun addReaction(chatId: String, messageId: String, reactionEmoji: String) {
        try {
            val resp = api.reactToMessage(token = chatId, messageId = messageId, reaction = reactionEmoji)
            if (resp.isSuccessful) {
                // Locally update reactions
                val currentMap = _messages.value.toMutableMap()
                val list = (currentMap[chatId] ?: emptyList()).toMutableList()
                val idx = list.indexOfFirst { it.id == messageId }
                if (idx != -1) {
                    val old = list[idx]
                    val updatedReactions = old.reactions.toMutableMap()
                    updatedReactions[_currentUser.value.id] = reactionEmoji
                    list[idx] = old.copy(reactions = updatedReactions)
                    currentMap[chatId] = list
                    _messages.value = currentMap
                }
                _isOffline.value = false
            }
        } catch (e: Exception) {
            handleNetworkError(e)
        }
    }

    override suspend fun removeReaction(messageId: String, userId: String) {}
    override suspend fun markChatAsRead(chatId: String) {}

    override suspend fun togglePinChat(chatId: String) {
        val list = _chats.value.toMutableList()
        val index = list.indexOfFirst { it.id == chatId }
        if (index != -1) {
            list[index] = list[index].copy(isPinned = !list[index].isPinned)
            _chats.value = list.sortedWith(compareByDescending<Chat> { it.isPinned }.thenByDescending { it.updatedAt })
        }
    }

    override suspend fun toggleMuteChat(chatId: String) {
        val list = _chats.value.toMutableList()
        val index = list.indexOfFirst { it.id == chatId }
        if (index != -1) {
            list[index] = list[index].copy(isMuted = !list[index].isMuted)
            _chats.value = list
        }
    }

    override suspend fun updateCurrentUserPresence(status: PresenceStatus, customStatus: String?) {
        if (!authStorage.isLoggedIn.value) return
        val statusType = when (status) {
            PresenceStatus.ONLINE -> "online"
            PresenceStatus.AWAY -> "away"
            PresenceStatus.BUSY -> "dnd"
            PresenceStatus.OFFLINE -> "invisible"
        }
        try {
            val response = api.setUserStatus(statusType)
            if (response.isSuccessful) {
                _currentUser.update { it.copy(status = status, customStatus = customStatus) }
            } else if (response.code() == 401) {
                handleSessionRevoked()
            } else {
                Log.w("NextcloudTalkRepo", "Status update failed: HTTP ${response.code()}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleNetworkError(e)
        }
    }

    override suspend fun setTypingStatus(chatId: String, isTyping: Boolean) {}

    override suspend fun downloadFile(
        attachment: Attachment,
        targetFile: File,
        onProgress: (Float) -> Unit
    ): Result<File> {
        val serverUrl = (authStorage.credentials.value?.serverUrl ?: NextcloudAuthStorage.DEFAULT_SERVER_URL).trimEnd('/')
        val currentUserId = _currentUser.value.id
        val cleanPath = attachment.path.trimStart('/')

        // Candidate URLs to download from in strict order of success likelihood
        val candidateUrls = mutableListOf<String>()

        // Resolve the original by file ID, using the recipient's access rights.
        // A shared-file browser link or a preview endpoint is not the file itself.
        if (attachment.id.isNotBlank() && attachment.id.all { it.isDigit() }) {
            try {
                val response = api.getDirectDownload(attachment.id)
                response.body()?.ocs?.data?.get("url")?.takeIf { response.isSuccessful && it.isNotBlank() }
                    ?.let { candidateUrls.add(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("NextcloudTalkRepo", "Direct download lookup failed: ${e.message}")
            }
        }

        // 1. Nextcloud Talk Share token / Share link
        val sToken = attachment.shareToken
        if (!sToken.isNullOrBlank()) {
            candidateUrls.add("$serverUrl/index.php/s/$sToken/download")
        }
        val sLink = attachment.shareLink
        if (!sLink.isNullOrBlank() && sLink.contains("/s/")) {
            if (sLink.endsWith("/download")) {
                candidateUrls.add(sLink)
            } else {
                candidateUrls.add("${sLink.trimEnd('/')}/download")
                candidateUrls.add(sLink)
            }
        }

        // 3. Direct attachment.url (including segment-encoded URL)
        if (attachment.url.isNotBlank() && !attachment.url.contains("/core/preview") &&
            !attachment.url.contains("/apps/files/api/") && !Regex("/f/\\d+").containsMatchIn(attachment.url)) {
            val direct = attachment.url.trim()
            val fullDirect = if (direct.startsWith("http://") || direct.startsWith("https://")) {
                direct
            } else {
                "$serverUrl/${direct.trimStart('/')}"
            }
            try {
                val uri = android.net.Uri.parse(fullDirect)
                val scheme = uri.scheme ?: "https"
                val host = uri.host ?: ""
                val portStr = if (uri.port != -1 && uri.port != 80 && uri.port != 443) ":${uri.port}" else ""
                val path = uri.path.orEmpty()
                val encodedPath = path.split("/").joinToString("/") { segment ->
                    if (segment.isEmpty()) "" else java.net.URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
                }
                val queryStr = if (!uri.query.isNullOrBlank()) "?${uri.query}" else ""
                candidateUrls.add("$scheme://$host$portStr$encodedPath$queryStr")
            } catch (_: Exception) {}
            candidateUrls.add(fullDirect)
        }

        // 4. WebDAV paths for sender (ownerId) and recipient (currentUserId)
        val urlOwnerMatch = Regex("/remote\\.php/dav/files/([^/]+)/").find(attachment.url)
        val extractedOwner = urlOwnerMatch?.groupValues?.getOrNull(1)

        val owners = listOfNotNull(
            currentUserId.takeIf { it.isNotBlank() },
            attachment.ownerId?.takeIf { it.isNotBlank() },
            extractedOwner?.takeIf { it.isNotBlank() && it != attachment.ownerId },
            currentUserId.takeIf { it.isNotBlank() && it != attachment.ownerId }
        ).distinct()

        if (cleanPath.isNotBlank()) {
            val encodedCleanPath = cleanPath.split("/").joinToString("/") { segment ->
                if (segment.isEmpty()) "" else java.net.URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
            }
            for (owner in owners) {
                val encodedOwner = java.net.URLEncoder.encode(owner, "UTF-8").replace("+", "%20")
                // Check Talk/ subfolder (Nextcloud Talk's default attachment folder)
                if (!cleanPath.startsWith("Talk/", ignoreCase = true)) {
                    candidateUrls.add("$serverUrl/remote.php/dav/files/$encodedOwner/Talk/$encodedCleanPath")
                    candidateUrls.add("$serverUrl/remote.php/dav/files/$owner/Talk/$cleanPath")
                }
                candidateUrls.add("$serverUrl/remote.php/dav/files/$encodedOwner/$encodedCleanPath")
                candidateUrls.add("$serverUrl/remote.php/dav/files/$owner/$cleanPath")
            }
            candidateUrls.add("$serverUrl/remote.php/webdav/Talk/$encodedCleanPath")
            candidateUrls.add("$serverUrl/remote.php/webdav/$encodedCleanPath")
        }

        val result = AttachmentDownloader(okHttpClient, report = {
            Log.i("AttachmentTransfer", it)
        }).download(candidateUrls, targetFile, attachment.mimeType, onProgress)
        if (result.isSuccess) _isOffline.value = false
        return result
    }
    private class ProgressRequestBody(
        private val contentType: okhttp3.MediaType?,
        private val context: Context,
        private val fileUri: Uri,
        private val totalLength: Long,
        private val onProgress: (Float) -> Unit
    ) : RequestBody() {
        override fun contentType() = contentType

        override fun contentLength() = if (totalLength > 0) totalLength else -1L

        override fun writeTo(sink: BufferedSink) {
            val inputStream = context.contentResolver.openInputStream(fileUri)
                ?: throw IOException("Cannot open input stream for $fileUri")
            inputStream.use { input ->
                val buffer = ByteArray(8192)
                var uploaded = 0L
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    sink.write(buffer, 0, read)
                    uploaded += read
                    if (totalLength > 0) {
                        onProgress((uploaded.toFloat() / totalLength).coerceIn(0f, 1f))
                    }
                }
            }
        }
    }

    override suspend fun uploadAndShareFile(
        chatId: String,
        fileUri: Uri,
        fileName: String,
        mimeType: String,
        fileSize: Long,
        context: Context,
        onProgress: (Float) -> Unit
    ): Result<Message> {
        val currentUserId = _currentUser.value.id
        if (currentUserId.isBlank()) {
            return Result.failure(IllegalStateException("User is not logged in"))
        }

        val sanitizedName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val uploadKey = "${System.currentTimeMillis()}${java.util.UUID.randomUUID().toString().take(6)}"
        val uniqueFileName = "${uploadKey}_$sanitizedName"

        val isImg = mimeType.startsWith("image/")
        val attType = when {
            isImg -> AttachmentType.IMAGE
            mimeType.startsWith("video/") -> AttachmentType.VIDEO
            mimeType.startsWith("audio/") -> AttachmentType.AUDIO
            mimeType == "application/vnd.android.package-archive" || fileName.endsWith(".apk", ignoreCase = true) -> AttachmentType.FILE
            mimeType.contains("pdf") || mimeType.contains("document") || mimeType.startsWith("text/") -> AttachmentType.DOCUMENT
            else -> AttachmentType.FILE
        }

        val localAtt = Attachment(
            id = "upload_$uploadKey",
            type = attType,
            url = fileUri.toString(),
            name = fileName,
            sizeBytes = fileSize,
            mimeType = mimeType,
            thumbnailUrl = if (isImg) fileUri.toString() else null,
            path = "Talk/$uniqueFileName"
        )

        val localMessage = Message(
            id = "temp_upload_$uploadKey",
            chatId = chatId,
            senderId = currentUserId,
            content = fileName,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENDING,
            attachments = listOf(localAtt)
        )
        appendMessage(chatId, localMessage)

        return try {
            val mediaType = mimeType.toMediaTypeOrNull()
            val requestBody = ProgressRequestBody(
                contentType = mediaType,
                context = context,
                fileUri = fileUri,
                totalLength = fileSize,
                onProgress = onProgress
            )

            var putResp = api.uploadFileWebDav(
                userId = currentUserId,
                fileName = uniqueFileName,
                fileBody = requestBody
            )

            // If 404/409, ensure Talk folder exists via MKCOL and retry once
            if (!putResp.isSuccessful && (putResp.code() == 404 || putResp.code() == 409)) {
                try {
                    api.createTalkFolder(currentUserId)
                } catch (_: Exception) {}
                val retryBody = ProgressRequestBody(
                    contentType = mediaType,
                    context = context,
                    fileUri = fileUri,
                    totalLength = fileSize,
                    onProgress = onProgress
                )
                putResp = api.uploadFileWebDav(
                    userId = currentUserId,
                    fileName = uniqueFileName,
                    fileBody = retryBody
                )
            }

            if (!putResp.isSuccessful) {
                val err = "Upload failed with HTTP ${putResp.code()}: ${putResp.message()}"
                val failedMsg = localMessage.copy(status = MessageStatus.FAILED)
                replaceMessage(chatId, localMessage.id, failedMsg)
                return Result.failure(Exception(err))
            }

            val shareResp = api.shareFileToRoom(
                path = "/Talk/$uniqueFileName",
                shareType = 10,
                shareWith = chatId
            )

            if (shareResp.isSuccessful) {
                val successMsg = localMessage.copy(status = MessageStatus.SENT)
                replaceMessage(chatId, localMessage.id, successMsg)
                // The server message for this file arrives with the next sync; the placeholder is
                // dropped as soon as it does, so one file is always one bubble.
                syncRoomMessages(chatId)
                _isOffline.value = false
                Result.success(successMsg)
            } else {
                val err = "Share failed with HTTP ${shareResp.code()}: ${shareResp.message()}"
                val failedMsg = localMessage.copy(status = MessageStatus.FAILED)
                replaceMessage(chatId, localMessage.id, failedMsg)
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            handleNetworkError(e)
            val failedMsg = localMessage.copy(status = MessageStatus.FAILED)
            replaceMessage(chatId, localMessage.id, failedMsg)
            Result.failure(e)
        }
    }

    override suspend fun latestUserMessage(chatId: String): Message? {
        if (!authStorage.isLoggedIn.value || chatId.isBlank()) return null
        return try {
            val response = api.getMessageHistory(token = chatId, lookIntoFuture = 0, limit = 50)
            if (!response.isSuccessful) return null
            (response.body()?.ocs?.data ?: emptyList())
                .filter { it.systemMessage.isNullOrBlank() }
                .sortedBy { it.id }
                .map { mapDtoToMessage(it, roomToken = chatId) }
                .lastOrNull { !it.isDeleted && !it.isSystemMessage && it.content.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun syncRoomMessages(chatId: String) {
        if (!authStorage.isLoggedIn.value || chatId.isBlank()) return
        try {
            val response = api.getMessageHistory(token = chatId, lookIntoFuture = 0, limit = 50)
            if (response.isSuccessful) {
                val dtos = response.body()?.ocs?.data ?: emptyList()
                val sortedDtos = dtos.sortedBy { it.id }
                val currentList = _messages.value[chatId] ?: emptyList()
                val updatedList = processIncomingDtos(currentList, sortedDtos, roomToken = chatId)

                val currentMap = _messages.value.toMutableMap()
                currentMap[chatId] = updatedList
                _messages.value = currentMap

                val maxId = dtos.maxOfOrNull { it.id } ?: 0L
                if (maxId > 0) {
                    val prevMax = lastKnownMessageIds[chatId] ?: 0L
                    lastKnownMessageIds[chatId] = maxOf(prevMax, maxId)
                }

                val latestMsg = updatedList.lastOrNull { !it.isSystemMessage }
                if (latestMsg != null) {
                    val list = _chats.value.toMutableList()
                    val idx = list.indexOfFirst { it.id == chatId }
                    if (idx != -1) {
                        list[idx] = list[idx].copy(
                            lastMessage = latestMsg,
                            updatedAt = latestMsg.timestamp
                        )
                        _chats.value = list
                    }
                }
                val currentRoom = _chats.value.find { it.id == chatId }
                val otherUserId = currentRoom?.participantIds?.find { it != _currentUser.value.id }
                if (!otherUserId.isNullOrBlank()) {
                    refreshUserPresence(otherUserId, chatId)
                }
                _isOffline.value = false
            } else if (response.code() == 401) {
                handleSessionRevoked()
            }
        } catch (e: Exception) {
            handleNetworkError(e)
        }
    }

    private fun handleSessionRevoked() {
        _isSessionRevoked.value = true
        scope.launch {
            authStorage.clearCredentials()
        }
    }

    private fun handleNetworkError(e: Throwable) {
        if (e is IOException) {
            _isOffline.value = true
        } else if (e is HttpException && e.code() == 401) {
            handleSessionRevoked()
        }
    }

    private fun cancelAllPolling() {
        for ((_, job) in pollingJobs) {
            if (job.isActive) job.cancel()
        }
        pollingJobs.clear()
        lastKnownMessageIds.clear()
    }

    private fun startLongPollingForRoom(token: String, forceReload: Boolean = false) {
        for ((activeToken, job) in pollingJobs) {
            if (activeToken != token && job.isActive) {
                job.cancel()
            }
        }

        if (!forceReload && pollingJobs[token]?.isActive == true) return

        pollingJobs[token]?.cancel()
        pollingJobs[token] = scope.launch {
            // Requirement 1 & 2: Fetch history and sort oldest-to-newest by ID
            try {
                val historyResp = api.getMessageHistory(token = token, lookIntoFuture = 0, limit = 100)
                if (historyResp.isSuccessful) {
                    val dtos = historyResp.body()?.ocs?.data ?: emptyList()
                    // Sort history oldest-to-newest by numeric id
                    val sortedDtos = dtos.sortedBy { it.id }
                    val messages = processIncomingDtos(emptyList(), sortedDtos, roomToken = token)
                    val currentMap = _messages.value.toMutableMap()
                    currentMap[token] = messages
                    _messages.value = currentMap
                    _chatHistoryErrors.value = _chatHistoryErrors.value - token

                    val maxId = dtos.maxOfOrNull { it.id } ?: 0L
                    lastKnownMessageIds[token] = maxId
                    _isOffline.value = false
                } else {
                    val err = "HTTP ${historyResp.code()}: ${historyResp.message()}"
                    Log.e("NextcloudTalkRepo", "Failed to load history for room $token: $err")
                    _chatHistoryErrors.value = _chatHistoryErrors.value + (token to err)
                    if (historyResp.code() == 401) {
                        handleSessionRevoked()
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.e("NextcloudTalkRepo", "Exception loading history for room $token", e)
                _chatHistoryErrors.value = _chatHistoryErrors.value + (token to (e.message ?: "Could not load messages"))
                handleNetworkError(e)
            }

            // Real-time long polling loop: lookIntoFuture=1, timeout=30
            while (isActive) {
                val lastId = lastKnownMessageIds[token] ?: 0L
                try {
                    val pollResp = api.pollNewMessages(
                        token = token,
                        lookIntoFuture = 1,
                        lastKnownMessageId = lastId,
                        timeout = 30
                    )

                    if (pollResp.code() == 304) {
                        _isOffline.value = false
                        continue
                    } else if (pollResp.isSuccessful) {
                        _isOffline.value = false
                        val newDtos = pollResp.body()?.ocs?.data ?: emptyList()
                        if (newDtos.isNotEmpty()) {
                            val sortedNewDtos = newDtos.sortedBy { it.id }
                            val currentList = _messages.value[token] ?: emptyList()
                            val updatedList = processIncomingDtos(currentList, sortedNewDtos, roomToken = token)
                            val currentMap = _messages.value.toMutableMap()
                            currentMap[token] = updatedList
                            _messages.value = currentMap

                            val maxId = newDtos.maxOfOrNull { it.id } ?: lastId
                            lastKnownMessageIds[token] = maxOf(lastId, maxId)
                        }
                    } else if (pollResp.code() == 401) {
                        handleSessionRevoked()
                        break
                    } else {
                        delay(3000)
                    }
                } catch (e: Exception) {
                    handleNetworkError(e)
                    delay(3000)
                }
            }
        }
    }

    private val HIDDEN_SYSTEM_MESSAGES = setOf(
        "message_deleted",
        "message_edited",
        "reaction",
        "reaction_revoked",
        "reaction_deleted"
    )

    /**
     * Requirement 1 & 4:
     * - Filters out hidden system messages from becoming bubbles
     * - Uses their "parent" field to update original messages
     * - Sorts oldest-to-newest by ID
     */
    private fun processIncomingDtos(
        existing: List<Message>,
        incomingDtos: List<NextcloudMessageDto>,
        roomToken: String
    ): List<Message> {
        val list = existing.toMutableList()

        for (dto in incomingDtos) {
            val sysMsg = dto.systemMessage?.trim() ?: ""

            if (sysMsg in HIDDEN_SYSTEM_MESSAGES) {
                val targetId = dto.parent?.id?.toString() ?: ""

                when (sysMsg) {
                    "message_deleted" -> {
                        val idx = list.indexOfFirst { it.id == targetId }
                        if (idx != -1) {
                            list[idx] = list[idx].copy(
                                isDeleted = true,
                                content = "This message was deleted",
                                attachments = emptyList(),
                                reactions = emptyMap()
                            )
                        }
                    }
                    "message_edited" -> {
                        val parentDto = dto.parent
                        if (parentDto != null) {
                            val editedId = parentDto.id.toString()
                            val idx = list.indexOfFirst { it.id == editedId }
                            val editedMsg = mapDtoToMessage(parentDto, roomToken, isEdited = true)
                            if (idx != -1) {
                                list[idx] = editedMsg
                            } else {
                                list.add(editedMsg)
                            }
                        }
                    }
                    "reaction", "reaction_revoked", "reaction_deleted" -> {
                        val parentDto = dto.parent
                        if (parentDto != null) {
                            val targetParentId = parentDto.id.toString()
                            val idx = list.indexOfFirst { it.id == targetParentId }
                            if (idx != -1) {
                                val newReactions = parseReactions(parentDto.reactions)
                                list[idx] = list[idx].copy(reactions = newReactions)
                            }
                        }
                    }
                }
                // Never add hidden system message as a bubble
            } else {
                val isOtherSystemMsg = sysMsg.isNotBlank() || dto.messageType == "system"
                val msg = mapDtoToMessage(dto, roomToken, isSystemMessage = isOtherSystemMsg)
                val idx = list.indexOfFirst { it.id == msg.id }
                if (idx != -1) {
                    val existingMsg = list[idx]
                    val finalMsg = if (msg.attachments.isEmpty() && existingMsg.attachments.isNotEmpty()) {
                        msg.copy(attachments = existingMsg.attachments)
                    } else {
                        msg
                    }
                    list[idx] = finalMsg
                } else {
                    list.add(msg)
                }
            }
        }

        dropSettledUploads(list)
        // Sort oldest-to-newest by numeric id, then local temp messages by timestamp
        return list.sortedWith(
            compareBy<Message> { it.id.toLongOrNull() ?: Long.MAX_VALUE }
                .thenBy { it.timestamp }
        )
    }

    private fun appendMessage(token: String, message: Message) {
        val currentMap = _messages.value.toMutableMap()
        val list = (currentMap[token] ?: emptyList()).toMutableList()
        list.add(message)
        currentMap[token] = list.sortedWith(
            compareBy<Message> { it.id.toLongOrNull() ?: Long.MAX_VALUE }
                .thenBy { it.timestamp }
        )
        _messages.value = currentMap
    }

    /** Removes local "uploading" placeholders whose file is already in the chat as a server message. */
    private fun dropSettledUploads(list: MutableList<Message>) {
        list.removeAll { local ->
            if (!local.id.startsWith("temp_upload_") || local.status == MessageStatus.FAILED) return@removeAll false
            val uploaded = local.attachments.firstOrNull()?.path?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                ?: return@removeAll false
            list.any { server ->
                server.id.toLongOrNull() != null && server.senderId == local.senderId &&
                    server.attachments.any { it.name == uploaded || it.path.substringAfterLast('/') == uploaded }
            }
        }
    }

    private fun replaceMessage(token: String, oldId: String, newMsg: Message) {
        val currentMap = _messages.value.toMutableMap()
        val list = (currentMap[token] ?: emptyList()).toMutableList()
        // A sync may already have added the server copy of this message: keep just one.
        if (newMsg.id != oldId && list.any { it.id == newMsg.id }) {
            list.removeAll { it.id == oldId }
            currentMap[token] = list
            _messages.value = currentMap
            return
        }
        val idx = list.indexOfFirst { it.id == oldId }
        if (idx != -1) {
            list[idx] = newMsg
        } else {
            list.add(newMsg)
        }
        dropSettledUploads(list)
        currentMap[token] = list.sortedWith(
            compareBy<Message> { it.id.toLongOrNull() ?: Long.MAX_VALUE }
                .thenBy { it.timestamp }
        )
        _messages.value = currentMap
    }

    private fun markMessageDeleted(token: String, messageId: String) {
        val currentMap = _messages.value.toMutableMap()
        val list = (currentMap[token] ?: emptyList()).toMutableList()
        val idx = list.indexOfFirst { it.id == messageId }
        if (idx != -1) {
            list[idx] = list[idx].copy(
                isDeleted = true,
                content = "This message was deleted",
                attachments = emptyList(),
                reactions = emptyMap()
            )
            currentMap[token] = list
            _messages.value = currentMap
        }
    }

    private suspend fun mapRoomToChat(dto: NextcloudRoomDto): Chat {
        val chatType = if (dto.type == 1) ChatType.DIRECT else ChatType.GROUP
        val lastMsg = dto.lastMessage?.let { mapDtoToMessage(it, roomToken = dto.token, isSystemMessage = !it.systemMessage.isNullOrBlank()) }
        // Room v4 responses do not contain the participant list. Resolve it separately;
        // otherwise the UI has no other user ID with which to observe presence.
        var participantIds = _chats.value.find { it.id == dto.token }?.participantIds
            ?: listOf(_currentUser.value.id)
        if (chatType == ChatType.DIRECT) {
            try {
                val response = api.getRoomParticipants(dto.token)
                if (response.isSuccessful) {
                    val participants = response.body()?.ocs?.data.orEmpty()
                    participantIds = participantUserIds(_currentUser.value.id, participants)
                    val serverUrl = authStorage.credentials.value?.serverUrl
                        ?: NextcloudAuthStorage.DEFAULT_SERVER_URL
                    val users = participants.filter { it.actorType == "users" && it.actorId.isNotBlank() }
                        .filter { it.actorId != _currentUser.value.id }.map { participant ->
                            val presence = refreshUserPresence(participant.actorId, dto.token)
                            User(
                                id = participant.actorId,
                                name = participant.displayName?.ifBlank { null } ?: participant.actorId,
                                avatarUrl = "$serverUrl/index.php/avatar/${participant.actorId}/128",
                                status = presence
                            )
                        }
                    _users.update { existing ->
                        (existing.filter { user -> users.none { it.id == user.id } } + users)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("NextcloudTalkRepo", "Could not refresh room participants: ${e.message}")
            }
        }
        return Chat(
            id = dto.token,
            type = chatType,
            title = dto.displayName.ifBlank { dto.name ?: "Conversation" },
            // Group and public rooms: load the room picture (team emblems) from the server.
            avatarUrl = if (dto.type == 2 || dto.type == 3) {
                val base = authStorage.credentials.value?.serverUrl ?: NextcloudAuthStorage.DEFAULT_SERVER_URL
                "$base/ocs/v2.php/apps/spreed/api/v1/room/${dto.token}/avatar" + (dto.avatarVersion?.let { "?v=$it" } ?: "")
            } else null,
            participantIds = participantIds,
            lastMessage = lastMsg,
            unreadCount = dto.unreadMessages,
            isPinned = dto.isFavorite,
            isMuted = false,
            updatedAt = if (dto.lastActivity > 0) dto.lastActivity * 1000L else (lastMsg?.timestamp ?: System.currentTimeMillis())
        )
    }

    private fun mapDtoToMessage(
        dto: NextcloudMessageDto,
        roomToken: String,
        isEdited: Boolean = false,
        isSystemMessage: Boolean = false
    ): Message {
        val isMe = dto.actorId == _currentUser.value.id
        val formattedContent = formatMessageContent(dto.message, dto.messageParameters)
        val reactionsMap = parseReactions(dto.reactions)
        val serverUrl = authStorage.credentials.value?.serverUrl ?: NextcloudAuthStorage.DEFAULT_SERVER_URL

        val roomTokenStr = if (dto.token.isNotBlank()) dto.token else roomToken
        var attachments = extractAttachments(dto.messageParameters, dto.actorId, serverUrl, roomTokenStr)
        if (attachments.isEmpty() && isMediaOrFileUrl(dto.message)) {
            parseUrlAttachment(dto.message)?.let {
                attachments = listOf(it)
            }
        }

        return Message(
            id = dto.id.toString(),
            chatId = roomTokenStr,
            senderId = dto.actorId ?: "unknown",
            content = formattedContent,
            timestamp = dto.timestamp * 1000L,
            status = if (isMe) MessageStatus.READ else MessageStatus.DELIVERED,
            reactions = reactionsMap,
            replyToMessageId = dto.parent?.id?.toString(),
            attachments = attachments,
            isEdited = isEdited,
            isDeleted = false,
            isSystemMessage = isSystemMessage,
            senderName = dto.actorDisplayName
        )
    }

    private fun extractAttachments(
        messageParameters: Any?,
        actorId: String?,
        serverUrl: String,
        roomToken: String = ""
    ): List<Attachment> {
        val attachments = mutableListOf<Attachment>()
        if (messageParameters is Map<*, *>) {
            val fileObj = messageParameters["file"]
            if (fileObj is Map<*, *>) {
                parseFileMap(fileObj, actorId, serverUrl, roomToken)?.let { attachments.add(it) }
            } else if (fileObj is List<*>) {
                for (item in fileObj) {
                    if (item is Map<*, *>) {
                        parseFileMap(item, actorId, serverUrl, roomToken)?.let { attachments.add(it) }
                    }
                }
            }

            val filesObj = messageParameters["files"]
            if (filesObj is List<*>) {
                for (item in filesObj) {
                    if (item is Map<*, *>) {
                        parseFileMap(item, actorId, serverUrl, roomToken)?.let { attachments.add(it) }
                    }
                }
            } else if (filesObj is Map<*, *>) {
                parseFileMap(filesObj, actorId, serverUrl, roomToken)?.let { attachments.add(it) }
            }

            for ((key, value) in messageParameters) {
                if (key != "file" && key != "files" && value is Map<*, *>) {
                    if (value["mimetype"] != null || value["path"] != null || value["type"] == "file") {
                        parseFileMap(value, actorId, serverUrl, roomToken)?.let { attachments.add(it) }
                    }
                }
            }
        } else if (messageParameters is List<*>) {
            for (item in messageParameters) {
                if (item is Map<*, *>) {
                    parseFileMap(item, actorId, serverUrl, roomToken)?.let { attachments.add(it) }
                }
            }
        }
        return attachments.distinctBy { it.name + it.sizeBytes }
    }

    private fun parseFileMap(
        fileMap: Map<*, *>,
        actorId: String?,
        serverUrl: String,
        roomToken: String = ""
    ): Attachment? {
        val id = normalizeAttachmentId(fileMap["id"])
        val name = fileMap["name"]?.toString() ?: fileMap["filename"]?.toString() ?: "file"
        val path = fileMap["path"]?.toString() ?: fileMap["file"]?.toString() ?: ""
        val mimeType = fileMap["mimetype"]?.toString()
            ?: fileMap["mimeType"]?.toString()
            ?: guessMimeType(name)
        val size = (fileMap["size"] as? Number)?.toLong()
            ?: fileMap["size"]?.toString()?.toLongOrNull()
            ?: 0L

        val shareToken = fileMap["share-token"]?.toString()
            ?: fileMap["shareToken"]?.toString()
            ?: fileMap["token"]?.toString()
        val shareLink = fileMap["link"]?.toString()
            ?: fileMap["url"]?.toString()

        val isImage = mimeType.startsWith("image/")
        val type = when {
            isImage -> AttachmentType.IMAGE
            mimeType.startsWith("video/") -> AttachmentType.VIDEO
            mimeType.startsWith("audio/") -> AttachmentType.AUDIO
            mimeType == "application/vnd.android.package-archive" || name.endsWith(".apk", ignoreCase = true) -> AttachmentType.FILE
            mimeType.startsWith("text/") || mimeType.contains("pdf") || mimeType.contains("document") || mimeType.contains("sheet") -> AttachmentType.DOCUMENT
            else -> AttachmentType.FILE
        }

        val cleanPath = path.trimStart('/')
        val encodedCleanPath = cleanPath.split("/").joinToString("/") { segment ->
            if (segment.isEmpty()) "" else java.net.URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
        }
        val owner = actorId ?: _currentUser.value.id
        val encodedOwner = java.net.URLEncoder.encode(owner, "UTF-8").replace("+", "%20")

        val previewUrl = if (id.isNotBlank()) {
            "$serverUrl/index.php/core/preview?fileId=$id&x=1024&y=1024&a=1"
        } else {
            "$serverUrl/remote.php/dav/files/$encodedOwner/$encodedCleanPath"
        }

        val downloadUrl = when {
            !shareToken.isNullOrBlank() -> "$serverUrl/index.php/s/$shareToken/download"
            !shareLink.isNullOrBlank() -> if (shareLink.endsWith("/download")) shareLink else "${shareLink.trimEnd('/')}/download"
            cleanPath.isNotBlank() -> "$serverUrl/remote.php/dav/files/$encodedOwner/$encodedCleanPath"
            id.isNotBlank() -> "$serverUrl/index.php/core/preview?fileId=$id&x=4096&y=4096"
            else -> previewUrl
        }

        return Attachment(
            id = if (id.isNotBlank()) id else "att_${System.currentTimeMillis()}",
            type = type,
            url = downloadUrl,
            name = name,
            sizeBytes = size,
            mimeType = mimeType,
            thumbnailUrl = previewUrl,
            path = cleanPath,
            shareToken = shareToken,
            shareLink = shareLink,
            ownerId = actorId,
            chatToken = roomToken.ifBlank { null }
        )
    }

    private fun isMediaOrFileUrl(text: String): Boolean {
        val trimmed = text.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return false
        val path = trimmed.substringBefore('?').lowercase()
        return path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png") ||
               path.endsWith(".webp") || path.endsWith(".gif") || path.endsWith(".pdf") ||
               path.endsWith(".apk") || path.endsWith(".zip") || path.endsWith(".mp4") ||
               path.endsWith(".mp3") || trimmed.contains("/remote.php/dav/files/") ||
               trimmed.contains("/index.php/apps/files/")
    }

    private fun parseUrlAttachment(url: String): Attachment? {
        val cleanUrl = url.trim()
        val path = cleanUrl.substringBefore('?')
        val name = path.substringAfterLast('/').ifBlank { "attachment" }
        val mime = guessMimeType(name)
        val isImg = mime.startsWith("image/")
        return Attachment(
            id = "url_${System.currentTimeMillis()}",
            type = if (isImg) AttachmentType.IMAGE else if (name.endsWith(".apk", ignoreCase = true)) AttachmentType.FILE else AttachmentType.DOCUMENT,
            url = cleanUrl,
            name = name,
            sizeBytes = 0L,
            mimeType = mime,
            thumbnailUrl = if (isImg) cleanUrl else null,
            path = ""
        )
    }

    private fun guessMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            "pdf" -> "application/pdf"
            "apk" -> "application/vnd.android.package-archive"
            "zip" -> "application/zip"
            "txt" -> "text/plain"
            "doc", "docx" -> "application/msword"
            else -> "application/octet-stream"
        }
    }

    private fun parseReactions(reactionsRaw: Any?): Map<String, String> {
        if (reactionsRaw !is Map<*, *>) return emptyMap()
        val result = mutableMapOf<String, String>()
        var dummyIndex = 0
        for ((emoji, users) in reactionsRaw) {
            val emojiStr = emoji?.toString() ?: continue
            if (users is List<*>) {
                for (u in users) {
                    result[u?.toString() ?: "user_${dummyIndex++}"] = emojiStr
                }
            } else if (users is Number) {
                val count = users.toInt()
                for (i in 0 until count) {
                    result["user_${emojiStr}_$i"] = emojiStr
                }
            } else {
                result["user_${dummyIndex++}"] = emojiStr
            }
        }
        return result
    }

    /**
     * Fills message placeholders like {user}, {actor}, {file} from each message's
     * messageParameters using the parameter's "name". Never rewrites text otherwise.
     */
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

    override suspend fun forwardMessage(targetChatId: String, message: Message): Result<Message> {
        return try {
            val sent = sendMessage(
                chatId = targetChatId,
                content = message.content,
                attachments = message.attachments
            )
            Result.success(sent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshUserPresence(userId: String, roomToken: String?): PresenceStatus {
        if (userId.isBlank() || !authStorage.isLoggedIn.value) return PresenceStatus.OFFLINE
        // Account status is authoritative. A room ping only describes activity in that room.
        try {
            val response = api.getUserStatus(userId)
            if (response.isSuccessful) {
                val data = response.body()?.ocs?.data
                val status = parsePresence(data?.status ?: data?.statusType)
                if (status != null) {
                    _userPresences.update { it + (userId to status) }
                    return status
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("NextcloudTalkRepo", "Account presence lookup failed: ${e.message}")
        }

        // Older servers may not expose user_status. Try room activity independently,
        // so failure of one endpoint does not prevent the other from being used.
        if (!roomToken.isNullOrBlank()) {
            try {
                val response = api.getRoomParticipants(roomToken)
                if (response.isSuccessful) {
                    val participant = response.body()?.ocs?.data?.find {
                        it.actorType == "users" && it.actorId == userId
                    }
                    if (participant != null) {
                        val status = resolvePresence(null, participant.status,
                            participant.lastPing, System.currentTimeMillis() / 1000L)
                        _userPresences.update { it + (userId to status) }
                        return status
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("NextcloudTalkRepo", "Room presence lookup failed: ${e.message}")
            }
        }
        return _userPresences.value[userId] ?: PresenceStatus.OFFLINE
    }
}

