package com.gamempires.messenger.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamempires.messenger.R
import com.gamempires.messenger.ui.theme.CorporateBlue

/** Corporate loading screen shown for a moment when the app is opened from the launcher. */
@Composable
fun StartupSplash(modifier: Modifier = Modifier) {
    val isDark = geIsDark()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Official Company Crown Logo
        Image(
            painter = painterResource(R.drawable.crown_logo),
            contentDescription = "GamEmpires Crown",
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "G",
                color = Color(0xFFF59E0B),
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "E",
                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Workspace Chat",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif
        )
        Spacer(modifier = Modifier.height(36.dp))
        CircularProgressIndicator(
            color = CorporateBlue,
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(28.dp)
        )
    }
}
