package com.gamempires.messenger.ui.chatlist

import com.gamempires.messenger.fake.FakeChatRepository
import com.gamempires.messenger.model.ChatFilterTab
import com.gamempires.messenger.model.PresenceStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeChatRepository
    private lateinit var viewModel: ChatListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeChatRepository()
        viewModel = ChatListViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiState() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertNotNull(state.currentUser)
        assertTrue(state.chats.isNotEmpty())
        assertEquals(ChatFilterTab.ALL, state.selectedFilterTab)
    }

    @Test
    fun testFilterByTabDirect() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.setFilterTab(ChatFilterTab.DIRECT)

        val state = viewModel.uiState.value
        assertEquals(ChatFilterTab.DIRECT, state.selectedFilterTab)
        assertTrue(state.chats.all { it.type == com.gamempires.messenger.model.ChatType.DIRECT })
    }

    @Test
    fun testSearchQueryFiltering() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.setSearchQuery("Bob")

        val state = viewModel.uiState.value
        assertEquals("Bob", state.searchQuery)
        assertTrue(state.chats.isNotEmpty())
    }

    @Test
    fun testSelectChat() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.selectChat("chat_1")

        val state = viewModel.uiState.value
        assertEquals("chat_1", state.selectedChatId)
    }

    @Test
    fun testUpdateUserPresence() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.updateUserPresence(PresenceStatus.BUSY, "Do Not Disturb")

        val currentUser = repository.currentUser.value
        assertEquals(PresenceStatus.BUSY, currentUser.status)
        assertEquals("Do Not Disturb", currentUser.customStatus)
    }
}
