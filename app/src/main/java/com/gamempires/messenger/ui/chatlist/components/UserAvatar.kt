package com.gamempires.messenger.ui.chatlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.gamempires.messenger.model.ChatType
import com.gamempires.messenger.model.PresenceStatus

@Composable
fun PresenceBadge(
    status: PresenceStatus,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    borderWidth: Dp = 2.dp
) {
    val color = when (status) {
        PresenceStatus.ONLINE -> Color(0xFF10B981) // Online / Available
        PresenceStatus.AWAY -> Color(0xFFF59E0B)   // Away
        PresenceStatus.BUSY -> Color(0xFF6366F1)   // Busy / Focus
        PresenceStatus.OFFLINE -> Color(0xFF94A3B8)// Offline
    }

    Box(
        modifier = modifier
            .size(size)
            .background(color, CircleShape)
            .border(borderWidth, MaterialTheme.colorScheme.surface, CircleShape)
    )
}

@Composable
fun UserAvatar(
    imageUrl: String?,
    name: String,
    presenceStatus: PresenceStatus? = null,
    chatType: ChatType = ChatType.DIRECT,
    size: Dp = 48.dp,
    showPresence: Boolean = true,
    modifier: Modifier = Modifier
) {
    val badgeSize = (size.value * 0.28f).dp.coerceAtLeast(10.dp)
    val badgeOffset = (size.value * 0.02f).dp

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(AvatarTileShape),
                loading = {
                    AvatarFallback(name = name, chatType = chatType, size = size)
                },
                error = {
                    AvatarFallback(name = name, chatType = chatType, size = size)
                }
            )
        } else {
            AvatarFallback(name = name, chatType = chatType, size = size)
        }

        if (showPresence && presenceStatus != null && chatType == ChatType.DIRECT) {
            PresenceBadge(
                status = presenceStatus,
                size = badgeSize,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = badgeOffset, y = badgeOffset)
            )
        }
    }
}

@Composable
private fun AvatarFallback(
    name: String,
    chatType: ChatType,
    size: Dp
) {
    val initials = name.trim().split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    when {
        name.contains("Note to self", ignoreCase = true) -> {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(AvatarTileShape)
                    .background(Color(0xFFF59E0B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = name,
                    tint = Color(0xFF1E1102),
                    modifier = Modifier.size(size * 0.52f)
                )
            }
        }
        name.contains("Let's get started", ignoreCase = true) -> {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(AvatarTileShape)
                    .background(Color(0xFF3E382E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = name,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(size * 0.52f)
                )
            }
        }
        name.contains("GamEmpires updates", ignoreCase = true) || name.contains("Talk updates", ignoreCase = true) -> {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(AvatarTileShape)
                    .background(Color(0xFFFFA000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = name,
                    tint = Color(0xFF1E1102),
                    modifier = Modifier.size(size * 0.52f)
                )
            }
        }
        chatType == ChatType.GROUP -> {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(AvatarTileShape)
                    .background(Color(0xFF3D372E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = name,
                    tint = Color(0xFFE2E8F0),
                    modifier = Modifier.size(size * 0.52f)
                )
            }
        }
        else -> {
            val hash = name.hashCode()
            val dark = com.gamempires.messenger.ui.components.geIsDark()
            val colors = if (dark) listOf(
                Color(0xFF60A5FA), Color(0xFFC084FC), Color(0xFF22D3EE), Color(0xFFFDBA74), Color(0xFF4ADE80), Color(0xFFF87171)
            ) else listOf(
                Color(0xFF1D4ED8), Color(0xFF7E22CE), Color(0xFF0E7490), Color(0xFFC2410C), Color(0xFF15803D), Color(0xFFB91C1C)
            )
            val letterColor = colors[kotlin.math.abs(hash) % colors.size]
            val tile = if (dark) androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF2E2A22), Color(0xFF161514)))
                else androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFFFF1D2), Color(0xFFEBD3A0)))
            val displayLetter = initials.ifEmpty {
                name.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "U"
            }

            Box(
                modifier = Modifier
                    .size(size)
                    .clip(AvatarTileShape)
                    .background(tile)
                    .border(1.5.dp, if (dark) Color(0x24FFFFFF) else Color(0x1A000000), AvatarTileShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayLetter,
                    color = letterColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                    fontSize = (size.value * 0.42f).sp
                )
            }
        }
    }
}

/** Circular avatar tile for corporate workspace look. */
internal val AvatarTileShape = androidx.compose.foundation.shape.CircleShape
