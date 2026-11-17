package com.gamempires.messenger

import android.os.Bundle
import androidx.compose.ui.graphics.luminance
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gamempires.messenger.ui.auth.LoginScreen
import com.gamempires.messenger.ui.auth.LoginUiState
import com.gamempires.messenger.ui.auth.LoginViewModel
import com.gamempires.messenger.ui.chatlist.ChatListViewModel
import com.gamempires.messenger.ui.detail.ChatDetailScreen
import com.gamempires.messenger.ui.detail.ChatDetailViewModel
import com.gamempires.messenger.ui.scaffold.MainAdaptiveScaffold
import com.gamempires.messenger.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @javax.inject.Inject
    lateinit var pushNotifier: com.gamempires.messenger.push.PushNotifier

    @javax.inject.Inject
    lateinit var sessionGuard: com.gamempires.messenger.push.SessionGuard

    @javax.inject.Inject
    lateinit var authStorage: com.gamempires.messenger.data.nextcloud.auth.NextcloudAuthStorage

    private fun handlePushIntent(intent: android.content.Intent?) {
        // Incoming call from the lock screen: show the call screen over the lock screen.
        if (com.gamempires.messenger.push.PushIntents.isCallLaunch(intent)) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        com.gamempires.messenger.push.PushIntents.handle(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePushIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        com.gamempires.messenger.push.PushIntents.appInForeground = true
        com.gamempires.messenger.ui.security.AppLock.onForeground(
            this, com.gamempires.messenger.push.PushIntents.isCallLaunch(intent)
        )
        // Signed in on another phone while this one was closed or offline? Sign out here.
        lifecycleScope.launch { sessionGuard.check() }
    }

    override fun onStop() {
        com.gamempires.messenger.push.PushIntents.appInForeground = false
        com.gamempires.messenger.ui.security.AppLock.onBackground()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handlePushIntent(intent)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val audioCalls: com.gamempires.messenger.calls.AudioCallViewModel = hiltViewModel()
                com.gamempires.messenger.calls.AudioCallHost(audioCalls)
                val pushAction by com.gamempires.messenger.push.PushIntents.action.collectAsState()
                LaunchedEffect(pushAction) {
                    when (val action = pushAction) {
                        is com.gamempires.messenger.push.PushAction.ShowIncomingCall -> {
                            // Keep the notification ringing until the user answers or declines.
                            audioCalls.openFromPush(action.token, action.name, autoAnswer = false)
                            com.gamempires.messenger.push.PushIntents.consume(action)
                        }
                        is com.gamempires.messenger.push.PushAction.AnswerCall -> {
                            pushNotifier.cancelCallAlerts()
                            audioCalls.openFromPush(action.token, action.name, autoAnswer = true)
                            com.gamempires.messenger.push.PushIntents.consume(action)
                        }
                        else -> Unit
                    }
                }
                // After a call that was shown over the lock screen, stop showing the app over the lock screen.
                val callState by audioCalls.state.collectAsState()
                val callShown = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                LaunchedEffect(callState.phase) {
                    if (callState.phase != com.gamempires.messenger.calls.AudioCallPhase.IDLE) {
                        callShown.value = true
                    } else if (callShown.value) {
                        callShown.value = false
                        setShowWhenLocked(false)
                        setTurnScreenOn(false)
                    }
                }
                // Company-logo loading screen on a normal app start (not when a call opens the app).
                val showStartupSplash = androidx.compose.runtime.saveable.rememberSaveable {
                    androidx.compose.runtime.mutableStateOf(
                        savedInstanceState == null && !com.gamempires.messenger.push.PushIntents.isCallLaunch(intent)
                    )
                }
                LaunchedEffect(Unit) {
                    if (showStartupSplash.value) {
                        delay(1400)
                        showStartupSplash.value = false
                    }
                }
                val lightBars = MaterialTheme.colorScheme.background.luminance() > 0.5f
                androidx.compose.runtime.SideEffect {
                    androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = lightBars
                        isAppearanceLightNavigationBars = lightBars
                    }
                }
                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        // Takes the automatic first focus when the app opens, so the search bar
                        // doesn't get focused and the keyboard doesn't pop up on launch.
                        .focusable(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val loginViewModel: LoginViewModel = hiltViewModel()
                    val isLoggedIn by loginViewModel.isLoggedIn.collectAsState()
                    val loginUiState by loginViewModel.uiState.collectAsState()

                    LaunchedEffect(loginUiState) {
                        if (loginUiState is LoginUiState.Success) {
                            delay(1000)
                            loginViewModel.dismiss()
                        }
                    }

                    if (!isLoggedIn) {
                        LoginScreen(
                            uiState = loginUiState,
                            onStartLogin = loginViewModel::startLoginFlow,
                            onPasswordLogin = loginViewModel::loginWithPassword
                        )
                    } else {
                        val viewModel: ChatListViewModel = hiltViewModel()
                        val lifecycleOwner = LocalLifecycleOwner.current

                        // Android 13+: ask once for permission to show message and call notifications.
                        val notificationPermission = androidx.activity.compose.rememberLauncherForActivityResult(
                            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                        ) { }
                        LaunchedEffect(Unit) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                                androidx.core.content.ContextCompat.checkSelfPermission(
                                    this@MainActivity, android.Manifest.permission.POST_NOTIFICATIONS
                                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }

                        // A tapped message notification opens that conversation.
                        LaunchedEffect(pushAction) {
                            val action = pushAction
                            if (action is com.gamempires.messenger.push.PushAction.OpenChat) {
                                viewModel.selectChat(action.token)
                                com.gamempires.messenger.push.PushIntents.consume(action)
                            }
                        }

                        // Room requests also keep Nextcloud presence online. Stop the
                        // timer in the background so inactive users can expire normally.
                        LaunchedEffect(lifecycleOwner) {
                            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                                while (isActive) {
                                    viewModel.backgroundSync()
                                    delay(30_000)
                                }
                            }
                        }

                        MainAdaptiveScaffold(
                            viewModel = viewModel,
                            isLoggedIn = true,
                            onOpenLogin = {},
                            onLogout = loginViewModel::logout,
                            detailContent = { chatId, onBackClick ->
                                val detailViewModel: ChatDetailViewModel = hiltViewModel(key = chatId)
                                detailViewModel.setChatId(chatId)
                                val detailUiState by detailViewModel.uiState.collectAsState()
                                val allChats by detailViewModel.allChats.collectAsState()
                                ChatDetailScreen(
                                    uiState = detailUiState,
                                    onBackClick = onBackClick,
                                    onInputTextChange = detailViewModel::onInputTextChanged,
                                    onSendMessage = detailViewModel::sendMessage,
                                    onAddReaction = detailViewModel::addReaction,
                                    onEditMessage = detailViewModel::editMessage,
                                    onDeleteMessage = detailViewModel::deleteMessage,
                                    onSetReplyingTo = detailViewModel::setReplyingTo,
                                    onRetryLoadHistory = detailViewModel::retryLoadHistory,
                                    onStartAudioCall = {
                                        detailUiState.chat?.let { chat ->
                                            audioCalls.dial(chat.id, chat.title, chat.type == com.gamempires.messenger.model.ChatType.DIRECT)
                                        }
                                    },
                                    allChats = allChats,
                                    onDownloadAttachment = { attachment ->
                                        detailViewModel.downloadAndOpenFile(attachment, this@MainActivity)
                                    },
                                    onSaveToDownloads = { attachment ->
                                        detailViewModel.saveToDownloads(attachment, this@MainActivity)
                                    },
                                    onForwardMessage = { targetChat, message ->
                                        detailViewModel.forwardMessage(targetChat, message, this@MainActivity)
                                    },
                                    onShareMessage = { message ->
                                        detailViewModel.shareMessageExternally(message, this@MainActivity)
                                    },
                                    onShareAttachment = { attachment ->
                                        detailViewModel.downloadAndShareFileExternally(attachment, this@MainActivity)
                                    },
                                    onUploadFiles = { uris ->
                                        detailViewModel.uploadFiles(uris, this@MainActivity)
                                    },
                                    onStartSync = detailViewModel::startActiveSync,
                                    onStopSync = detailViewModel::stopActiveSync
                                )
                            }
                        )
                    }
                }
                // Fingerprint lock (only when turned on in Settings and someone is signed in).
                val appLocked by com.gamempires.messenger.ui.security.AppLock.locked.collectAsState()
                val signedIn by authStorage.isLoggedIn.collectAsState()
                if (appLocked && signedIn && !showStartupSplash.value) {
                    com.gamempires.messenger.ui.security.LockScreen()
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = showStartupSplash.value,
                    enter = androidx.compose.animation.EnterTransition.None,
                    exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(350))
                ) {
                    com.gamempires.messenger.ui.components.StartupSplash()
                }
                }
            }
        }
    }
}
