# Project Plan

Chat app like Teams and Whatsapp

## Project Brief

# Project Brief: Chat App (MVP)

## Features
- **Direct & Group Messaging**: Real-time one-on-one direct chats and group channels.
- **Media & File Sharing**: In-chat attachments for images, documents, and media files.
- **User Presence & Typing Indicators**: Real-time online/offline status updates and live typing indicators.
- **Chat List & Search**: Main conversation view with recent message previews, unread badges, and user/chat search.

## High-Level Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Navigation & Adaptive Strategy**: **Jetpack Navigation 3** (state-driven navigation) and **Compose Material Adaptive** (using `ListDetailPaneScaffold` for dynamic dual-pane support on foldables and tablets)
- **Asynchrony & State**: Kotlin Coroutines & StateFlow
- **Architecture**: MVVM with Unidirectional Data Flow (UDF)
- **Dependency Injection**: Hilt

## Implementation Steps
**Total Duration:** 16m 5s

### Task_1_DataModelsAndRepositorySetup: Define core data models (User, Chat, Message, Attachment) and implement Hilt DI modules and ChatRepository with StateFlow streams for mock direct/group messaging, search, presence, and typing indicators.
- **Status:** COMPLETED
- **Updates:** Data models (User, Chat, Message, Attachment, PresenceStatus) defined. ChatRepository interface and ChatRepositoryImpl implemented with StateFlow streams and rich mock data. Hilt DI setup completed with RepositoryModule and ChatApplication. Verified assembleDebug build.
- **Acceptance Criteria:**
  - Data models for User, Chat, Message, and Attachment created
  - Repository layer with StateFlow for chats, messages, and presence implemented
  - Hilt DI bindings configured
  - Project builds successfully
- **Duration:** 8m 48s

### Task_2_ChatListAndAdaptiveScaffold: Implement Navigation 3 state-driven navigation and Compose Material Adaptive ListDetailPaneScaffold, along with Chat List UI showing conversation previews, unread badges, presence status, and search functionality.
- **Status:** COMPLETED
- **Updates:** Implemented MainAdaptiveScaffold using NavigableListDetailPaneScaffold from Compose Material 3 Adaptive. Created ChatListViewModel and ChatListScreen with profile presence selector dialog, search bar, filter tabs (All, Direct, Groups, Unread), conversation list items with avatars, presence indicators, last message preview, unread badges, typing indicators, and context menus. Verified assembleDebug build and ChatListViewModelTest passed.
- **Acceptance Criteria:**
  - Navigation 3 state management setup with ListDetailPaneScaffold for dynamic dual-pane support
  - Chat List UI displays direct and group chats, unread badges, and presence indicators
  - Search functionality filters users and conversations in real time
  - Project builds successfully
- **Duration:** 7m 17s

### Task_3_ChatDetailAndMessagingUI: Build Chat Detail screen with message bubbles, text input bar, media and document attachment picker previews, typing indicator animations, and dynamic presence updates in the chat header.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - Chat Detail screen displays message history bubbles with sender and timestamp info
  - Text input bar supports sending messages and attachment picker previews for images/documents
  - Typing indicators and online status update dynamically in chat header
  - Project builds successfully
- **StartTime:** 2026-11-12 15:46:21 PKT

### Task_4_RunAndVerify: Run and verify application stability, verify direct/group messaging, attachment previews, search, presence/typing indicators, confirm responsiveness on single and dual pane layouts, and instruct critic_agent to verify app stability and report any UI issues.
- **Status:** PENDING
- **Acceptance Criteria:**
  - build pass
  - make sure all existing tests pass
  - app does not crash
  - critic_agent verifies application stability (no crashes), confirms alignment with user requirements, and reports critical UI issues

