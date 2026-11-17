package com.gamempires.messenger.ui.theme

import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test

class AppearanceTest {
    @Test fun `saved modes survive parsing and unknown settings retain existing dark default`() {
        assertEquals(AppearanceMode.LIGHT, AppearanceMode.fromPreference("LIGHT"))
        assertEquals(AppearanceMode.SYSTEM, AppearanceMode.fromPreference("SYSTEM"))
        assertEquals(AppearanceMode.DARK, AppearanceMode.fromPreference(null))
        assertEquals(AppearanceMode.DARK, AppearanceMode.fromPreference("invalid"))
    }

    @Test fun `both palettes provide readable surface text`() {
        for (colors in listOf(LightAppColors, DarkAppColors)) {
            val bright = maxOf(colors.surface.luminance(), colors.onSurface.luminance())
            val dark = minOf(colors.surface.luminance(), colors.onSurface.luminance())
            assertTrue((bright + 0.05f) / (dark + 0.05f) >= 4.5f)
        }
    }
}
