package com.gamempires.messenger.model

data class User(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val status: PresenceStatus = PresenceStatus.OFFLINE,
    val customStatus: String? = null,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)
