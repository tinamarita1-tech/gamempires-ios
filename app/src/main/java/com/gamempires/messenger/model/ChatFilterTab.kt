package com.gamempires.messenger.model

enum class ChatFilterTab(val label: String) {
    HOME("Home"),   // 1.0.11: Home tab first, opens on app start
    ALL("All"),
    DIRECT("Direct"),
    GROUPS("Groups"),
    UNREAD("Unread")
}
