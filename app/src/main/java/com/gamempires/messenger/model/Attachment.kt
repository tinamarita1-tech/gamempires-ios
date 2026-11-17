package com.gamempires.messenger.model

enum class AttachmentType {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    FILE
}

data class Attachment(
    val id: String,
    val type: AttachmentType,
    val url: String,
    val name: String,
    val sizeBytes: Long = 0L,
    val mimeType: String = "",
    val durationMs: Long? = null,
    val thumbnailUrl: String? = null,
    val path: String = "",
    val shareToken: String? = null,
    val shareLink: String? = null,
    val ownerId: String? = null,
    val chatToken: String? = null
)
