package com.gamempires.messenger.data.nextcloud.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OcsResponse<T>(
    @Json(name = "ocs") val ocs: OcsPayload<T>
)

@JsonClass(generateAdapter = true)
data class OcsPayload<T>(
    @Json(name = "meta") val meta: OcsMeta,
    @Json(name = "data") val data: T
)

@JsonClass(generateAdapter = true)
data class OcsMeta(
    @Json(name = "status") val status: String? = null,
    @Json(name = "statuscode") val statuscode: Int = 200,
    @Json(name = "message") val message: String? = null
)

// Login Flow v2 DTOs
@JsonClass(generateAdapter = true)
data class LoginV2InitResponse(
    @Json(name = "poll") val poll: LoginV2Poll,
    @Json(name = "login") val login: String
)

@JsonClass(generateAdapter = true)
data class LoginV2Poll(
    @Json(name = "token") val token: String,
    @Json(name = "endpoint") val endpoint: String
)

@JsonClass(generateAdapter = true)
data class LoginV2PollSuccessResponse(
    @Json(name = "server") val server: String,
    @Json(name = "loginName") val loginName: String,
    @Json(name = "appPassword") val appPassword: String
)

// Nextcloud Talk Room (v4) DTO
@JsonClass(generateAdapter = true)
data class NextcloudRoomDto(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "token") val token: String = "",
    @Json(name = "type") val type: Int = 1,
    @Json(name = "avatarVersion") val avatarVersion: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "displayName") val displayName: String = "",
    @Json(name = "description") val description: String? = null,
    @Json(name = "isFavorite") val isFavorite: Boolean = false,
    @Json(name = "unreadMessages") val unreadMessages: Int = 0,
    @Json(name = "unreadMention") val unreadMention: Boolean = false,
    @Json(name = "lastActivity") val lastActivity: Long = 0L,
    @Json(name = "lastMessage") val lastMessage: NextcloudMessageDto? = null
)

// Nextcloud Talk Chat Message (v1) DTO
@JsonClass(generateAdapter = true)
data class NextcloudMessageDto(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "token") val token: String = "",
    @Json(name = "actorType") val actorType: String? = null,
    @Json(name = "actorId") val actorId: String? = null,
    @Json(name = "actorDisplayName") val actorDisplayName: String? = null,
    @Json(name = "timestamp") val timestamp: Long = 0L,
    @Json(name = "message") val message: String = "",
    @Json(name = "systemMessage") val systemMessage: String? = null,
    @Json(name = "messageType") val messageType: String? = null,
    @Json(name = "messageParameters") val messageParameters: Any? = null,
    @Json(name = "reactions") val reactions: Any? = null,
    @Json(name = "parent") val parent: NextcloudMessageDto? = null
)

// Authenticated User OCS DTO
@JsonClass(generateAdapter = true)
data class NextcloudUserDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "displayname") val displayname: String? = null,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "email") val email: String? = null
)

// Autocomplete User Search DTO
@JsonClass(generateAdapter = true)
data class NextcloudAutocompleteUserDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "label") val label: String = "",
    @Json(name = "icon") val icon: String? = null,
    @Json(name = "subline") val subline: String? = null,
    @Json(name = "source") val source: String? = null
)

// User Status DTO (OCS user_status API v1)
@JsonClass(generateAdapter = true)
data class NextcloudUserStatusDto(
    @Json(name = "userId") val userId: String = "",
    @Json(name = "status") val status: String? = null,
    @Json(name = "statusType") val statusType: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "icon") val icon: String? = null
)

// Room Participant DTO (Nextcloud Talk spreed API v4)
@JsonClass(generateAdapter = true)
data class NextcloudParticipantDto(
    @Json(name = "actorType") val actorType: String? = null,
    @Json(name = "actorId") val actorId: String = "",
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "lastPing") val lastPing: Long = 0L,
    @Json(name = "status") val status: String? = null
)


