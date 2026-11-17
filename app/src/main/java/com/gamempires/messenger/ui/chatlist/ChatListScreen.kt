package com.gamempires.messenger.ui.chatlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gamempires.messenger.model.ChatFilterTab
import com.gamempires.messenger.model.PresenceStatus
import com.gamempires.messenger.model.User
import com.gamempires.messenger.ui.chatlist.components.ChatListItem
import com.gamempires.messenger.ui.chatlist.components.PresenceSelectorDialog
import com.gamempires.messenger.ui.chatlist.components.UserAvatar
import com.gamempires.messenger.ui.components.GamEmpiresLogo
import com.gamempires.messenger.ui.components.GamingBackground
import com.gamempires.messenger.ui.home.GamingHomeTab
import com.gamempires.messenger.ui.theme.CorporateBlue
import com.gamempires.messenger.ui.theme.GamEmpiresBg
import com.gamempires.messenger.ui.theme.GamEmpiresBorder
import com.gamempires.messenger.ui.theme.GamEmpiresSurface
import com.gamempires.messenger.ui.theme.GamEmpiresTextMuted
import com.gamempires.messenger.ui.theme.GamEmpiresTextPrimary
import com.gamempires.messenger.ui.theme.GamEmpiresTextSecondary
import com.gamempires.messenger.ui.theme.MyApplicationTheme
import com.gamempires.messenger.ui.theme.StatusAway
import com.gamempires.messenger.ui.theme.StatusBusy
import com.gamempires.messenger.ui.theme.StatusOffline
import com.gamempires.messenger.ui.theme.StatusOnline
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    uiState: ChatListUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterTabSelected: (ChatFilterTab) -> Unit,
    onChatSelected: (String) -> Unit,
    onRefresh: () -> Unit = {},
    onSearchUsers: suspend (String) -> List<User> = { emptyList() },
    onCreateOneToOneChat: (String) -> Unit = {},
    onHomeSelected: () -> Unit = {},
    isHomeSelected: Boolean = false,
    onTogglePinChat: (String) -> Unit = {},
    onToggleMuteChat: (String) -> Unit = {},
    onMarkChatAsRead: (String) -> Unit = {},
    onUpdatePresence: (PresenceStatus, String?) -> Unit = { _, _ -> },
    isLoggedIn: Boolean = false,
    onOpenLogin: () -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showPresenceDialog by rememberSaveable { mutableStateOf(false) }
    var showNewChatDialog by rememberSaveable { mutableStateOf(false) }
    var otherExpanded by rememberSaveable { mutableStateOf(true) }

    if (showPresenceDialog) {
        PresenceSelectorDialog(
            currentUser = uiState.currentUser,
            onDismissRequest = { showPresenceDialog = false },
            onStatusSelected = onUpdatePresence,
            onLogout = onLogout
        )
    }

    if (showNewChatDialog) {
        NewChatDialog(
            onDismissRequest = { showNewChatDialog = false },
            onSearchUsers = onSearchUsers,
            onUserSelected = { userId ->
                showNewChatDialog = false
                onCreateOneToOneChat(userId)
            }
        )
    }

    var peopleResults by remember { mutableStateOf<List<User>>(emptyList()) }
    LaunchedEffect(uiState.searchQuery) {
        val q = uiState.searchQuery.trim()
        if (q.length >= 2) {
            delay(300)
            peopleResults = try { onSearchUsers(q) } catch (_: Exception) { emptyList() }
        } else {
            peopleResults = emptyList()
        }
    }

    GamingBackground(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Offline banner
            AnimatedVisibility(visible = uiState.isOffline) {
                Surface(
                    color = Color(0xFF854D0E),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Offline indicator",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No connection – retrying",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }

            // Top bar on MaterialTheme.colorScheme.surface
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GamEmpiresLogo(
                        fontSize = 18.sp,
                        chatTagSize = 12.sp
                    )

                    val currentUsr = uiState.currentUser
                    val presenceLabel = when (currentUsr?.status) {
                        PresenceStatus.ONLINE -> "Available"
                        PresenceStatus.AWAY -> "Away"
                        PresenceStatus.BUSY -> "Focus time"
                        PresenceStatus.OFFLINE, null -> "Offline"
                    }
                    val presenceColor = when (currentUsr?.status) {
                        PresenceStatus.ONLINE -> StatusOnline
                        PresenceStatus.AWAY -> StatusAway
                        PresenceStatus.BUSY -> StatusBusy
                        PresenceStatus.OFFLINE, null -> StatusOffline
                    }

                    // Pill-shaped availability button
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(
                                onClick = { showPresenceDialog = true },
                                onClickLabel = "Open status and presence dialog"
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(presenceColor)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = presenceLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(8.dp))
                            UserAvatar(
                                imageUrl = currentUsr?.avatarUrl,
                                name = currentUsr?.name?.ifBlank { "User" } ?: "User",
                                presenceStatus = currentUsr?.status ?: PresenceStatus.ONLINE,
                                size = 32.dp,
                                showPresence = true
                            )
                        }
                    }
                }
            }

            // Search bar: 40.dp height CircleShape field with placeholder "Find teammates, chats, or shared docs..."
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (uiState.searchQuery.isEmpty()) {
                            Text(
                                text = "Find teammates, chats, or shared docs...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        var searchActive by remember { mutableStateOf(false) }
                        val searchFocus = remember { androidx.compose.ui.focus.FocusRequester() }
                        LaunchedEffect(searchActive) { if (searchActive) searchFocus.requestFocus() }
                        BasicTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocus)
                                .focusProperties { canFocus = searchActive }
                                .onFocusChanged { if (!it.isFocused && uiState.searchQuery.isEmpty()) searchActive = false }
                        )
                        if (!searchActive) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) { searchActive = true }
                            )
                        }
                    }
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search query",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Filter tabs: CircleShape pills (Home, All, Direct, Teams, Unread)
            val tabOrder = ChatFilterTab.entries
            val tabScroll = rememberScrollState()
            LaunchedEffect(uiState.selectedFilterTab) {
                val idx = tabOrder.indexOf(uiState.selectedFilterTab)
                tabScroll.animateScrollTo(if (idx <= 1) 0 else (tabScroll.maxValue * idx) / tabOrder.lastIndex)
            }
            val pagerState = rememberPagerState(
                initialPage = tabOrder.indexOf(uiState.selectedFilterTab).coerceAtLeast(0),
                pageCount = { tabOrder.size }
            )
            val tabsScope = rememberCoroutineScope()
            LaunchedEffect(pagerState.settledPage) {
                val tab = tabOrder[pagerState.settledPage]
                if (tab != uiState.selectedFilterTab) onFilterTabSelected(tab)
            }
            LaunchedEffect(uiState.selectedFilterTab) {
                val idx = tabOrder.indexOf(uiState.selectedFilterTab)
                if (idx >= 0 && idx != pagerState.currentPage && !pagerState.isScrollInProgress) {
                    pagerState.animateScrollToPage(idx)
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(tabScroll)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp)
            ) {
                tabOrder.forEachIndexed { index, tab ->
                    CorporateTabChip(
                        tab = tab,
                        selected = pagerState.currentPage == index,
                        unreadCount = uiState.totalUnreadCount,
                        onClick = {
                            onFilterTabSelected(tab)
                            tabsScope.launch { pagerState.animateScrollToPage(index) }
                        }
                    )
                }
            }

            // Pager container with 52.dp circular FAB at BottomEnd (20.dp padding)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val pageTab = tabOrder[page]
                    if (pageTab == ChatFilterTab.HOME && uiState.searchQuery.isBlank()) {
                        GamingHomeTab(
                            userName = uiState.currentUser?.name?.ifBlank { null } ?: "Teammate",
                            status = uiState.statusMessage,
                            onOpenChat = onChatSelected,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        PullToRefreshBox(
                            isRefreshing = uiState.isManualRefreshing,
                            onRefresh = onRefresh,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val pageChats = uiState.chatsByTab[pageTab] ?: uiState.chats
                            val favoriteChats = pageChats.filter { it.isPinned }
                            val otherChats = pageChats.filter { !it.isPinned }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item { Spacer(Modifier.height(4.dp)) }

                                // Pinned chats
                                items(favoriteChats, key = { it.id }) { chat ->
                                    val otherUserId = chat.participantIds.find { it != uiState.currentUser?.id }
                                    val otherUser = uiState.users.find { it.id == otherUserId }
                                    val presence = if (otherUserId != null) uiState.userPresences[otherUserId] else null
                                    ChatListItem(
                                        chat = chat,
                                        otherUser = otherUser,
                                        presenceStatus = presence,
                                        typingText = null,
                                        isSelected = chat.id == uiState.selectedChatId && !isHomeSelected,
                                        onChatClick = onChatSelected,
                                        onTogglePin = onTogglePinChat,
                                        onToggleMute = onToggleMuteChat,
                                        onMarkAsRead = onMarkChatAsRead
                                    )
                                }

                                item {
                                    CategoryHeader(
                                        title = "Chats",
                                        isExpanded = otherExpanded,
                                        onToggle = { otherExpanded = !otherExpanded }
                                    )
                                }

                                if (otherExpanded) {
                                    items(otherChats, key = { it.id }) { chat ->
                                        val otherUserId = chat.participantIds.find { it != uiState.currentUser?.id }
                                        val otherUser = uiState.users.find { it.id == otherUserId }
                                        val presence = if (otherUserId != null) uiState.userPresences[otherUserId] else null
                                        ChatListItem(
                                            chat = chat,
                                            otherUser = otherUser,
                                            presenceStatus = presence,
                                            typingText = null,
                                            isSelected = chat.id == uiState.selectedChatId && !isHomeSelected,
                                            onChatClick = onChatSelected,
                                            onTogglePin = onTogglePinChat,
                                            onToggleMute = onToggleMuteChat,
                                            onMarkAsRead = onMarkChatAsRead
                                        )
                                    }
                                }

                                // Search people results
                                if (peopleResults.isNotEmpty() && uiState.searchQuery.isNotBlank()) {
                                    item {
                                        CategoryHeader(title = "Start a new chat", isExpanded = true, onToggle = {}, showArrow = false)
                                    }
                                    items(peopleResults, key = { "person_" + it.id }) { user ->
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 4.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable { onCreateOneToOneChat(user.id) }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                            ) {
                                                UserAvatar(
                                                    imageUrl = user.avatarUrl,
                                                    name = user.name,
                                                    presenceStatus = user.status,
                                                    size = 42.dp
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text(
                                                        text = user.name,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = "Tap to chat",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.Default.AddComment,
                                                    contentDescription = "Start a chat with ${user.name}",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                item { Spacer(Modifier.height(80.dp)) }
                            }
                        }
                    }
                }

                // 52.dp circular FloatingActionButton at Alignment.BottomEnd (20.dp padding)
                FloatingActionButton(
                    onClick = { showNewChatDialog = true },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                        .size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Conversation",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CorporateTabChip(
    tab: ChatFilterTab,
    selected: Boolean,
    unreadCount: Int,
    onClick: () -> Unit
) {
    val shape = CircleShape
    val icon = if (tab == ChatFilterTab.HOME) Icons.Outlined.Home else null
    val label = when (tab) {
        ChatFilterTab.HOME -> "Home"
        ChatFilterTab.ALL -> "All"
        ChatFilterTab.DIRECT -> "Direct"
        ChatFilterTab.GROUPS -> "Teams"
        ChatFilterTab.UNREAD -> "Unread"
    }
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

    Surface(
        shape = shape,
        color = bg,
        border = border,
        modifier = Modifier
            .height(34.dp)
            .clip(shape)
            .clickable(onClick = onClick, onClickLabel = "Show $label")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
            if (tab == ChatFilterTab.UNREAD && unreadCount > 0) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selected) Color.White else MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun NewChatDialog(
    onDismissRequest: () -> Unit,
    onSearchUsers: suspend (String) -> List<User>,
    onUserSelected: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<User>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.trim().length >= 2) {
            delay(300)
            isSearching = true
            try {
                results = onSearchUsers(query.trim())
            } catch (_: Exception) {
                results = emptyList()
            } finally {
                isSearching = false
            }
        } else {
            results = emptyList()
            isSearching = false
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "New Conversation",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text("Search user by name...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search users",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else if (results.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        items(results, key = { it.id }) { user ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onUserSelected(user.id) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                UserAvatar(
                                    imageUrl = user.avatarUrl,
                                    name = user.name,
                                    presenceStatus = user.status,
                                    size = 36.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "@${user.id}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else if (query.trim().length >= 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No users found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Type at least 2 characters to search",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        modifier = Modifier.clickable(onClick = onDismissRequest)
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    showArrow: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = showArrow,
                onClick = onToggle,
                onClickLabel = if (isExpanded) "Collapse $title section" else "Expand $title section"
            )
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (showArrow) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@PreviewScreenSizes
@PreviewFontScale
@Composable
private fun ChatListScreenPreview() {
    MyApplicationTheme {
        ChatListScreen(
            uiState = ChatListUiState(
                chats = listOf(
                    com.gamempires.messenger.model.Chat(
                        id = "chat_1",
                        type = ChatFilterTab.ALL.name.let { com.gamempires.messenger.model.ChatType.DIRECT },
                        title = "Team Shadow",
                        avatarUrl = null,
                        participantIds = listOf("user_1"),
                        lastMessage = null,
                        unreadCount = 2,
                        isPinned = true,
                        isMuted = false,
                        updatedAt = System.currentTimeMillis()
                    ),
                    com.gamempires.messenger.model.Chat(
                        id = "chat_2",
                        type = com.gamempires.messenger.model.ChatType.GROUP,
                        title = "Announcements",
                        avatarUrl = null,
                        participantIds = listOf("user_1", "user_2"),
                        lastMessage = null,
                        unreadCount = 0,
                        isPinned = false,
                        isMuted = false,
                        updatedAt = System.currentTimeMillis() - 3600000L
                    )
                ),
                isLoggedIn = true
            ),
            onSearchQueryChange = {},
            onFilterTabSelected = {},
            onChatSelected = {}
        )
    }
}
