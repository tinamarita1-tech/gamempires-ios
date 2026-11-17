package com.gamempires.messenger.ui.chatlist.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamempires.messenger.model.AttachmentType
import com.gamempires.messenger.model.Chat
import com.gamempires.messenger.model.ChatType
import com.gamempires.messenger.model.MessageStatus
import com.gamempires.messenger.model.PresenceStatus
import com.gamempires.messenger.model.User
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Corporate Workspace Chat List Item:
 * Rendered as a 16.dp rounded card (outer padding: horizontal = 20.dp, vertical = 4.dp;
 * inner padding: horizontal = 14.dp, vertical = 12.dp) with 1.dp outline border, subtle
 * primary tint when isSelected, clean sentence-case title, and circular #2563EB unread badge.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListItem(
    chat: Chat,
    otherUser: User?,
    presenceStatus: PresenceStatus?,
    typingText: String?,
    isSelected: Boolean,
    onChatClick: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onToggleMute: (String) -> Unit,
    onMarkAsRead: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline
        ),
        shadowElevation = if (isSelected) 1.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { onChatClick(chat.id) },
                onLongClick = { showMenu = true }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with Presence
            UserAvatar(
                imageUrl = if (chat.type == ChatType.DIRECT) otherUser?.avatarUrl else chat.avatarUrl,
                name = chat.title,
                presenceStatus = presenceStatus ?: otherUser?.status ?: PresenceStatus.OFFLINE,
                chatType = chat.type,
                size = 46.dp,
                showPresence = chat.type == ChatType.DIRECT
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Main Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Top row: Title + Timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    val timeText = formatMessageTime(chat.lastMessage?.timestamp ?: chat.updatedAt)
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Bottom row: Preview / Typing indicator + Badges/Icons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (!typingText.isNullOrBlank()) {
                            // Typing indicator
                            Text(
                                text = typingText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                fontStyle = FontStyle.Italic,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            val lastMsg = chat.lastMessage
                            if (lastMsg != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Status ticks for sent messages if direct chat
                                    if (lastMsg.senderId == "user_me") {
                                        MessageStatusIcon(status = lastMsg.status)
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }

                                    val previewText = getMessagePreview(lastMsg.content, lastMsg.attachments.map { it.type })
                                    Text(
                                        text = previewText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (chat.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                Text(
                                    text = "No messages yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Trailing icons: Pinned, Muted, Unread badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (chat.isPinned) {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (chat.isMuted) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsOff,
                                contentDescription = "Muted",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (chat.unreadCount > 0) {
                            // Circular unread badge
                            Box(
                                modifier = Modifier
                                    .sizeIn(minWidth = 20.dp, minHeight = 20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dropdown Context Menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(if (chat.isPinned) "Unpin Chat" else "Pin Chat") },
                leadingIcon = {
                    Icon(
                        imageVector = if (chat.isPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                        contentDescription = null
                    )
                },
                onClick = {
                    onTogglePin(chat.id)
                    showMenu = false
                }
            )

            DropdownMenuItem(
                text = { Text(if (chat.isMuted) "Unmute Notifications" else "Mute Notifications") },
                leadingIcon = {
                    Icon(
                        imageVector = if (chat.isMuted) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff,
                        contentDescription = null
                    )
                },
                onClick = {
                    onToggleMute(chat.id)
                    showMenu = false
                }
            )

            if (chat.unreadCount > 0) {
                DropdownMenuItem(
                    text = { Text("Mark as Read") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.MarkEmailRead,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        onMarkAsRead(chat.id)
                        showMenu = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MessageStatusIcon(status: MessageStatus) {
    val icon = when (status) {
        MessageStatus.SENDING, MessageStatus.SENT -> Icons.Default.Check
        MessageStatus.DELIVERED, MessageStatus.READ -> Icons.Default.DoneAll
        MessageStatus.FAILED -> Icons.Default.Check
    }
    val tint = if (status == MessageStatus.READ) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }

    Icon(
        imageVector = icon,
        contentDescription = status.name,
        tint = tint,
        modifier = Modifier.size(16.dp)
    )
}

private fun getMessagePreview(content: String, attachmentTypes: List<AttachmentType>): String {
    if (attachmentTypes.isNotEmpty()) {
        val icon = when (attachmentTypes.first()) {
            AttachmentType.IMAGE -> "📷 Photo"
            AttachmentType.VIDEO -> "🎥 Video"
            AttachmentType.AUDIO -> "🎵 Audio"
            AttachmentType.DOCUMENT -> "📄 Document"
            AttachmentType.FILE -> "📁 File"
        }
        return if (content.isNotBlank()) "$icon: $content" else icon
    }
    return content
}

private fun formatMessageTime(timestamp: Long): String {
    val now = Calendar.getInstance()
    val msgTime = Calendar.getInstance().apply { timeInMillis = timestamp }

    return if (now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR)
    ) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
    } else if (now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) - msgTime.get(Calendar.DAY_OF_YEAR) == 1
    ) {
        "Yesterday"
    } else if (now.timeInMillis - timestamp < 7 * 24 * 60 * 60 * 1000L) {
        SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
    } else {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
