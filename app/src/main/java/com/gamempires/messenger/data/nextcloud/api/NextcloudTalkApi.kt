package com.gamempires.messenger.data.nextcloud.api

import com.gamempires.messenger.data.nextcloud.dto.LoginV2InitResponse
import com.gamempires.messenger.data.nextcloud.dto.LoginV2PollSuccessResponse
import com.gamempires.messenger.data.nextcloud.dto.NextcloudMessageDto
import com.gamempires.messenger.data.nextcloud.dto.NextcloudRoomDto
import com.gamempires.messenger.data.nextcloud.dto.OcsResponse
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface NextcloudTalkApi {

    // Login Flow v2
    @POST("index.php/login/v2")
    suspend fun initLoginV2(): LoginV2InitResponse

    @FormUrlEncoded
    @POST
    suspend fun pollLoginV2(
        @Url pollUrl: String,
        @Field("token") token: String
    ): Response<LoginV2PollSuccessResponse>

    // Conversations / Rooms (API v4)
    @GET("ocs/v2.php/apps/spreed/api/v4/room")
    suspend fun getRooms(): OcsResponse<List<NextcloudRoomDto>>

    // Chat Message History (API v1)
    @GET("ocs/v2.php/apps/spreed/api/v1/chat/{token}")
    suspend fun getMessageHistory(
        @Path("token") token: String,
        @Query("lookIntoFuture") lookIntoFuture: Int = 0,
        @Query("limit") limit: Int = 100
    ): Response<OcsResponse<List<NextcloudMessageDto>>>

    // Real-Time New Messages Long Polling (API v1)
    // Returns 304 Not Modified when there are no new messages within timeout
    @GET("ocs/v2.php/apps/spreed/api/v1/chat/{token}")
    suspend fun pollNewMessages(
        @Path("token") token: String,
        @Query("lookIntoFuture") lookIntoFuture: Int = 1,
        @Query("lastKnownMessageId") lastKnownMessageId: Long,
        @Query("timeout") timeout: Int = 30
    ): Response<OcsResponse<List<NextcloudMessageDto>>>

    // Send Chat Message (API v1) with optional replyTo
    @FormUrlEncoded
    @POST("ocs/v2.php/apps/spreed/api/v1/chat/{token}")
    suspend fun sendMessage(
        @Path("token") token: String,
        @Field("message") message: String,
        @Field("replyTo") replyTo: String? = null
    ): OcsResponse<NextcloudMessageDto>

    // Edit Chat Message (API v1)
    @FormUrlEncoded
    @retrofit2.http.PUT("ocs/v2.php/apps/spreed/api/v1/chat/{token}/{messageId}")
    suspend fun editMessage(
        @Path("token") token: String,
        @Path("messageId") messageId: String,
        @Field("message") message: String
    ): Response<OcsResponse<NextcloudMessageDto>>

    // Delete Chat Message (API v1)
    @retrofit2.http.DELETE("ocs/v2.php/apps/spreed/api/v1/chat/{token}/{messageId}")
    suspend fun deleteMessage(
        @Path("token") token: String,
        @Path("messageId") messageId: String
    ): Response<OcsResponse<Any>>

    // React with Emoji to Message (API v1)
    @FormUrlEncoded
    @POST("ocs/v2.php/apps/spreed/api/v1/reaction/{token}/{messageId}")
    suspend fun reactToMessage(
        @Path("token") token: String,
        @Path("messageId") messageId: String,
        @Field("reaction") reaction: String
    ): Response<OcsResponse<Any>>

    // Delete App Password on Logout
    @retrofit2.http.DELETE("ocs/v2.php/core/apppassword")
    suspend fun deleteAppPassword(): Response<OcsResponse<Any>>

    // Autocomplete User Search
    @GET("ocs/v2.php/core/autocomplete/get")
    suspend fun searchUsers(
        @Query("search") search: String,
        @Query("itemType") itemType: String = "call",
        @Query("itemId") itemId: String = "new",
        @Query("shareTypes[]") shareTypes: Int = 0
    ): Response<OcsResponse<List<com.gamempires.messenger.data.nextcloud.dto.NextcloudAutocompleteUserDto>>>

    // Create One-to-One Room
    @FormUrlEncoded
    @POST("ocs/v2.php/apps/spreed/api/v4/room")
    suspend fun createOneToOneRoom(
        @Field("roomType") roomType: Int = 1,
        @Field("invite") invite: String
    ): Response<OcsResponse<NextcloudRoomDto>>

    // Avatar check: header X-NC-IsCustomAvatar is 1 for an uploaded photo, 0 for the server's automatic letter picture
    @HEAD("index.php/avatar/{user}/128")
    suspend fun avatarInfo(@Path("user") user: String): Response<Void>

    // Authenticated User Profile (OCS Cloud API)
    @GET("ocs/v2.php/cloud/user")
    suspend fun getCurrentUser(): Response<OcsResponse<com.gamempires.messenger.data.nextcloud.dto.NextcloudUserDto>>

    // Upload file via WebDAV PUT to Talk folder
    @PUT("remote.php/dav/files/{userId}/Talk/{fileName}")
    suspend fun uploadFileWebDav(
        @Path("userId") userId: String,
        @Path("fileName") fileName: String,
        @Body fileBody: RequestBody
    ): Response<ResponseBody>

    // WebDAV MKCOL fallback to ensure /Talk folder exists
    @retrofit2.http.HTTP(method = "MKCOL", path = "remote.php/dav/files/{userId}/Talk", hasBody = false)
    suspend fun createTalkFolder(
        @Path("userId") userId: String
    ): Response<ResponseBody>

    // Share File into Talk Room (API v1)
    @FormUrlEncoded
    @POST("ocs/v2.php/apps/files_sharing/api/v1/shares")
    suspend fun shareFileToRoom(
        @Field("path") path: String,
        @Field("shareType") shareType: Int = 10,
        @Field("shareWith") shareWith: String
    ): Response<OcsResponse<Any>>

    // User Status (OCS user_status API v1)
    @GET("ocs/v2.php/apps/user_status/api/v1/statuses/{userId}")
    suspend fun getUserStatus(
        @Path("userId") userId: String
    ): Response<OcsResponse<com.gamempires.messenger.data.nextcloud.dto.NextcloudUserStatusDto>>

    @FormUrlEncoded
    @PUT("ocs/v2.php/apps/user_status/api/v1/user_status/status")
    suspend fun setUserStatus(
        @Field("statusType") statusType: String
    ): Response<OcsResponse<com.gamempires.messenger.data.nextcloud.dto.NextcloudUserStatusDto>>

    @FormUrlEncoded
    @POST("ocs/v2.php/apps/dav/api/v1/direct")
    suspend fun getDirectDownload(
        @Field("fileId") fileId: String
    ): Response<OcsResponse<Map<String, String>>>

    // Room Participants (Nextcloud Talk spreed API v4)
    @GET("ocs/v2.php/apps/spreed/api/v4/room/{token}/participants")
    suspend fun getRoomParticipants(
        @Path("token") token: String
    ): Response<OcsResponse<List<com.gamempires.messenger.data.nextcloud.dto.NextcloudParticipantDto>>>
}


