package com.gamempires.messenger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Corporate Workspace UI design system shared pieces.
 */

object GeColors {
    val Amber = Color(0xFFD97706)
    val AmberLight = Color(0xFFF59E0B)
    val AmberInk = Color(0xFF78350F)
    val Pink = Color(0xFFD97706)
    val Orange = Color(0xFFD97706)
    val Gold = Color(0xFFF59E0B)
    val Cyan = Color(0xFFD97706)
    val Purple = Color(0xFFD97706)
    val AmberGradient = Brush.horizontalGradient(listOf(Amber, AmberLight))
    val SlashGradient = Brush.horizontalGradient(listOf(Amber, AmberLight))
}

/** True when the dark colour scheme is active. */
@Composable
fun geIsDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** Accent color used for highlighted text and icons: Corporate Blue. */
@Composable
fun geAccent(): Color = MaterialTheme.colorScheme.primary

/** Card / field fill: surfaceVariant. */
@Composable
fun geFieldColor(): Color = MaterialTheme.colorScheme.surfaceVariant

/** Outline / border color: outline. */
@Composable
fun geBorderColor(): Color = MaterialTheme.colorScheme.outline

/** Slant shape replaced with CircleShape. */
fun geSlantShape(slant: Dp = 6.dp): Shape = CircleShape

/** Chamfer shape replaced with RoundedCornerShape(16.dp). */
fun geChamferShape(cut: Dp = 10.dp): Shape = RoundedCornerShape(16.dp)

/** Message bubble: RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (mine) 18.dp else 4.dp, bottomEnd = if (mine) 4.dp else 18.dp). */
fun geBubbleShape(mine: Boolean, cut: Dp = 10.dp): Shape = RoundedCornerShape(
    topStart = 18.dp,
    topEnd = 18.dp,
    bottomStart = if (mine) 18.dp else 4.dp,
    bottomEnd = if (mine) 4.dp else 18.dp
)

/** Title text: plain FontWeight.SemiBold without "// ". */
fun geSlashTitle(title: String): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
        append(title)
    }
}

/**
 * Screen background: clean solid MaterialTheme.colorScheme.background.
 */
@Composable
fun GamingBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()
    }
}

/** Legacy corner brackets: cleanly omitted in Corporate Workspace design. */
fun Modifier.geCornerBrackets(color: Color = Color.Transparent, inset: Dp = 8.dp, length: Dp = 12.dp): Modifier = this

/** Clean corporate icon tile container (replaces hexagon icon frame). */
@Composable
fun GeHexIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}
