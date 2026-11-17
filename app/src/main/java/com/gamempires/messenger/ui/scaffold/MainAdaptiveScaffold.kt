package com.gamempires.messenger.ui.scaffold

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.setValue
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.gamempires.messenger.ui.chatlist.ChatListScreen
import com.gamempires.messenger.ui.chatlist.ChatListViewModel
import com.gamempires.messenger.ui.home.GamEmpiresHomeScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainAdaptiveScaffold(
    viewModel: ChatListViewModel,
    modifier: Modifier = Modifier,
    isLoggedIn: Boolean = false,
    onOpenLogin: () -> Unit = {},
    onLogout: () -> Unit = {},
    detailContent: @Composable (chatId: String, onBackClick: () -> Unit) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val coroutineScope = rememberCoroutineScope()

    val selectedChatId = uiState.selectedChatId
    val isHome = selectedChatId == null

    BackHandler(enabled = navigator.canNavigateBack() || selectedChatId != null) {
        viewModel.selectChat(null)
        if (navigator.canNavigateBack()) {
            coroutineScope.launch {
                navigator.navigateBack()
            }
        }
    }

    val listContent: @Composable () -> Unit = {
            ChatListScreen(
                uiState = uiState,
                onSearchQueryChange = viewModel::setSearchQuery,
                onFilterTabSelected = viewModel::setFilterTab,
                isHomeSelected = isHome,
                onRefresh = viewModel::refresh,
                onSearchUsers = viewModel::searchUsers,
                onCreateOneToOneChat = { userId ->
                    viewModel.createOneToOneChat(userId) { chatId ->
                        coroutineScope.launch {
                            navigator.navigateTo(
                                pane = ListDetailPaneScaffoldRole.Detail,
                                contentKey = chatId
                            )
                        }
                    }
                },
                onHomeSelected = {
                    viewModel.selectChat(null)
                    if (navigator.canNavigateBack()) {
                        coroutineScope.launch {
                            navigator.navigateBack()
                        }
                    }
                },
                onChatSelected = { chatId ->
                    viewModel.selectChat(chatId)
                    coroutineScope.launch {
                        navigator.navigateTo(
                            pane = ListDetailPaneScaffoldRole.Detail,
                            contentKey = chatId
                        )
                    }
                },
                onTogglePinChat = viewModel::togglePinChat,
                onToggleMuteChat = viewModel::toggleMuteChat,
                onMarkChatAsRead = viewModel::markChatAsRead,
                onUpdatePresence = viewModel::updateUserPresence,
                isLoggedIn = isLoggedIn,
                onOpenLogin = onOpenLogin,
                onLogout = onLogout
            )
    }

    // Phones: the chat slides in over the list (list stays loaded underneath, so going back is instant
    // and keeps its scroll position). Wide screens keep the side-by-side list/detail layout below.
    val compact = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 600
    if (compact) {
        CompactChatHost(
            selectedChatId = selectedChatId,
            listContent = listContent,
            detailContent = { chatId -> detailContent(chatId) { viewModel.selectChat(null) } }
        )
        return
    }

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
          AnimatedPane { listContent() }
        },
        detailPane = {
          AnimatedPane {
            val activeChatId = navigator.currentDestination?.contentKey ?: selectedChatId
            if (activeChatId != null) {
                detailContent(activeChatId) {
                    viewModel.selectChat(null)
                    if (navigator.canNavigateBack()) {
                        coroutineScope.launch {
                            navigator.navigateBack()
                        }
                    }
                }
            } else {
                GamEmpiresHomeScreen(
                    userName = uiState.currentUser?.name.takeIf { !it.isNullOrBlank() } ?: "GamEmpires User",
                    onStartMeeting = {
                        val firstChat = uiState.chats.firstOrNull()?.id
                        if (firstChat != null) {
                            viewModel.selectChat(firstChat)
                            coroutineScope.launch {
                                navigator.navigateTo(
                                    pane = ListDetailPaneScaffoldRole.Detail,
                                    contentKey = firstChat
                                )
                            }
                        }
                    },
                    onNewConversation = {
                        val firstChat = uiState.chats.firstOrNull()?.id
                        if (firstChat != null) {
                            viewModel.selectChat(firstChat)
                            coroutineScope.launch {
                                navigator.navigateTo(
                                    pane = ListDetailPaneScaffoldRole.Detail,
                                    contentKey = firstChat
                                )
                            }
                        }
                    }
                )
            }
          }
        },
        modifier = modifier
    )
}

@Composable
private fun CompactChatHost(
    selectedChatId: String?,
    listContent: @Composable () -> Unit,
    detailContent: @Composable (chatId: String) -> Unit
) {
    // Keep showing the last chat while it slides out.
    var shownChatId by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(selectedChatId) }
    if (selectedChatId != null) shownChatId = selectedChatId
    val duration = 280
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        listContent()
        AnimatedVisibility(
            visible = selectedChatId != null,
            enter = slideInHorizontally(tween(duration, easing = FastOutSlowInEasing)) { it } +
                fadeIn(tween(duration / 2)),
            exit = slideOutHorizontally(tween(duration, easing = FastOutSlowInEasing)) { it } +
                fadeOut(tween(duration))
        ) {
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxSize(),
                color = androidx.compose.material3.MaterialTheme.colorScheme.background
            ) {
                shownChatId?.let { detailContent(it) }
            }
        }
    }
}
