package com.gamempires.messenger.data.nextcloud.dto

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

class NextcloudMessageDtoAdapter : JsonAdapter<NextcloudMessageDto>() {

    override fun fromJson(reader: JsonReader): NextcloudMessageDto? {
        when (reader.peek()) {
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                return null
            }
            JsonReader.Token.BEGIN_ARRAY -> {
                reader.beginArray()
                while (reader.hasNext()) {
                    reader.skipValue()
                }
                reader.endArray()
                return null
            }
            JsonReader.Token.BEGIN_OBJECT -> {
                // Parse object below
            }
            else -> {
                reader.skipValue()
                return null
            }
        }

        reader.beginObject()
        var id = 0L
        var token = ""
        var actorType: String? = null
        var actorId: String? = null
        var actorDisplayName: String? = null
        var timestamp = 0L
        var message = ""
        var systemMessage: String? = null
        var messageType: String? = null
        var messageParameters: Any? = null
        var reactions: Any? = null
        var parent: NextcloudMessageDto? = null

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> {
                    id = when (reader.peek()) {
                        JsonReader.Token.NUMBER -> reader.nextLong()
                        JsonReader.Token.STRING -> reader.nextString().toLongOrNull() ?: 0L
                        else -> { reader.skipValue(); 0L }
                    }
                }
                "token" -> {
                    token = when (reader.peek()) {
                        JsonReader.Token.STRING -> reader.nextString()
                        else -> { reader.skipValue(); "" }
                    }
                }
                "actorType" -> {
                    actorType = when (reader.peek()) {
                        JsonReader.Token.STRING -> reader.nextString()
                        else -> { reader.skipValue(); null }
                    }
                }
                "actorId" -> {
                    actorId = when (reader.peek()) {
                        JsonReader.Token.STRING -> reader.nextString()
                        JsonReader.Token.NUMBER -> reader.nextLong().toString()
                        else -> { reader.skipValue(); null }
                    }
                }
                "actorDisplayName" -> {
                    actorDisplayName = when (reader.peek()) {
                        JsonReader.Token.STRING -> reader.nextString()
                        else -> { reader.skipValue(); null }
                    }
                }
                "timestamp" -> {
                    timestamp = when (reader.peek()) {
                        JsonReader.Token.NUMBER -> reader.nextLong()
                        JsonReader.Token.STRING -> reader.nextString().toLongOrNull() ?: 0L
                        else -> { reader.skipValue(); 0L }
                    }
                }
                "message" -> {
                    message = when (reader.peek()) {
                        JsonReader.Token.STRING -> reader.nextString()
                        JsonReader.Token.NUMBER -> reader.nextLong().toString()
                        else -> { reader.skipValue(); "" }
                    }
                }
                "systemMessage" -> {
                    systemMessage = when (reader.peek()) {
                        JsonReader.Token.STRING -> reader.nextString().takeIf { it.isNotBlank() }
                        else -> { reader.skipValue(); null }
                    }
                }
                "messageType" -> {
                    messageType = when (reader.peek()) {
                        JsonReader.Token.STRING -> reader.nextString()
                        else -> { reader.skipValue(); null }
                    }
                }
                "messageParameters" -> {
                    messageParameters = reader.readJsonValue()
                }
                "reactions" -> {
                    reactions = reader.readJsonValue()
                }
                "parent" -> {
                    parent = when (reader.peek()) {
                        JsonReader.Token.BEGIN_OBJECT -> fromJson(reader)
                        JsonReader.Token.BEGIN_ARRAY -> {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                reader.skipValue()
                            }
                            reader.endArray()
                            null
                        }
                        else -> {
                            reader.skipValue()
                            null
                        }
                    }
                }
                else -> {
                    reader.skipValue()
                }
            }
        }
        reader.endObject()

        return NextcloudMessageDto(
            id = id,
            token = token,
            actorType = actorType,
            actorId = actorId,
            actorDisplayName = actorDisplayName,
            timestamp = timestamp,
            message = message,
            systemMessage = systemMessage,
            messageType = messageType,
            messageParameters = messageParameters,
            reactions = reactions,
            parent = parent
        )
    }

    override fun toJson(writer: JsonWriter, value: NextcloudMessageDto?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("id").value(value.id)
        writer.name("token").value(value.token)
        value.actorType?.let { writer.name("actorType").value(it) }
        value.actorId?.let { writer.name("actorId").value(it) }
        value.actorDisplayName?.let { writer.name("actorDisplayName").value(it) }
        writer.name("timestamp").value(value.timestamp)
        writer.name("message").value(value.message)
        value.systemMessage?.let { writer.name("systemMessage").value(it) }
        value.messageType?.let { writer.name("messageType").value(it) }
        if (value.parent != null) {
            writer.name("parent")
            toJson(writer, value.parent)
        }
        writer.endObject()
    }
}

class NextcloudRoomDtoAdapter(
    private val messageAdapter: JsonAdapter<NextcloudMessageDto> = NextcloudMessageDtoAdapter()
) : JsonAdapter<NextcloudRoomDto>() {

    override fun fromJson(reader: JsonReader): NextcloudRoomDto? {
        when (reader.peek()) {
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                return null
            }
            JsonReader.Token.BEGIN_OBJECT -> {
                // Parse object below
            }
            else -> {
                reader.skipValue()
                return null
            }
        }

        reader.beginObject()
        var id = 0L
        var token = ""
        var type = 1
        var name: String? = null
        var displayName = ""
        var description: String? = null
        var isFavorite = false
        var unreadMessages = 0
        var unreadMention = false
        var lastActivity = 0L
        var lastMessage: NextcloudMessageDto? = null

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> {
                    id = when (reader.peek()) {
                        JsonReader.Token.NUMBER -> reader.nextLong()
                        JsonReader.Token.STRING -> reader.nextString().toLongOrNull() ?: 0L
                        else -> { reader.skipValue(); 0L }
                    }
                }
                "token" -> {
                    token = when (reader.peek()) {
                        JsonReader.Token.STRING -> reader.nextString()
                        else -> { reader.skipValue(); "" }
                    }
                }
                "type" -> {
                    type = when (reader.peek()) {
                        JsonReader.Token.NUMBER -> reader.nextInt()
                        JsonReader.Token.STRING -> reader.nextString().toIntOrNull() ?: 1
                        else -> { reader.skipValue(); 1 }
                    }
                }
                "name" -> {
                    name = when (reader.peek()) {
                        JsonReader.Token.STRING -> reader.nextString()
                        else -> { reader.skipValue(); null }
                    }
                }
                "displayName" -> {
                    displayName = when (reader.peek()) {
                        JsonReader.Token.STRING -> reader.nextString()
                        else -> { reader.skipValue(); "" }
                    }
                }
                "description" -> {
                    description = when (reader.peek()) {
                        JsonReader.Token.STRING -> reader.nextString()
                        else -> { reader.skipValue(); null }
                    }
                }
                "isFavorite" -> {
                    isFavorite = when (reader.peek()) {
                        JsonReader.Token.BOOLEAN -> reader.nextBoolean()
                        JsonReader.Token.NUMBER -> reader.nextInt() != 0
                        JsonReader.Token.STRING -> reader.nextString().toBoolean()
                        else -> { reader.skipValue(); false }
                    }
                }
                "unreadMessages" -> {
                    unreadMessages = when (reader.peek()) {
                        JsonReader.Token.NUMBER -> reader.nextInt()
                        JsonReader.Token.STRING -> reader.nextString().toIntOrNull() ?: 0
                        else -> { reader.skipValue(); 0 }
                    }
                }
                "unreadMention" -> {
                    unreadMention = when (reader.peek()) {
                        JsonReader.Token.BOOLEAN -> reader.nextBoolean()
                        JsonReader.Token.NUMBER -> reader.nextInt() != 0
                        else -> { reader.skipValue(); false }
                    }
                }
                "lastActivity" -> {
                    lastActivity = when (reader.peek()) {
                        JsonReader.Token.NUMBER -> reader.nextLong()
                        JsonReader.Token.STRING -> reader.nextString().toLongOrNull() ?: 0L
                        else -> { reader.skipValue(); 0L }
                    }
                }
                "lastMessage" -> {
                    lastMessage = when (reader.peek()) {
                        JsonReader.Token.BEGIN_OBJECT -> messageAdapter.fromJson(reader)
                        JsonReader.Token.BEGIN_ARRAY -> {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                reader.skipValue()
                            }
                            reader.endArray()
                            null
                        }
                        else -> {
                            reader.skipValue()
                            null
                        }
                    }
                }
                else -> {
                    reader.skipValue()
                }
            }
        }
        reader.endObject()

        return NextcloudRoomDto(
            id = id,
            token = token,
            type = type,
            name = name,
            displayName = displayName,
            description = description,
            isFavorite = isFavorite,
            unreadMessages = unreadMessages,
            unreadMention = unreadMention,
            lastActivity = lastActivity,
            lastMessage = lastMessage
        )
    }

    override fun toJson(writer: JsonWriter, value: NextcloudRoomDto?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("id").value(value.id)
        writer.name("token").value(value.token)
        writer.name("type").value(value.type)
        value.name?.let { writer.name("name").value(it) }
        writer.name("displayName").value(value.displayName)
        value.description?.let { writer.name("description").value(it) }
        writer.name("isFavorite").value(value.isFavorite)
        writer.name("unreadMessages").value(value.unreadMessages)
        writer.name("unreadMention").value(value.unreadMention)
        writer.name("lastActivity").value(value.lastActivity)
        if (value.lastMessage != null) {
            writer.name("lastMessage")
            messageAdapter.toJson(writer, value.lastMessage)
        }
        writer.endObject()
    }
}
