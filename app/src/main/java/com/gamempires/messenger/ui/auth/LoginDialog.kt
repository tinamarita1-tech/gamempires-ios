package com.gamempires.messenger.ui.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gamempires.messenger.data.nextcloud.auth.NextcloudAuthStorage
import com.gamempires.messenger.ui.components.GamEmpiresLogo
import com.gamempires.messenger.ui.theme.GamEmpiresActionCream
import com.gamempires.messenger.ui.theme.GamEmpiresActionGold
import com.gamempires.messenger.ui.theme.GamEmpiresCard
import com.gamempires.messenger.ui.theme.GamEmpiresGold
import com.gamempires.messenger.ui.theme.GamEmpiresOnlineGreen
import com.gamempires.messenger.ui.theme.GamEmpiresSurface
import com.gamempires.messenger.ui.theme.MyApplicationTheme
import com.gamempires.messenger.ui.theme.GamEmpiresTextMuted
import com.gamempires.messenger.ui.theme.GamEmpiresTextPrimary
import com.gamempires.messenger.ui.theme.GamEmpiresTextSecondary

@Composable
fun LoginDialog(
    uiState: LoginUiState,
    onStartLogin: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Automatically launch browser when loginUrl is received
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.AwaitingBrowserAuth) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uiState.loginUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = GamEmpiresSurface),
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                GamEmpiresLogo(
                    fontSize = 22.sp,
                    chatTagSize = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = CircleShape,
                    color = GamEmpiresCard,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "GamEmpires Chat Server",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GamEmpiresTextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = NextcloudAuthStorage.DEFAULT_SERVER_URL,
                    fontSize = 13.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (uiState) {
                    is LoginUiState.Idle -> {
                        Text(
                            text = "Authenticate securely via official GamEmpires login flow. The app never asks for or stores your personal password.",
                            fontSize = 12.sp,
                            color = GamEmpiresTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { onStartLogin(NextcloudAuthStorage.DEFAULT_SERVER_URL) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GamEmpiresActionGold,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Log in to GamEmpires Chat",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    is LoginUiState.Initializing, is LoginUiState.SigningIn -> {
                        CircularProgressIndicator(
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Initializing Login Flow...",
                            fontSize = 13.sp,
                            color = GamEmpiresTextSecondary
                        )
                    }

                    is LoginUiState.AwaitingBrowserAuth -> {
                        CircularProgressIndicator(
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Waiting for browser approval...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GamEmpiresTextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Please approve login in your browser. Once granted, GamEmpires Chat connects automatically.",
                            fontSize = 12.sp,
                            color = GamEmpiresTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uiState.loginUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = GamEmpiresGold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Reopen Browser",
                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    is LoginUiState.Success -> {
                        Surface(
                            shape = CircleShape,
                            color = GamEmpiresOnlineGreen,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Connected successfully!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GamEmpiresOnlineGreen
                        )
                    }

                    is LoginUiState.Error -> {
                        Text(
                            text = uiState.message,
                            fontSize = 12.sp,
                            color = Color(0xFFEF4444),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { onStartLogin(NextcloudAuthStorage.DEFAULT_SERVER_URL) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Retry Login")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = GamEmpiresTextMuted
                    )
                ) {
                    Text("Close", fontSize = 12.sp)
                }
            }
        }
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Composable
private fun LoginDialogPreview() {
    MyApplicationTheme {
        LoginDialog(
            uiState = LoginUiState.Idle,
            onStartLogin = {},
            onDismiss = {}
        )
    }
}

