package com.gamempires.messenger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamempires.messenger.ui.theme.CorporateBlue

/**
 * Corporate GamEmpires Logo:
 * Exact match to corporate-chat-mockup.html:
 * 36.dp rounded container (12.dp radius) in Corporate Blue (#2563EB) with bold white "GE",
 * followed by bold "GamEmpires" title and a soft pill-shaped "Chat" badge.
 */
@Composable
fun GamEmpiresLogo(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    showChatTag: Boolean = true,
    chatTagSize: TextUnit = 12.sp
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // 36.dp rounded-2xl Corporate Blue badge with bold white "GE"
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CorporateBlue),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "GE",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // "GamEmpires" title
        Text(
            text = "GamEmpires",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )

        // Pill-shaped "Chat" badge
        if (showChatTag) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chat",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = chatTagSize,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}

/**
 * Renders a clean solid MaterialTheme.colorScheme.background without repeating canvas watermarks.
 */
@Composable
fun GamEmpiresWatermarkBackground(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        content()
    }
}
