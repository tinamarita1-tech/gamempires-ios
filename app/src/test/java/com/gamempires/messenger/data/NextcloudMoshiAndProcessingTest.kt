package com.gamempires.messenger.data

import com.gamempires.messenger.data.nextcloud.dto.NextcloudMessageDto
import com.gamempires.messenger.data.nextcloud.dto.NextcloudMessageDtoAdapter
import com.gamempires.messenger.data.nextcloud.dto.NextcloudRoomDto
import com.gamempires.messenger.data.nextcloud.dto.NextcloudRoomDtoAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NextcloudMoshiAndProcessingTest {

    private lateinit var moshi: Moshi
    private lateinit var messageAdapter: NextcloudMessageDtoAdapter
    private lateinit var roomAdapter: NextcloudRoomDtoAdapter

    @Before
    fun setUp() {
        messageAdapter = NextcloudMessageDtoAdapter()
        roomAdapter = NextcloudRoomDtoAdapter(messageAdapter)
        moshi = Moshi.Builder()
            .add(NextcloudMessageDto::class.java, messageAdapter)
            .add(NextcloudRoomDto::class.java, roomAdapter)
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Test
    fun testEmptyArrayParentParsesAsNull() {
        val json = """
            {
                "id": 101,
                "token": "room123",
                "message": "Hello world",
                "parent": []
            }
        """.trimIndent()

        val adapter = moshi.adapter(NextcloudMessageDto::class.java)
        val dto = adapter.fromJson(json)

        assertNotNull(dto)
        assertEquals(101L, dto?.id)
        assertEquals("Hello world", dto?.message)
        assertNull(dto?.parent)
    }

    @Test
    fun testStringIdAndBooleanSystemMessage() {
        val json = """
            {
                "id": "205",
                "token": "room456",
                "message": "Testing string id",
                "systemMessage": false,
                "reactions": [],
                "messageParameters": []
            }
        """.trimIndent()

        val adapter = moshi.adapter(NextcloudMessageDto::class.java)
        val dto = adapter.fromJson(json)

        assertNotNull(dto)
        assertEquals(205L, dto?.id)
        assertNull(dto?.systemMessage)
    }

    @Test
    fun testEmptyArrayLastMessageInRoomParsesAsNull() {
        val json = """
            {
                "id": 1,
                "token": "empty_room",
                "type": 1,
                "displayName": "Empty Room",
                "isFavorite": 1,
                "lastMessage": []
            }
        """.trimIndent()

        val adapter = moshi.adapter(NextcloudRoomDto::class.java)
        val roomDto = adapter.fromJson(json)

        assertNotNull(roomDto)
        assertEquals("empty_room", roomDto?.token)
        assertTrue(roomDto?.isFavorite == true)
        assertNull(roomDto?.lastMessage)
    }

    @Test
    fun testNestedParentMessageParsing() {
        val json = """
            {
                "id": 300,
                "token": "room_nested",
                "message": "Edited message",
                "systemMessage": "message_edited",
                "parent": {
                    "id": 299,
                    "token": "room_nested",
                    "message": "Original message modified",
                    "parent": []
                }
            }
        """.trimIndent()

        val adapter = moshi.adapter(NextcloudMessageDto::class.java)
        val dto = adapter.fromJson(json)

        assertNotNull(dto)
        assertEquals(300L, dto?.id)
        assertEquals("message_edited", dto?.systemMessage)
        assertNotNull(dto?.parent)
        assertEquals(299L, dto?.parent?.id)
        assertEquals("Original message modified", dto?.parent?.message)
        assertNull(dto?.parent?.parent)
    }

    @Test
    fun testSortMessagesOldestToNewestById() {
        val dtos = listOf(
            NextcloudMessageDto(id = 105L, message = "Fifth"),
            NextcloudMessageDto(id = 101L, message = "First"),
            NextcloudMessageDto(id = 103L, message = "Third"),
            NextcloudMessageDto(id = 102L, message = "Second"),
            NextcloudMessageDto(id = 104L, message = "Fourth")
        )

        val sorted = dtos.sortedBy { it.id }
        assertEquals(listOf(101L, 102L, 103L, 104L, 105L), sorted.map { it.id })
    }
}
