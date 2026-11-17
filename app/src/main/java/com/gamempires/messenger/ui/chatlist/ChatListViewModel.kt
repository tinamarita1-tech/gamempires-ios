package com.gamempires.messenger.ui.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamempires.messenger.data.ChatRepository
import com.gamempires.messenger.model.Chat
import com.gamempires.messenger.model.ChatFilterTab
import com.gamempires.messenger.model.ChatType
import com.gamempires.messenger.model.PresenceStatus
import com.gamempires.messenger.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatListUiState(
    val chats: List<Chat> = emptyList(),
    /** Each tab's own list, so the swipe pager can show the next tab's chats while it slides in. */
    val chatsByTab: Map<ChatFilterTab, List<Chat>> = emptyMap(),
    val searchQuery: String = "",
    val selectedFilterTab: ChatFilterTab = ChatFilterTab.HOME,
    val currentUser: User? = null,
    val userPresences: Map<String, PresenceStatus> = emptyMap(),
    val typingUsers: Map<String, Set<String>> = emptyMap(),
    val users: List<User> = emptyList(),
    val selectedChatId: String? = null,
    val filterCounts: Map<ChatFilterTab, Int> = emptyMap(),
    /** Home tab status: latest message in the Announcements group (only CEO and HR can post there), with its room id. */
    val statusMessage: Pair<String, com.gamempires.messenger.model.Message>? = null,
    val totalUnreadCount: Int = 0,
    val isLoggedIn: Boolean = false,
    val isRefreshing: Boolean = false,
    val isManualRefreshing: Boolean = false,
    val isBackgroundSyncing: Boolean = false,
    val isOffline: Boolean = false,
    val isSessionRevoked: Boolean = false
)

private data class FlowGroup1(
    val filteredChats: List<Chat>,
    val allChats: List<Chat>,
    val searchQuery: String,
    val filterTab: ChatFilterTab
)

private data class FlowGroup2(
    val currentUser: User,
    val userPresences: Map<String, PresenceStatus>,
    val typingUsers: Map<String, Set<String>>,
    val users: List<User>,
    val selectedChatId: String?
)

private data class StatusGroup(
    val isLoggedIn: Boolean,
    val isManualRefreshing: Boolean,
    val isBackgroundSyncing: Boolean,
    val isOffline: Boolean,
    val isSessionRevoked: Boolean
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _selectedFilterTab = MutableStateFlow(ChatFilterTab.HOME)
    private val _selectedChatId = MutableStateFlow<String?>(null)
    private val _isManualRefreshing = MutableStateFlow(false)
    private val _isBackgroundSyncing = MutableStateFlow(false)

    /** Home tab status when the Announcements room's last item is a system note (e.g. "picture changed"). */
    private val _announcementStatus = MutableStateFlow<Pair<String, com.gamempires.messenger.model.Message>?>(null)
    private var announcementCheckedKey: String? = null

    init {
        viewModelScope.launch {
            chatRepository.chats.collect { chats ->
                val room = chats.firstOrNull {
                    it.type == ChatType.GROUP && it.title.trim().equals("Announcements", ignoreCase = true)
                } ?: return@collect
                val last = room.lastMessage
                if (last != null && !last.isSystemMessage && !last.isDeleted) {
                    _announcementStatus.value = room.id to last
                    announcementCheckedKey = room.id + ":" + last.id
                    return@collect
                }
                val key = room.id + ":" + (last?.id ?: "none")
                if (key == announcementCheckedKey) return@collect   // already looked up for this state
                announcementCheckedKey = key
                val real = chatRepository.latestUserMessage(room.id)
                _announcementStatus.value = real?.let { room.id to it }
            }
        }
    }

    private val group1Flow = combine(
        chatRepository.filteredChats,
        chatRepository.chats,
        chatRepository.searchQuery,
        _selectedFilterTab
    ) { filtered, allChats, query, filterTab ->
        FlowGroup1(filtered, allChats, query, filterTab)
    }

    private val group2Flow = combine(
        chatRepository.currentUser,
        chatRepository.userPresences,
        chatRepository.typingUsers,
        chatRepository.users,
        _selectedChatId
    ) { currentUsr, presences, typingMap, allUsers, selectedId ->
        FlowGroup2(currentUsr, presences, typingMap, allUsers, selectedId)
    }

    private val statusFlow = combine(
        chatRepository.isLoggedIn,
        _isManualRefreshing,
        _isBackgroundSyncing,
        chatRepository.isOffline,
        chatRepository.isSessionRevoked
    ) { loggedIn, manualRefreshing, backgroundSyncing, offline, revoked ->
        StatusGroup(loggedIn, manualRefreshing, backgroundSyncing, offline, revoked)
    }

    val uiState: StateFlow<ChatListUiState> = combine(
        group1Flow,
        group2Flow,
        statusFlow,
        _announcementStatus
    ) { g1, g2, status, announcement ->
        val counts = mapOf(
            ChatFilterTab.HOME to 0,
            ChatFilterTab.ALL to g1.filteredChats.size,
            ChatFilterTab.DIRECT to g1.filteredChats.count { it.type == ChatType.DIRECT },
            ChatFilterTab.GROUPS to g1.filteredChats.count { it.type == ChatType.GROUP },
            ChatFilterTab.UNREAD to g1.filteredChats.count { it.unreadCount > 0 }
        )

        val byTab = ChatFilterTab.entries.associateWith { tab ->
            when (tab) {
                ChatFilterTab.HOME, ChatFilterTab.ALL -> g1.filteredChats
                ChatFilterTab.DIRECT -> g1.filteredChats.filter { it.type == ChatType.DIRECT }
                ChatFilterTab.GROUPS -> g1.filteredChats.filter { it.type == ChatType.GROUP }
                ChatFilterTab.UNREAD -> g1.filteredChats.filter { it.unreadCount > 0 }
            }
        }
        val displayChats = byTab[g1.filterTab] ?: g1.filteredChats

        val unreadTotal = g1.allChats.sumOf { it.unreadCount }

        ChatListUiState(
            chats = displayChats,
            chatsByTab = byTab,
            searchQuery = g1.searchQuery,
            selectedFilterTab = g1.filterTab,
            currentUser = g2.currentUser,
            userPresences = g2.userPresences,
            typingUsers = g2.typingUsers,
            users = g2.users,
            selectedChatId = g2.selectedChatId,
            filterCounts = counts,
            statusMessage = announcement,
            totalUnreadCount = unreadTotal,
            isLoggedIn = status.isLoggedIn,
            isRefreshing = status.isManualRefreshing,
            isManualRefreshing = status.isManualRefreshing,
            isBackgroundSyncing = status.isBackgroundSyncing,
            isOffline = status.isOffline,
            isSessionRevoked = status.isSessionRevoked
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatListUiState()
    )

    fun manualRefresh() {
        viewModelScope.launch {
            _isManualRefreshing.value = true
            try {
                chatRepository.refreshRooms()
            } finally {
                _isManualRefreshing.value = false
            }
        }
    }

    fun backgroundSync() {
        viewModelScope.launch {
            _isBackgroundSyncing.value = true
            try {
                chatRepository.refreshRooms()
            } finally {
                _isBackgroundSyncing.value = false
            }
        }
    }

    fun refresh() {
        manualRefresh()
    }

    suspend fun searchUsers(query: String): List<User> {
        return chatRepository.searchUsers(query)
    }

    fun createOneToOneChat(userId: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val chat = chatRepository.createOneToOneChat(userId)
            if (chat != null) {
                selectChat(chat.id)
                onCreated(chat.id)
            }
        }
    }

    fun setSearchQuery(query: String) {
        chatRepository.setSearchQuery(query)
    }

    fun setFilterTab(tab: ChatFilterTab) {
        _selectedFilterTab.value = tab
    }

    fun selectChat(chatId: String?) {
        _selectedChatId.value = chatId
        if (chatId != null) {
            viewModelScope.launch {
                chatRepository.markChatAsRead(chatId)
            }
        }
    }

    fun togglePinChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.togglePinChat(chatId)
        }
    }

    fun toggleMuteChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.toggleMuteChat(chatId)
        }
    }

    fun markChatAsRead(chatId: String) {
        viewModelScope.launch {
            chatRepository.markChatAsRead(chatId)
        }
    }

    fun updateUserPresence(status: PresenceStatus, customStatus: String? = null) {
        viewModelScope.launch {
            chatRepository.updateCurrentUserPresence(status, customStatus)
        }
    }
}
