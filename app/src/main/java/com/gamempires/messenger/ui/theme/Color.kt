package com.gamempires.messenger.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme

// Corporate Brand Palette (#2563EB)
val CorporateBlue = Color(0xFF2563EB)
val CorporateBlueDark = Color(0xFF1D4ED8)
val CorporateBlueLight = Color(0xFF3B82F6)
val CorporateBlue50 = Color(0xFFEFF6FF)
val CorporateBlue100 = Color(0xFFDBEAFE)

// Backward-compatibility aliases for GamEmpires theme references
val GamEmpiresGold = CorporateBlue
val GamEmpiresGoldLight = CorporateBlueLight
val GamEmpiresGoldDark = CorporateBlueDark
val GamEmpiresAmber = Color(0xFFF59E0B)
val GamEmpiresYellow = Color(0xFFFBBF24)

// Status Indicators (from design tokens)
val StatusOnline = Color(0xFF10B981)
val StatusAway = Color(0xFFF59E0B)
val StatusBusy = Color(0xFF6366F1) // Focus time (#6366F1)
val StatusOffline = Color(0xFF94A3B8)

// Theme Surfaces
val GamEmpiresBg: Color
    @Composable get() = MaterialTheme.colorScheme.background
val GamEmpiresSurface: Color
    @Composable get() = MaterialTheme.colorScheme.surface
val GamEmpiresSurfaceVariant: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val GamEmpiresCard: Color
    @Composable get() = MaterialTheme.colorScheme.surface
val GamEmpiresBorder: Color
    @Composable get() = MaterialTheme.colorScheme.outline
val GamEmpiresWatermark: Color
    @Composable get() = MaterialTheme.colorScheme.background

// Card & Accent Colors
val GamEmpiresPillLight = Color(0xFFEFF6FF)
val GamEmpiresPillText = Color(0xFF1D4ED8)
val GamEmpiresActionGold = CorporateBlue
val GamEmpiresActionCream = Color(0xFFEFF6FF)
val GamEmpiresCardGoldBg = CorporateBlue
val GamEmpiresCardGoldDark = CorporateBlueDark

// Text Colors
val GamEmpiresTextPrimary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface
val GamEmpiresTextSecondary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val GamEmpiresTextMuted: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

// Status Indicators
val GamEmpiresOnlineGreen = StatusOnline
val GamEmpiresAwayYellow = StatusAway
val GamEmpiresBusyRed = StatusBusy
