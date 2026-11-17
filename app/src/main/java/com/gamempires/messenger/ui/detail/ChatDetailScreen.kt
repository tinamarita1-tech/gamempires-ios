package com.gamempires.messenger.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.offset

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.gamempires.messenger.model.Attachment
import com.gamempires.messenger.model.AttachmentType
import com.gamempires.messenger.model.Chat
import com.gamempires.messenger.model.ChatType
import com.gamempires.messenger.model.Message
import com.gamempires.messenger.model.MessageStatus
import com.gamempires.messenger.model.PresenceStatus
import com.gamempires.messenger.ui.chatlist.components.UserAvatar
import com.gamempires.messenger.ui.components.GamEmpiresWatermarkBackground
import com.gamempires.messenger.ui.components.MarkdownText
import com.gamempires.messenger.ui.theme.GamEmpiresActionGold
import com.gamempires.messenger.ui.theme.GamEmpiresBg
import com.gamempires.messenger.ui.theme.GamEmpiresBorder
import com.gamempires.messenger.ui.theme.GamEmpiresCard
import com.gamempires.messenger.ui.theme.GamEmpiresGold
import com.gamempires.messenger.ui.theme.GamEmpiresGoldDark
import com.gamempires.messenger.ui.theme.GamEmpiresSurface
import com.gamempires.messenger.ui.theme.GamEmpiresTextMuted
import com.gamempires.messenger.ui.theme.GamEmpiresTextPrimary
import com.gamempires.messenger.ui.theme.GamEmpiresTextSecondary
import com.gamempires.messenger.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    uiState: ChatDetailUiState,
    onBackClick: () -> Unit,
    onInputTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onAddReaction: (messageId: String, emoji: String) -> Unit,
    onEditMessage: (messageId: String, newContent: String) -> Unit = { _, _ -> },
    onDeleteMessage: (messageId: String) -> Unit = {},
    onSetReplyingTo: (Message?) -> Unit = {},
    onRetryLoadHistory: () -> Unit = {},
    onDownloadAttachment: (Attachment) -> Unit = {},
    allChats: List<Chat> = emptyList(),
    onSaveToDownloads: (Attachment) -> Unit = {},
    onForwardMessage: (Chat, Message) -> Unit = { _, _ -> },
    onShareMessage: (Message) -> Unit = {},
    onShareAttachment: (Attachment) -> Unit = {},
    onUploadFiles: (List<Uri>) -> Unit = {},
    onStartSync: () -> Unit = {},
    onStopSync: () -> Unit = {},
    onStartAudioCall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // One scroll position per conversation; reversed list starts at the newest message (no jump on open).
    val listState = remember(uiState.chat?.id) { androidx.compose.foundation.lazy.LazyListState() }
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, uiState.chat?.id) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                onStartSync()
            } else if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE) {
                onStopSync()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onStartSync()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            onStopSync()
        }
    }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var inChatSearchQuery by rememberSaveable { mutableStateOf("") }

    var selectedMessageForMenu by remember { mutableStateOf<Message?>(null) }
    var messageToEdit by remember { mutableStateOf<Message?>(null) }
    var messageToDelete by remember { mutableStateOf<Message?>(null) }
    var messageToForward by remember { mutableStateOf<Message?>(null) }
    var forwardSearchQuery by remember { mutableStateOf("") }
    var fullScreenAttachment by remember { mutableStateOf<Pair<Attachment, String>?>(null) }
    var showAttachmentOptions by remember { mutableStateOf(false) }

    // Picked files wait here (one queue per chat) and are sent with the Send button or Enter.
    val pendingFiles = remember(uiState.chat?.id) { androidx.compose.runtime.mutableStateListOf<Uri>() }
    fun queueFiles(uris: List<Uri>) {
        uris.forEach { if (it !in pendingFiles) pendingFiles.add(it) }
    }
    val sendAll: () -> Unit = {
        if (pendingFiles.isNotEmpty()) {
            onUploadFiles(pendingFiles.toList())
            pendingFiles.clear()
        }
        if (uiState.inputText.isNotBlank()) onSendMessage()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 30)
    ) { uris: List<Uri> -> queueFiles(uris) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> -> queueFiles(uris) }

    val displayedMessages = remember(uiState.messages, inChatSearchQuery, isSearching) {
        if (isSearching && inChatSearchQuery.isNotBlank()) {
            uiState.messages.filter { it.content.contains(inChatSearchQuery, ignoreCase = true) }
        } else {
            uiState.messages
        }
    }

    // Requirement 1: Open a chat at the newest message, and auto-scroll on new message.
    // The list is drawn bottom-up (reverseLayout), so it opens at the newest message without a jump.
    val newestFirst = remember(displayedMessages) { displayedMessages.asReversed() }
    val messagesById = remember(uiState.messages) { uiState.messages.associateBy { it.id } }
    var seenNewestId by remember(uiState.chat?.id) { mutableStateOf<String?>(null) }
    val lastMessageId = displayedMessages.lastOrNull()?.id

    LaunchedEffect(lastMessageId, uiState.chat?.id) {
        if (lastMessageId == null) return@LaunchedEffect
        if (seenNewestId != null && seenNewestId != lastMessageId) listState.animateScrollToItem(0)
        seenNewestId = lastMessageId
    }

    // Full Screen Image Dialog (Requirement 1 & 3)
    if (fullScreenAttachment != null) {
        val (att, imageUrl) = fullScreenAttachment!!
        Dialog(
            onDismissRequest = { fullScreenAttachment = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = att.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                // Top bar in full screen dialog with Save to Downloads and Share
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    IconButton(
                        onClick = { fullScreenAttachment = null },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close full-screen image",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = att.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    IconButton(
                        onClick = { onSaveToDownloads(att) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Save to Downloads",
                            tint = GamEmpiresGold
                        )
                    }

                    IconButton(
                        onClick = { onShareAttachment(att) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share image",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    com.gamempires.messenger.ui.components.GamingBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // Offline Banner
            if (uiState.isOffline) {
                Surface(
                    color = Color(0xFFD97706),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Offline indicator",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No connection – retrying",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Chat Top Bar
            TopAppBar(
                title = {
                    if (isSearching) {
                        OutlinedTextField(
                            value = inChatSearchQuery,
                            onValueChange = { inChatSearchQuery = it },
                            placeholder = {
                                Text(
                                    text = "Search in chat...",
                                    color = GamEmpiresTextMuted,
                                    fontSize = 14.sp
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = GamEmpiresBg,
                                unfocusedContainerColor = GamEmpiresBg,
                                focusedBorderColor = GamEmpiresGold,
                                unfocusedBorderColor = GamEmpiresBorder,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            trailingIcon = {
                                if (inChatSearchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { inChatSearchQuery = "" },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = GamEmpiresTextSecondary
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.chat?.type == ChatType.DIRECT && uiState.otherUser != null) {
                                UserAvatar(
                                    imageUrl = uiState.otherUser.avatarUrl,
                                    name = uiState.otherUser.name,
                                    presenceStatus = uiState.otherUserPresence ?: PresenceStatus.OFFLINE,
                                    size = 40.dp,
                                    showPresence = true
                                )
                            } else {
                                // Group emblem from the server (gaming look 1.0.11)
                                UserAvatar(
                                    imageUrl = uiState.chat?.avatarUrl,
                                    name = uiState.chat?.title ?: "GE",
                                    chatType = uiState.chat?.type ?: ChatType.GROUP,
                                    size = 40.dp,
                                    showPresence = false
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = uiState.chat?.title ?: "Chat",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val statusSubtitle = when {
                                    uiState.isOtherUserTyping -> "typing..."
                                    uiState.chat?.type == ChatType.DIRECT -> {
                                        when (uiState.otherUserPresence) {
                                            PresenceStatus.ONLINE -> "Available"
                                            PresenceStatus.AWAY -> "Away"
                                            PresenceStatus.BUSY -> "Focus time"
                                            PresenceStatus.OFFLINE, null -> "Offline"
                                        }
                                    }
                                    else -> "${uiState.chat?.participantIds?.size ?: 0} members"
                                }
                                Text(
                                    text = statusSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (uiState.isOtherUserTyping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to conversation list",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (isSearching) {
                        IconButton(
                            onClick = {
                                isSearching = false
                                inChatSearchQuery = ""
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close in-chat search",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onStartAudioCall,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Start audio call",
                                tint = Color(0xFF10B981)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Outline divider line under the top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )

            // 1. Pinned Announcement Banner (per approved features mockup)
            var pinDismissed by rememberSaveable(uiState.chat?.id) { mutableStateOf(false) }
            val showPinnedBanner = (uiState.chat?.isPinned == true) && !pinDismissed
            if (showPinnedBanner) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pinned message",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "PINNED CONVERSATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = uiState.chat?.title ?: "Pinned conversation",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = { pinDismissed = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss pinned banner",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline)
                )
            }

            // Messages Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GamEmpiresGold)
                    }
                } else if (uiState.historyError != null && displayedMessages.isEmpty()) {
                    // Requirement 2: Show "Could not load messages - tap to retry"
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(
                                    onClick = onRetryLoadHistory,
                                    onClickLabel = "Tap to retry loading messages"
                                )
                                .padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry loading messages",
                                tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Could not load messages - tap to retry",
                                color = GamEmpiresTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            if (!uiState.historyError.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = uiState.historyError,
                                    color = GamEmpiresTextMuted,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else if (displayedMessages.isEmpty()) {
                    // While a chat's history is still arriving, show a spinner instead of flashing
                    // "No messages yet"; the hint appears only if the chat really stays empty.
                    var showEmptyHint by remember(uiState.chat?.id) { mutableStateOf(false) }
                    LaunchedEffect(uiState.chat?.id) {
                        kotlinx.coroutines.delay(1500)
                        showEmptyHint = true
                    }
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (showEmptyHint || isSearching) {
                            Text(
                                text = if (isSearching && inChatSearchQuery.isNotBlank()) "No messages match your search" else "No messages yet. Send a greeting!",
                                color = GamEmpiresTextMuted,
                                fontSize = 14.sp
                            )
                        } else {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = newestFirst,
                            key = { it.id },
                            contentType = { if (it.isSystemMessage) 0 else 1 }
                        ) { message ->
                            // Requirement 4: System messages show as a small centered grey line
                            if (message.isSystemMessage) {
                                SystemMessageLine(content = message.content)
                            } else {
                                val isMe = message.senderId == uiState.currentUser?.id
                                val quotedMessage = if (message.replyToMessageId != null) {
                                    messagesById[message.replyToMessageId]
                                } else null

                                MessageBubble(
                                    message = message,
                                    quotedMessage = quotedMessage,
                                    isMe = isMe,
                                    downloadProgress = uiState.downloadProgress,
                                    downloadedAttachmentIds = uiState.downloadedAttachmentIds,
                                    onLongClick = { selectedMessageForMenu = message },
                                    onAddReaction = { emoji -> onAddReaction(message.id, emoji) },
                                    onImageClick = { att, url -> fullScreenAttachment = att to url },
                                    onDownloadClick = onDownloadAttachment,
                                    onSaveToDownloads = onSaveToDownloads,
                                    onReply = { onSetReplyingTo(message) }
                                )
                            }
                        }
                    }
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GamEmpiresBorder)
            )

            // Bottom Input Bar with Reply Quote Banner
            androidx.compose.runtime.key(uiState.chat?.id) {
            ChatInputBar(
                text = uiState.inputText,
                replyingToMessage = uiState.replyingToMessage,
                uploadProgress = uiState.uploadProgress,
                pendingFiles = pendingFiles,
                onRemovePendingFile = { pendingFiles.remove(it) },
                onCancelReply = { onSetReplyingTo(null) },
                onTextChange = onInputTextChange,
                onAttachClick = { showAttachmentOptions = true },
                onSendMessage = sendAll
            )
            }
        }
    }

    // Attachment Chooser Bottom Sheet (Photos/Videos or Documents/Files)
    if (showAttachmentOptions) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentOptions = false },
            containerColor = GamEmpiresSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Share Attachment",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showAttachmentOptions = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Photos & Videos",
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Photos & Videos",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pick one or more images or videos",
                            fontSize = 12.sp,
                            color = GamEmpiresTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showAttachmentOptions = false
                            filePickerLauncher.launch("*/*")
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Document or File",
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Document / File",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Share PDF, APK, documents, zip, or any file",
                            fontSize = 12.sp,
                            color = GamEmpiresTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Long-Press Bottom Sheet Context Menu
    if (selectedMessageForMenu != null) {
        val targetMsg = selectedMessageForMenu!!
        val isOwnMessage = targetMsg.senderId == uiState.currentUser?.id

        ModalBottomSheet(
            onDismissRequest = { selectedMessageForMenu = null },
            containerColor = GamEmpiresSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "React with emoji",
                    style = MaterialTheme.typography.labelMedium,
                    color = GamEmpiresTextSecondary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick emoji reaction bar
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val quickEmojis = listOf("👍", "❤️", "🔥", "😂", "🎉", "👏")
                    quickEmojis.forEach { emoji ->
                        Surface(
                            shape = CircleShape,
                            color = GamEmpiresCard,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable {
                                    onAddReaction(targetMsg.id, emoji)
                                    selectedMessageForMenu = null
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action: Reply
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable {
                            onSetReplyingTo(targetMsg)
                            selectedMessageForMenu = null
                        }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = "Reply",
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Reply",
                        color = GamEmpiresTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Action: Forward
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable {
                            messageToForward = targetMsg
                            selectedMessageForMenu = null
                        }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Forward,
                        contentDescription = "Forward",
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Forward",
                        color = GamEmpiresTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Action: Save to Downloads (if message has attachments)
                if (targetMsg.attachments.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable {
                                targetMsg.attachments.forEach { att ->
                                    onSaveToDownloads(att)
                                }
                                selectedMessageForMenu = null
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Save to Downloads",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Save to Downloads",
                            color = GamEmpiresTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Action: Share via...
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable {
                            onShareMessage(targetMsg)
                            selectedMessageForMenu = null
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share via...",
                        tint = GamEmpiresTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Share via...",
                        color = GamEmpiresTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Action: Copy Text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Message Text", targetMsg.content)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
                            selectedMessageForMenu = null
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy text",
                        tint = GamEmpiresTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Copy text",
                        color = GamEmpiresTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Action: Edit (own messages only)
                if (isOwnMessage) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable {
                                messageToEdit = targetMsg
                                selectedMessageForMenu = null
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit message",
                            tint = GamEmpiresTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Edit message",
                            color = GamEmpiresTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Action: Delete (own messages only)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable {
                                messageToDelete = targetMsg
                                selectedMessageForMenu = null
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete message",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Delete message",
                            color = Color(0xFFEF4444),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Forward To... Bottom Sheet
    if (messageToForward != null) {
        val targetMsg = messageToForward!!
        val filteredChats = remember(allChats, forwardSearchQuery) {
            if (forwardSearchQuery.isBlank()) {
                allChats
            } else {
                allChats.filter { it.title.contains(forwardSearchQuery, ignoreCase = true) }
            }
        }

        ModalBottomSheet(
            onDismissRequest = {
                messageToForward = null
                forwardSearchQuery = ""
            },
            containerColor = GamEmpiresSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Forward To...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = forwardSearchQuery,
                    onValueChange = { forwardSearchQuery = it },
                    placeholder = {
                        Text("Search chats...", color = GamEmpiresTextMuted, fontSize = 14.sp)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = GamEmpiresBg,
                        unfocusedContainerColor = GamEmpiresBg,
                        focusedBorderColor = GamEmpiresGold,
                        unfocusedBorderColor = GamEmpiresBorder,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                if (filteredChats.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No chats found",
                            color = GamEmpiresTextMuted,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredChats, key = { it.id }) { chat ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onForwardMessage(chat, targetMsg)
                                        messageToForward = null
                                        forwardSearchQuery = ""
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = GamEmpiresGoldDark,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = chat.title.take(2).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = chat.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GamEmpiresTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val subtitle = if (chat.type == ChatType.DIRECT) "Direct chat" else "${chat.participantIds.size} members"
                                    Text(
                                        text = subtitle,
                                        fontSize = 12.sp,
                                        color = GamEmpiresTextMuted
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Edit Message Dialog
    if (messageToEdit != null) {
        val target = messageToEdit!!
        var editedContent by rememberSaveable { mutableStateOf(target.content) }

        AlertDialog(
            onDismissRequest = { messageToEdit = null },
            title = {
                Text(text = "Edit Message", fontWeight = FontWeight.Bold)
            },
            text = {
                OutlinedTextField(
                    value = editedContent,
                    onValueChange = { editedContent = it },
                    singleLine = false,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = GamEmpiresBg,
                        unfocusedContainerColor = GamEmpiresBg,
                        focusedBorderColor = GamEmpiresGold,
                        unfocusedBorderColor = GamEmpiresBorder,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = editedContent.trim()
                        if (trimmed.isNotBlank()) {
                            onEditMessage(target.id, trimmed)
                        }
                        messageToEdit = null
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = GamEmpiresGold)
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToEdit = null }) {
                    Text("Cancel", color = GamEmpiresTextMuted)
                }
            }
        )
    }

    // Delete Message Confirmation Dialog
    if (messageToDelete != null) {
        val target = messageToDelete!!
        AlertDialog(
            onDismissRequest = { messageToDelete = null },
            title = {
                Text(text = "Delete Message?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(text = "Are you sure you want to delete this message? This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteMessage(target.id)
                        messageToDelete = null
                    }
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToDelete = null }) {
                    Text("Cancel", color = GamEmpiresTextMuted)
                }
            }
        )
    }
}

/**
 * Requirement 4: Other system messages show as a small centered grey line, not a bubble.
 */
@Composable
private fun SystemMessageLine(
    content: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = content,
            fontSize = 12.sp,
            color = GamEmpiresTextMuted,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    quotedMessage: Message?,
    isMe: Boolean,
    downloadProgress: Map<String, Float>,
    downloadedAttachmentIds: Set<String>,
    onLongClick: () -> Unit,
    onAddReaction: (String) -> Unit,
    onImageClick: (Attachment, String) -> Unit,
    onDownloadClick: (Attachment) -> Unit,
    onSaveToDownloads: (Attachment) -> Unit = onDownloadClick,
    onReply: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Requirement 4: A deleted message shows as one muted italic line
    if (message.isDeleted) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 12.dp),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            Text(
                text = "This message was deleted",
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                color = GamEmpiresTextMuted
            )
        }
        return
    }

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    // Message bubbles: Outgoing (isMe == true) uses solid primary (#2563EB) with white text;
    // incoming (isMe == false) uses surface with a 1.dp outline border (remove 3.dp left amber bar and dark gradients).
    val geDark = com.gamempires.messenger.ui.components.geIsDark()
    val bubbleShape = com.gamempires.messenger.ui.components.geBubbleShape(mine = isMe)
    val bubbleColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val bubbleBorder = if (isMe) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    val bodyTextColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
    val metaColor = if (isMe) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
    val tickColor = if (isMe) Color.White.copy(alpha = 0.9f) else metaColor

    var swipeOffset by remember { mutableStateOf(0f) }
    val animatedSwipeOffset by animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "swipeToReply"
    )

    Column(
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            if (animatedSwipeOffset > 15f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = "Reply",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Surface(
                shape = bubbleShape,
                color = bubbleColor,
                border = bubbleBorder,
                shadowElevation = if (isMe) 0.dp else 1.dp,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .offset { androidx.compose.ui.unit.IntOffset(animatedSwipeOffset.toInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (swipeOffset > 80f) {
                                    onReply()
                                }
                                swipeOffset = 0f
                            },
                            onDragCancel = { swipeOffset = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                swipeOffset = (swipeOffset + dragAmount).coerceIn(0f, 140f)
                            }
                        )
                    }
                    .padding(
                        start = if (isMe) 48.dp else 0.dp,
                        end = if (isMe) 0.dp else 48.dp
                    )
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onLongClick
                    )
            ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Quoted reply banner if this message replied to another message
                if (quotedMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isMe) Color(0x22FFFFFF) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(30.dp)
                                    .background(if (isMe) Color.White else MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = quotedMessage.content,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = bodyTextColor.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // Requirement 3: Attachments (images inline with full screen tap, non-images as cards with download)
                if (message.attachments.isNotEmpty()) {
                    message.attachments.forEach { att ->
                        if (att.type == AttachmentType.IMAGE) {
                            val imgUrl = att.thumbnailUrl.takeIf { !it.isNullOrBlank() } ?: att.url
                            AsyncImage(
                                model = imgUrl,
                                contentDescription = att.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onImageClick(att, imgUrl) }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        } else {
                            val progress = downloadProgress[att.id]
                            FileAttachmentCard(
                                attachment = att,
                                downloadProgress = progress,
                                downloaded = att.id in downloadedAttachmentIds,
                                onClick = { onDownloadClick(att) },
                                onSaveClick = { onSaveToDownloads(att) }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }

                // Message Text: don't show duplicate file name or raw URL if attachment matches content
                val hasMatchingAttachment = message.attachments.any { att ->
                    val c = message.content.trim()
                    c.equals(att.name.trim(), ignoreCase = true) ||
                    c.equals(att.url.trim(), ignoreCase = true) ||
                    c == "{file}" ||
                    c.endsWith(att.name.trim(), ignoreCase = true)
                }
                if (!hasMatchingAttachment && message.content.isNotBlank()) {
                    MarkdownText(
                        markdown = message.content,
                        color = bodyTextColor,
                        linkColor = if (isMe) Color.White else MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Time, Edited Status & Delivery Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    if (message.isEdited) {
                        Text(
                            text = "edited",
                            fontSize = 10.sp,
                            color = metaColor,
                            fontStyle = FontStyle.Italic
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = metaColor
                    )

                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        when (message.status) {
                            MessageStatus.SENDING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 1.5.dp,
                                    color = tickColor
                                )
                            }
                            MessageStatus.SENT -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Sent",
                                    tint = tickColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            MessageStatus.DELIVERED -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Delivered",
                                    tint = tickColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            MessageStatus.READ -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Read",
                                    tint = if (isMe) Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            MessageStatus.FAILED -> {
                                Text(
                                    text = "!",
                                    color = Color(0xFFEF4444),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        // Reactions Display below the message bubble
        if (message.reactions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(3.dp))
            val reactionCounts = message.reactions.values.groupingBy { it }.eachCount()

            Row(
                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                reactionCounts.forEach { (emoji, count) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GamEmpiresCard,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable { onAddReaction(emoji) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = emoji, fontSize = 12.sp)
                            if (count > 1) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = count.toString(),
                                    fontSize = 11.sp,
                                    color = GamEmpiresTextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Requirement 3: File Card with icon by type, name, size, download progress, and tap-to-open.
 */
@Composable
private fun FileAttachmentCard(
    attachment: Attachment,
    downloadProgress: Float?,
    downloaded: Boolean = false,
    onClick: () -> Unit,
    onSaveClick: () -> Unit = onClick,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector = when (attachment.type) {
        AttachmentType.AUDIO -> Icons.Default.AudioFile
        AttachmentType.VIDEO -> Icons.Default.VideoFile
        AttachmentType.DOCUMENT -> Icons.Default.Description
        AttachmentType.FILE -> {
            if (attachment.name.endsWith(".apk", ignoreCase = true) || attachment.mimeType.contains("android.package")) {
                Icons.Default.Android
            } else {
                Icons.Default.InsertDriveFile
            }
        }
        AttachmentType.IMAGE -> Icons.Default.InsertDriveFile
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                onClick = onClick,
                enabled = downloadProgress == null,
                onClickLabel = "Open ${attachment.name}"
            )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GamEmpiresGold.copy(alpha = 0.2f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = attachment.type.name,
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = attachment.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatFileSize(attachment.sizeBytes),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (downloadProgress != null) {
                    if (downloadProgress < 0f || downloadProgress >= 1f) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        CircularProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.size(24.dp),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp
                        )
                    }
                } else {
                    IconButton(
                        onClick = onSaveClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (downloaded) Icons.Default.Check else Icons.Default.Download,
                            contentDescription = if (downloaded) "Downloaded" else "Save to Downloads",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (downloaded && downloadProgress == null) {
                Text("Downloaded · tap to open", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
            }

            if (downloadProgress != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when {
                        downloadProgress < 0f -> "Downloading…"
                        downloadProgress >= 1f -> "Finishing…"
                        else -> "${(downloadProgress * 100).toInt()}%"
                    },
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp
                )
                if (downloadProgress in 0f..1f) {
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
        else -> "$bytes B"
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    replyingToMessage: Message?,
    uploadProgress: Map<String, Float> = emptyMap(),
    pendingFiles: List<Uri> = emptyList(),
    onRemovePendingFile: (Uri) -> Unit = {},
    onCancelReply: () -> Unit,
    onTextChange: (String) -> Unit,
    onAttachClick: () -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEmojiPicker by remember { mutableStateOf(false) }
    val canSend = text.isNotBlank() || pendingFiles.isNotEmpty()
    // The message box only takes focus (and opens the keyboard) after the user taps it,
    // so opening a chat never pops up the keyboard by itself.
    var inputArmed by remember { mutableStateOf(false) }
    val inputFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val emojiZonePx = with(androidx.compose.ui.platform.LocalDensity.current) { 52.dp.toPx() }
    LaunchedEffect(inputArmed) {
        if (inputArmed) {
            runCatching { inputFocus.requestFocus() }
            keyboard?.show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(GamEmpiresSurface)
    ) {
        if (uploadProgress.isNotEmpty()) {
            uploadProgress.forEach { (fileName, progress) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GamEmpiresBg)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Uploading",
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Uploading $fileName...",
                        fontSize = 12.sp,
                        color = GamEmpiresTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (progress > 0f) {
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GamEmpiresGold
                        )
                    } else {
                        CircularProgressIndicator(
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Files waiting to be sent (removable); they go out with Send or Enter.
        if (pendingFiles.isNotEmpty()) {
            PendingFilesStrip(files = pendingFiles, onRemove = onRemovePendingFile)
        }

        // Quoted reply bar preview above text field if replying
        if (replyingToMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val senderLabel = replyingToMessage.senderName?.ifBlank { "message" } ?: "message"
                        Text(
                            text = "Replying to $senderLabel",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = replyingToMessage.content.ifBlank { "Shared attachment" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onCancelReply,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel reply",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        var isRecordingVoice by rememberSaveable { mutableStateOf(false) }
        var recordingSeconds by rememberSaveable { mutableStateOf(0) }
        LaunchedEffect(isRecordingVoice) {
            if (isRecordingVoice) {
                recordingSeconds = 0
                while (isRecordingVoice) {
                    kotlinx.coroutines.delay(1000)
                    recordingSeconds++
                }
            }
        }

        if (isRecordingVoice) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                    Spacer(Modifier.width(10.dp))
                    val mins = recordingSeconds / 60
                    val secs = recordingSeconds % 60
                    Text(
                        text = "$mins:${if (secs < 10) "0" else ""}$secs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Recording voice note...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isRecordingVoice = false },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Cancel recording",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(
                                onClick = { isRecordingVoice = false },
                                onClickLabel = "Send voice note"
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send voice note",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Input Field and Send Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            IconButton(
                onClick = onAttachClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attach file",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = {
                    Text(
                        text = "Write a message...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                // Enter / the keyboard's send key sends the message and any queued files.
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Send
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSend = { if (canSend) onSendMessage() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                trailingIcon = {
                    IconButton(
                        onClick = { showEmojiPicker = !showEmojiPicker },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (showEmojiPicker) Icons.Default.Keyboard else Icons.Default.SentimentSatisfiedAlt,
                            contentDescription = if (showEmojiPicker) "Show keyboard" else "Insert emoji",
                            tint = if (showEmojiPicker) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            // Taps on the emoji button (right end) don't arm the keyboard.
                            if (down.position.x < size.width - emojiZonePx) inputArmed = true
                        }
                    }
                    .focusProperties { canFocus = inputArmed }
                    .focusRequester(inputFocus)
                    .onPreviewKeyEvent { event ->
                        // Hardware keyboard: Enter sends, Shift+Enter adds a new line.
                        if (event.key == androidx.compose.ui.input.key.Key.Enter && !event.isShiftPressed) {
                            if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown && canSend) onSendMessage()
                            true
                        } else false
                    }
                    .onFocusChanged {
                        if (it.isFocused && showEmojiPicker) {
                            showEmojiPicker = false
                        }
                    }
            )

            Spacer(modifier = Modifier.width(6.dp))

            if (canSend) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(
                            onClick = onSendMessage,
                            onClickLabel = "Send message"
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(
                            onClick = { isRecordingVoice = true },
                            onClickLabel = "Record voice note"
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Record voice note",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }

        // Requirement 3: Native Compose Emoji Picker Grid
        AnimatedVisibility(visible = showEmojiPicker) {
            val emojis = remember {
                listOf(
                    "😀", "😂", "🤣", "😊", "😍", "🔥",
                    "👍", "👏", "🎉", "🎮", "🏆", "💯",
                    "❤️", "🙏", "😅", "🤔", "👀", "🚀",
                    "✅", "❌", "😎", "🤝", "💬", "⚡"
                )
            }
            Surface(
                color = GamEmpiresBg,
                border = BorderStroke(1.dp, GamEmpiresBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(emojis) { emoji ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onTextChange(text + emoji)
                                }
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 22.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Composable
private fun ChatDetailScreenPreview() {
    MyApplicationTheme {
        ChatDetailScreen(
            uiState = ChatDetailUiState(
                chat = com.gamempires.messenger.model.Chat(
                    id = "chat_1",
                    type = ChatType.DIRECT,
                    title = "Sagher HR",
                    avatarUrl = null,
                    participantIds = listOf("user_1"),
                    lastMessage = null,
                    unreadCount = 0,
                    isPinned = false,
                    isMuted = false,
                    updatedAt = System.currentTimeMillis()
                ),
                messages = listOf(
                    Message(
                        id = "1",
                        chatId = "chat_1",
                        senderId = "user_1",
                        content = "Hello! Did you review the **Q4 metrics**?",
                        timestamp = System.currentTimeMillis() - 60000L
                    ),
                    Message(
                        id = "2",
                        chatId = "chat_1",
                        senderId = "me",
                        content = "Yes, reviewing it now. Looks great! 👍",
                        timestamp = System.currentTimeMillis()
                    )
                )
            ),
            onBackClick = {},
            onInputTextChange = {},
            onSendMessage = {},
            onAddReaction = { _, _ -> }
        )
    }
}

/** Horizontal list of picked files waiting to be sent, each with a remove button. */
@Composable
private fun PendingFilesStrip(files: List<Uri>, onRemove: (Uri) -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GamEmpiresBg)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = if (files.size == 1) "1 file ready to send" else "${files.size} files ready to send",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = GamEmpiresGold
        )
        Spacer(modifier = Modifier.height(4.dp))
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
        ) {
            items(files.size, key = { files[it].toString() }) { index ->
                val uri = files[index]
                val name = remember(uri) {
                    runCatching {
                        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
                    }.getOrNull() ?: uri.lastPathSegment ?: "file"
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = GamEmpiresCard,
                    border = BorderStroke(1.dp, GamEmpiresBorder)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 10.dp, end = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = GamEmpiresGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = name,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 150.dp)
                        )
                        IconButton(onClick = { onRemove(uri) }, modifier = Modifier.size(30.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove $name",
                                tint = GamEmpiresTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
