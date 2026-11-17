package com.gamempires.messenger.ui.security

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamempires.messenger.R
import com.gamempires.messenger.ui.components.geIsDark
import com.gamempires.messenger.ui.theme.CorporateBlue

/** Covers the app until the fingerprint (or screen lock) is confirmed. */
@Composable
fun LockScreen() {
    val context = LocalContext.current
    val isDark = geIsDark()
    LaunchedEffect(Unit) { AppLock.unlock(context) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Swallow touches so nothing underneath can be used while locked.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.crown_logo),
            contentDescription = "GamEmpires Crown",
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "G",
                color = Color(0xFFF59E0B),
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "E",
                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "GamEmpires Messenger is locked",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { AppLock.unlock(context) },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CorporateBlue, contentColor = Color.White)
        ) {
            Icon(Icons.Default.Fingerprint, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Unlock", fontWeight = FontWeight.SemiBold)
        }
    }
}
