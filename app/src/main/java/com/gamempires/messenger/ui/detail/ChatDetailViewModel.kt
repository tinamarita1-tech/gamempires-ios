package com.gamempires.messenger.ui.detail

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamempires.messenger.data.ChatRepository
import com.gamempires.messenger.model.Attachment
import com.gamempires.messenger.model.Chat
import com.gamempires.messenger.model.Message
import com.gamempires.messenger.model.PresenceStatus
import com.gamempires.messenger.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ChatDetailUiState(
    val chat: Chat? = null,
    val messages: List<Message> = emptyList(),
    val currentUser: User? = null,
    val otherUser: User? = null,
    val otherUserPresence: PresenceStatus? = null,
    val isOtherUserTyping: Boolean = false,
    val inputText: String = "",
    val isLoading: Boolean = false,
    val replyingToMessage: Message? = null,
    val isOffline: Boolean = false,
    val historyError: String? = null,
    val downloadProgress: Map<String, Float> = emptyMap(),
    val uploadProgress: Map<String, Float> = emptyMap(),
    val downloadedAttachmentIds: Set<String> = emptySet()
)

private data class ChatDetailContext(
    val chat: Chat?,
    val messages: List<Message>,
    val currentUser: User,
    val users: List<User>
)

private data class ChatInputMeta(
    val text: String,
    val replyMsg: Message?,
    val offline: Boolean,
    val historyError: String?,
    val downloadProgress: Map<String, Float>,
    val uploadProgress: Map<String, Float>,
    val downloadedAttachmentIds: Set<String>
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle,
    private val downloadedFiles: com.gamempires.messenger.data.DownloadedFiles
) : ViewModel() {

    private val _chatId = MutableStateFlow<String?>(savedStateHandle["chatId"])
    private val _inputText = MutableStateFlow("")
    private val _replyingTo = MutableStateFlow<Message?>(null)
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    private val _uploadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())

    private var syncJob: Job? = null

    val inputText: StateFlow<String> = _inputText.asStateFlow()
    val allChats: StateFlow<List<Chat>> = chatRepository.chats

    fun setChatId(id: String) {
        if (_chatId.value != id) {
            _chatId.value = id
            _replyingTo.value = null
            startActiveSync()
            viewModelScope.launch {
                chatRepository.markChatAsRead(id)
            }
        } else if (syncJob == null || syncJob?.isActive != true) {
            startActiveSync()
        }
    }

    fun startActiveSync() {
        val id = _chatId.value ?: return
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            while (isActive) {
                chatRepository.syncRoomMessages(id)
                delay(2500)
            }
        }
    }

    fun stopActiveSync() {
        syncJob?.cancel()
        syncJob = null
    }

    fun setReplyingTo(message: Message?) {
        _replyingTo.value = message
    }

    val uiState: StateFlow<ChatDetailUiState> = _chatId.flatMapLatest { id ->
        if (id == null) {
            flowOf(ChatDetailUiState())
        } else {
            val contextFlow = combine(
                chatRepository.getChatById(id),
                chatRepository.getMessages(id),
                chatRepository.currentUser,
                chatRepository.users
            ) { chat, messages, currentUser, users ->
                ChatDetailContext(chat, messages, currentUser, users)
            }

            val progressFlow = combine(_downloadProgress, _uploadProgress, downloadedFiles.files) { dl, up, saved -> Triple(dl, up, saved.keys) }
            val metaFlow = combine(
                _inputText,
                _replyingTo,
                chatRepository.isOffline,
                chatRepository.getChatHistoryError(id),
                progressFlow
            ) { text, reply, offline, histErr, progressPair ->
                ChatInputMeta(text, reply, offline, histErr, progressPair.first, progressPair.second, progressPair.third)
            }

            combine(
                contextFlow,
                chatRepository.userPresences,
                chatRepository.typingUsers,
                metaFlow
            ) { ctx, presences, typingUsers, meta ->
                val otherUserId = ctx.chat?.participantIds?.find { it != ctx.currentUser.id }
                val otherUser = if (otherUserId != null) ctx.users.find { it.id == otherUserId } else null
                val presence = if (otherUserId != null) presences[otherUserId] else null
                val isTyping = typingUsers[id]?.any { it != ctx.currentUser.id } ?: false

                ChatDetailUiState(
                    chat = ctx.chat,
                    messages = ctx.messages,
                    currentUser = ctx.currentUser,
                    otherUser = otherUser,
                    otherUserPresence = presence,
                    isOtherUserTyping = isTyping,
                    inputText = meta.text,
                    isLoading = false,
                    replyingToMessage = meta.replyMsg,
                    isOffline = meta.offline,
                    historyError = meta.historyError,
                    downloadProgress = meta.downloadProgress,
                    uploadProgress = meta.uploadProgress,
                    downloadedAttachmentIds = meta.downloadedAttachmentIds
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatDetailUiState(isLoading = true)
    )

    fun onInputTextChanged(text: String) {
        _inputText.value = text
        val id = _chatId.value ?: return
        viewModelScope.launch {
            chatRepository.setTypingStatus(id, text.isNotBlank())
        }
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        val id = _chatId.value ?: return
        if (text.isBlank()) return

        val replyToId = _replyingTo.value?.id

        viewModelScope.launch {
            chatRepository.sendMessage(
                chatId = id,
                content = text,
                replyToMessageId = replyToId
            )
            _inputText.value = ""
            _replyingTo.value = null
            chatRepository.setTypingStatus(id, false)
        }
    }

    fun retryLoadHistory() {
        val id = _chatId.value ?: return
        viewModelScope.launch {
            chatRepository.retryLoadHistory(id)
        }
    }

    fun editMessage(messageId: String, newContent: String) {
        val id = _chatId.value ?: return
        viewModelScope.launch {
            chatRepository.editMessage(id, messageId, newContent)
        }
    }

    fun deleteMessage(messageId: String) {
        val id = _chatId.value ?: return
        viewModelScope.launch {
            chatRepository.deleteMessage(id, messageId)
        }
    }

    fun addReaction(messageId: String, emoji: String) {
        val id = _chatId.value ?: return
        viewModelScope.launch {
            chatRepository.addReaction(id, messageId, emoji)
        }
    }

    private fun beginDownload(id: String): Boolean {
        while (true) {
            val current = _downloadProgress.value
            if (id in current) return false
            if (_downloadProgress.compareAndSet(current, current + (id to -1f))) return true
        }
    }
    fun downloadAndOpenFile(attachment: Attachment, context: Context) {
        if (!beginDownload(attachment.id)) return
        val downloadAccount = downloadedFiles.accountKey()
        viewModelScope.launch {

            try {
                downloadedFiles.existingUri(attachment.id, downloadAccount)?.let {
                    openContentUri(it, attachment.mimeType, context)
                    return@launch
                }
                val downloadsDir = File(context.cacheDir, "downloads").apply { mkdirs() }
                val targetFile = File(downloadsDir, attachment.name)
                val result = chatRepository.downloadFile(attachment, targetFile) { progress ->
                    _downloadProgress.update { it + (attachment.id to progress) }
                }

                if (result.isSuccess) {
                    saveFileToPublicDownloads(context, targetFile, attachment.name, attachment.mimeType)?.let {
                        downloadedFiles.markSaved(attachment.id, it, downloadAccount)
                    }
                    openFileWithMatchingApp(targetFile, attachment.mimeType, context)
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Download failed"
                    Toast.makeText(context, "Failed to download ${attachment.name}: $err", Toast.LENGTH_LONG).show()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {

                Toast.makeText(context, "Failed to download ${attachment.name}: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _downloadProgress.update { it - attachment.id }
            }
        }
    }

    fun saveToDownloads(attachment: Attachment, context: Context) {
        if (!beginDownload(attachment.id)) return
        val downloadAccount = downloadedFiles.accountKey()
        viewModelScope.launch {

            try {
                if (downloadedFiles.existingUri(attachment.id, downloadAccount) != null) {
                    Toast.makeText(context, "Already saved to Downloads", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val tempDir = File(context.cacheDir, "temp_downloads").apply { mkdirs() }
                val targetFile = File(tempDir, attachment.name)
                val result = chatRepository.downloadFile(attachment, targetFile) { progress ->
                    _downloadProgress.update { it + (attachment.id to progress) }
                }

                if (result.isSuccess) {
                    val uri = saveFileToPublicDownloads(context, targetFile, attachment.name, attachment.mimeType)
                    if (uri != null) {
                        downloadedFiles.markSaved(attachment.id, uri, downloadAccount)
                        Toast.makeText(context, "Saved ${attachment.name} to Downloads", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Could not save ${attachment.name} to Downloads", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Download failed"
                    Toast.makeText(context, "Failed to save ${attachment.name}: $err", Toast.LENGTH_LONG).show()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {

                Toast.makeText(context, "Failed to save ${attachment.name}: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _downloadProgress.update { it - attachment.id }
            }
        }
    }

    fun forwardMessage(targetChat: Chat, message: Message, context: Context) {
        viewModelScope.launch {
            val result = chatRepository.forwardMessage(targetChat.id, message)
            if (result.isSuccess) {
                Toast.makeText(context, "Forwarded to ${targetChat.title}", Toast.LENGTH_SHORT).show()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Forward failed"
                Toast.makeText(context, "Failed to forward: $err", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun shareMessageExternally(message: Message, context: Context) {
        if (message.attachments.isNotEmpty()) {
            val att = message.attachments.first()
            downloadAndShareFileExternally(att, context)
        } else {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, message.content)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share via...")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        }
    }

    fun downloadAndShareFileExternally(attachment: Attachment, context: Context) {
        if (!beginDownload(attachment.id)) return
        val downloadAccount = downloadedFiles.accountKey()
        viewModelScope.launch {

            try {
                val tempDir = File(context.cacheDir, "shared_files").apply { mkdirs() }
                val targetFile = File(tempDir, attachment.name)
                val result = chatRepository.downloadFile(attachment, targetFile) { progress ->
                    _downloadProgress.update { it + (attachment.id to progress) }
                }

                if (result.isSuccess) {
                    val contentUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        targetFile
                    )
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        type = attachment.mimeType.ifBlank { "*/*" }
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(shareIntent, "Share ${attachment.name} via...")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                } else {
                    Toast.makeText(context, "Failed to prepare file for sharing", Toast.LENGTH_SHORT).show()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {

                Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                _downloadProgress.update { it - attachment.id }
            }
        }
    }

    private suspend fun saveFileToPublicDownloads(
        context: Context,
        sourceFile: File,
        displayName: String,
        mimeType: String
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType.ifBlank { "application/octet-stream" })
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/GamEmpires")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) ?: return@withContext null
                try {
                    val stream = resolver.openOutputStream(uri) ?: throw java.io.IOException("Cannot write to Downloads")
                    stream.use { out ->
                        sourceFile.inputStream().use { input -> input.copyTo(out) }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    uri
                } catch (e: Exception) {
                    resolver.delete(uri, null, null)
                    throw e
                }
            } else {
                val targetDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val gamEmpiresDir = File(targetDir, "GamEmpires").apply { mkdirs() }
                val targetFile = File(gamEmpiresDir, displayName)
                sourceFile.copyTo(targetFile, overwrite = true)
                Uri.fromFile(targetFile)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("ChatDetailViewModel", "Could not copy to public downloads: ${e.message}")
            null
        }
    }

    private fun openFileWithMatchingApp(file: File, mimeType: String, context: Context) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            openContentUri(uri, mimeType, context)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openContentUri(uri: Uri, mimeType: String, context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, if (mimeType.isNotBlank()) mimeType else "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open with..."))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app found to open this file", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /** Sends the picked files one after another, so they arrive in the chat in the order picked. */
    fun uploadFiles(fileUris: List<Uri>, context: Context) {
        val id = _chatId.value ?: return
        viewModelScope.launch {
            fileUris.forEach { uploadOne(id, it, context) }
        }
    }

    fun uploadFile(fileUri: Uri, context: Context) = uploadFiles(listOf(fileUri), context)

    private suspend fun uploadOne(id: String, fileUri: Uri, context: Context) {
        run {
            try {
                var fileName = "file_${System.currentTimeMillis()}"
                var fileSize = 0L
                val mimeType = context.contentResolver.getType(fileUri) ?: "application/octet-stream"

                context.contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) {
                            val displayName = cursor.getString(nameIndex)
                            if (!displayName.isNullOrBlank()) fileName = displayName
                        }
                        if (sizeIndex != -1) {
                            fileSize = cursor.getLong(sizeIndex)
                        }
                    }
                }

                // The photo picker sometimes names files without an extension ("1000063123"); Nextcloud then
                // treats a photo as a plain file. Add the extension that matches the file type.
                if (!fileName.substringAfterLast('/').contains('.')) {
                    android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                        ?.let { fileName = "$fileName.$it" }
                }

                _uploadProgress.value = _uploadProgress.value + (fileName to 0f)
                val result = chatRepository.uploadAndShareFile(
                    chatId = id,
                    fileUri = fileUri,
                    fileName = fileName,
                    mimeType = mimeType,
                    fileSize = fileSize,
                    context = context
                ) { progress ->
                    _uploadProgress.value = _uploadProgress.value + (fileName to progress)
                }

                _uploadProgress.value = _uploadProgress.value - fileName
                if (result.isFailure) {
                    val err = result.exceptionOrNull()?.message ?: "Upload failed"
                    Toast.makeText(context, "Upload failed: $err", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot upload file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopActiveSync()
    }
}


