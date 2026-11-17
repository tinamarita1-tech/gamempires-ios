package com.gamempires.messenger.ui.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamempires.messenger.data.nextcloud.auth.NextcloudAuthStorage
import com.gamempires.messenger.ui.components.GamEmpiresLogo
import com.gamempires.messenger.ui.theme.GamEmpiresActionGold
import com.gamempires.messenger.ui.theme.GamEmpiresBg
import com.gamempires.messenger.ui.theme.GamEmpiresCard
import com.gamempires.messenger.ui.theme.GamEmpiresGold
import com.gamempires.messenger.ui.theme.GamEmpiresOnlineGreen
import com.gamempires.messenger.ui.theme.GamEmpiresSurface
import com.gamempires.messenger.ui.theme.GamEmpiresTextMuted
import com.gamempires.messenger.ui.theme.GamEmpiresTextPrimary
import com.gamempires.messenger.ui.theme.GamEmpiresTextSecondary
import com.gamempires.messenger.ui.theme.MyApplicationTheme

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onStartLogin: (String) -> Unit,
    onPasswordLogin: (String, String) -> Unit = { _, _ -> },
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

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(GamEmpiresBg)
            .padding(24.dp)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = GamEmpiresSurface),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                GamEmpiresLogo(
                    fontSize = 26.sp,
                    chatTagSize = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    shape = CircleShape,
                    color = GamEmpiresCard,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "GamEmpires Chat Server",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = GamEmpiresTextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = NextcloudAuthStorage.DEFAULT_SERVER_URL,
                    fontSize = 14.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(24.dp))

                when (uiState) {
                    is LoginUiState.Idle -> {
                        PasswordLoginForm(
                            onPasswordLogin = onPasswordLogin,
                            onBrowserLogin = { onStartLogin(NextcloudAuthStorage.DEFAULT_SERVER_URL) }
                        )
                    }

                    is LoginUiState.SigningIn -> {
                        CircularProgressIndicator(
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Signing in...",
                            fontSize = 14.sp,
                            color = GamEmpiresTextSecondary
                        )
                    }

                    is LoginUiState.Initializing -> {
                        CircularProgressIndicator(
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Initializing Login Flow...",
                            fontSize = 14.sp,
                            color = GamEmpiresTextSecondary
                        )
                    }

                    is LoginUiState.AwaitingBrowserAuth -> {
                        CircularProgressIndicator(
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Waiting for browser approval...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GamEmpiresTextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Please approve login in your browser. Once granted, GamEmpires Chat connects automatically.",
                            fontSize = 13.sp,
                            color = GamEmpiresTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uiState.loginUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = GamEmpiresGold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reopen Browser",
                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    is LoginUiState.Success -> {
                        Surface(
                            shape = CircleShape,
                            color = GamEmpiresOnlineGreen,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Connected successfully!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GamEmpiresOnlineGreen
                        )
                    }

                    is LoginUiState.Error -> {
                        Text(
                            text = uiState.message,
                            fontSize = 13.sp,
                            color = Color(0xFFEF4444),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        PasswordLoginForm(
                            onPasswordLogin = onPasswordLogin,
                            onBrowserLogin = { onStartLogin(NextcloudAuthStorage.DEFAULT_SERVER_URL) }
                        )
                    }
                }
            }
        }
    }
}

/** Username + password, sent once to the server to get an app password (the password itself is never stored). */
@Composable
private fun PasswordLoginForm(
    onPasswordLogin: (String, String) -> Unit,
    onBrowserLogin: () -> Unit
) {
    var username by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    // Not saveable on purpose: the password is never written to saved state.
    var password by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var showPassword by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val canSubmit = username.isNotBlank() && password.isNotEmpty()
    val submit = { if (canSubmit) onPasswordLogin(username.trim(), password) }
    val fieldColors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
        focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.outline,
        focusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
        cursorColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        focusedLabelColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
    )

    androidx.compose.material3.OutlinedTextField(
        value = username,
        onValueChange = { username = it },
        label = { Text("Username") },
        singleLine = true,
        colors = fieldColors,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Ascii,
            imeAction = androidx.compose.ui.text.input.ImeAction.Next,
            autoCorrectEnabled = false
        ),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))
    androidx.compose.material3.OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        singleLine = true,
        colors = fieldColors,
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None
            else androidx.compose.ui.text.input.PasswordVisualTransformation(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
            imeAction = androidx.compose.ui.text.input.ImeAction.Done
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { submit() }),
        trailingIcon = {
            androidx.compose.material3.IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                    imageVector = if (showPassword) androidx.compose.material.icons.Icons.Default.VisibilityOff
                        else androidx.compose.material.icons.Icons.Default.Visibility,
                    contentDescription = if (showPassword) "Hide password" else "Show password",
                    tint = GamEmpiresTextSecondary
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(20.dp))
    Button(
        onClick = { submit() },
        enabled = canSubmit,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text(text = "Log in", fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
    Spacer(modifier = Modifier.height(8.dp))
    androidx.compose.material3.TextButton(onClick = onBrowserLogin) {
        Text(
            text = "Log in with browser instead",
            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
            fontSize = 13.sp
        )
    }
    Text(
        text = "Use your office (Windows) username and password. Your password is only used to sign in and is never stored on the phone.",
        fontSize = 12.sp,
        color = GamEmpiresTextMuted,
        textAlign = TextAlign.Center,
        lineHeight = 17.sp
    )
}

@PreviewScreenSizes
@PreviewFontScale
@Composable
private fun LoginScreenPreview() {
    MyApplicationTheme {
        LoginScreen(
            uiState = LoginUiState.Idle,
            onStartLogin = {}
        )
    }
}

