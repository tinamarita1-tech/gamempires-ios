package com.gamempires.messenger.ui.chatlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamempires.messenger.model.PresenceStatus
import com.gamempires.messenger.model.User

@Composable
fun PresenceSelectorDialog(
    currentUser: User?,
    onDismissRequest: () -> Unit,
    onStatusSelected: (PresenceStatus, String?) -> Unit,
    onLogout: () -> Unit = {},
    serverUrl: String = "talk.gamempires.com"
) {
    var selectedStatus by remember(currentUser) {
        mutableStateOf(currentUser?.status ?: PresenceStatus.ONLINE)
    }
    var customStatusText by remember(currentUser) {
        val initial = currentUser?.customStatus ?: ""
        mutableStateOf(
            if (initial.contains("Nextcloud", ignoreCase = true) || initial.contains("Talk", ignoreCase = true)) "" else initial
        )
    }
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            title = {
                Text("Log out", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to log out from GamEmpires Chat? You will need to sign in again to access your messages.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirmation = false
                        onDismissRequest()
                        onLogout()
                    }
                ) {
                    Text("Log out", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UserAvatar(
                    imageUrl = currentUser?.avatarUrl,
                    name = currentUser?.name ?: "User",
                    presenceStatus = selectedStatus,
                    size = 40.dp,
                    showPresence = true
                )
                Column {
                    Text(
                        text = currentUser?.name ?: "Set Your Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Choose your availability",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Settings (appearance, notifications, about) sits at the top-right of the profile dialog.
                com.gamempires.messenger.ui.theme.AppearanceSettingsButton()
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customStatusText,
                    onValueChange = { customStatusText = it },
                    label = { Text("Custom status") },
                    placeholder = { Text("What's your status?") },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Presence Status",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                PresenceOptionRow(
                    status = PresenceStatus.ONLINE,
                    label = "Available",
                    description = "Active and receiving notifications",
                    color = Color(0xFF10B981),
                    isSelected = selectedStatus == PresenceStatus.ONLINE,
                    onSelect = { selectedStatus = PresenceStatus.ONLINE }
                )

                PresenceOptionRow(
                    status = PresenceStatus.AWAY,
                    label = "In a meeting",
                    description = "Calendar event or call in progress",
                    color = Color(0xFFF59E0B),
                    isSelected = selectedStatus == PresenceStatus.AWAY,
                    onSelect = { selectedStatus = PresenceStatus.AWAY }
                )

                PresenceOptionRow(
                    status = PresenceStatus.BUSY,
                    label = "Focus time",
                    description = "Mute sound & notification popups",
                    color = Color(0xFF6366F1),
                    isSelected = selectedStatus == PresenceStatus.BUSY,
                    onSelect = { selectedStatus = PresenceStatus.BUSY }
                )

                PresenceOptionRow(
                    status = PresenceStatus.OFFLINE,
                    label = "Stepping away",
                    description = "Temporarily away from workstation",
                    color = Color(0xFF94A3B8),
                    isSelected = selectedStatus == PresenceStatus.OFFLINE,
                    onSelect = { selectedStatus = PresenceStatus.OFFLINE }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Server connection status badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Connected • $serverUrl",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dedicated Log out button styled in muted red/coral (Requirement 6)
                OutlinedButton(
                    onClick = { showLogoutConfirmation = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF5350)
                    ),
                    border = BorderStroke(1.dp, Color(0x66EF5350)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Log out",
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log out from GamEmpires",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFEF5350)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "GamEmpires Chat ${com.gamempires.messenger.BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onStatusSelected(selectedStatus, customStatusText.ifBlank { null })
                    onDismissRequest()
                }
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PresenceOptionRow(
    status: PresenceStatus,
    label: String,
    description: String,
    color: Color,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
        }
    }
}
