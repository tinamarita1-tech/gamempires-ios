package com.gamempires.messenger.calls

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun AudioCallHost(viewModel: AudioCallViewModel) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val microphone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), viewModel::permissionResult)
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.setForeground(true)
                Lifecycle.Event.ON_STOP -> viewModel.setForeground(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.setForeground(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); viewModel.setForeground(false) }
    }
    LaunchedEffect(state.phase) {
        if (state.phase == AudioCallPhase.PERMISSION) microphone.launch(Manifest.permission.RECORD_AUDIO)
    }
    if (state.phase != AudioCallPhase.IDLE) AlertDialog(
        onDismissRequest = { if (state.phase == AudioCallPhase.ENDED) viewModel.dismiss() },
        title = { Text(if (state.phase == AudioCallPhase.INCOMING) "Incoming audio call" else "Audio call") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.name.isNotBlank()) Text(state.name, style = MaterialTheme.typography.titleMedium)
                Text(when (state.phase) {
                    AudioCallPhase.INCOMING -> "Answer to enable your microphone."
                    AudioCallPhase.PERMISSION -> "Allow microphone access to continue."
                    else -> state.message.ifBlank { "Connecting…" }
                })
                if (state.phase in listOf(AudioCallPhase.CONNECTING, AudioCallPhase.WAITING)) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (state.phase in listOf(AudioCallPhase.WAITING, AudioCallPhase.ACTIVE)) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::mute) { Text(if (state.muted) "Unmute" else "Mute") }
                    OutlinedButton(onClick = viewModel::speaker) { Text(if (state.speaker) "Earpiece" else "Speaker") }
                }
                if (state.phase in listOf(AudioCallPhase.WAITING, AudioCallPhase.ACTIVE)) Text("You can leave the app or lock the phone; the call keeps going. Hang up from here or from the notification.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            if (state.phase == AudioCallPhase.INCOMING) Button(onClick = viewModel::answer) { Text("Answer") }
            else TextButton(onClick = { if (state.phase == AudioCallPhase.ENDED) viewModel.dismiss() else viewModel.endCall() }) {
                Text(if (state.phase == AudioCallPhase.ENDED) "Close" else "End call")
            }
        },
        dismissButton = { if (state.phase == AudioCallPhase.INCOMING) TextButton(onClick = viewModel::dismiss) { Text("Decline") } }
    )
}
