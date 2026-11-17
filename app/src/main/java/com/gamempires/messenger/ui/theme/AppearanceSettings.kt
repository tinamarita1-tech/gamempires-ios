package com.gamempires.messenger.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

enum class AppearanceMode { SYSTEM, LIGHT, DARK;
    companion object {
        fun fromPreference(value: String?): AppearanceMode = entries.find { it.name == value } ?: LIGHT
    }
}

internal val LocalAppearanceMode = staticCompositionLocalOf { AppearanceMode.LIGHT }
internal val LocalSetAppearance = staticCompositionLocalOf<(AppearanceMode) -> Unit> { {} }

@Composable
fun AppearanceSettingsButton() {
    var visible by remember { mutableStateOf(false) }
    val mode = LocalAppearanceMode.current
    val setMode = LocalSetAppearance.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    IconButton(onClick = { visible = true }) {
        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
    }
    if (visible) AlertDialog(
        onDismissRequest = { visible = false },
        title = { Text("Settings") },
        text = {
            androidx.compose.foundation.layout.Column(
                Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                SettingsSectionLabel("Appearance")
                AppearanceMode.entries.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = mode == option, role = Role.RadioButton, onClick = { setMode(option) })
                            .padding(vertical = 2.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        RadioButton(selected = mode == option, onClick = null)
                        Text(
                            when (option) {
                                AppearanceMode.SYSTEM -> "Use device setting"
                                AppearanceMode.LIGHT -> "Light"
                                AppearanceMode.DARK -> "Dark"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f).padding(start = 12.dp, top = 10.dp, bottom = 10.dp)
                        )
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                SettingsSectionLabel("Notifications")
                SettingsRow("Notification settings") {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                    runCatching { context.startActivity(intent) }
                }
                // Android 14+: incoming calls can only take over the lock screen when this is allowed.
                if (android.os.Build.VERSION.SDK_INT >= 34) {
                    val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)
                    if (notificationManager?.canUseFullScreenIntent() == false) {
                        SettingsRow("Allow call screen on lock screen") {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                            runCatching { context.startActivity(intent) }
                        }
                    }
                }
                // Some phones (e.g. Xiaomi) also need "Show on lock screen" in the app's own permission page.
                SettingsRow("App permissions (lock screen, battery)") {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:${context.packageName}"))
                    runCatching { context.startActivity(intent) }
                }

                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                SettingsSectionLabel("Security")
                var lockOn by remember { mutableStateOf(com.gamempires.messenger.ui.security.AppLock.isEnabled(context)) }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                        Text("Fingerprint lock", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Ask for fingerprint or screen lock when opening the app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = lockOn, onCheckedChange = { turnOn ->
                        val lock = com.gamempires.messenger.ui.security.AppLock
                        if (!turnOn) {
                            lock.authenticate(context, "Turn off fingerprint lock", onSuccess = {
                                lock.setEnabled(context, false); lockOn = false
                            })
                        } else if (!lock.isAvailable(context)) {
                            android.widget.Toast.makeText(context,
                                "Set up a fingerprint or screen lock in the phone settings first",
                                android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            lock.authenticate(context, "Turn on fingerprint lock", onSuccess = {
                                lock.setEnabled(context, true); lockOn = true
                            })
                        }
                    })
                }

                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                SettingsSectionLabel("About")
                SettingsRow(if (exporting) "Preparing diagnostics…" else "Share call diagnostics", enabled = !exporting) {
                    exporting = true
                    scope.launch {
                        try {
                            val file = com.gamempires.messenger.calls.CallDiagnostics.export(context)
                            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share call diagnostics"))
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Could not export call diagnostics", android.widget.Toast.LENGTH_LONG).show()
                        } finally { exporting = false }
                    }
                }
                SettingsRow("Open-source licences") { showLicenses = true }
                Text(
                    "Version ${com.gamempires.messenger.BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = { visible = false }) { Text("Done") } }
    )
    if (showLicenses) OpenSourceLicensesDialog(onDismiss = { showLicenses = false })
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

/** Full-width, left-aligned row so every option lines up with the section labels. */
@Composable
private fun SettingsRow(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp)
    )
}

@Composable
private fun OpenSourceLicensesDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val licenses = remember {
        runCatching {
            context.assets.open("open_source_licenses.txt").bufferedReader().use { it.readText() }
        }.getOrDefault("Licence information is not available.")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open-source licences") },
        text = {
            androidx.compose.foundation.layout.Box(
                Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Text(licenses, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
