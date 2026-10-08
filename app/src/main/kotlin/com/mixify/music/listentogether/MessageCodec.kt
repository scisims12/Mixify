package com.mixify.music.listentogether

import com.google.protobuf.ByteString
import com.google.protobuf.MessageLite
import com.mixify.music.listentogether.proto.Listentogether
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

enum class MessageFormat {
    JSON,
    PROTOBUF,
}

class MessageCodec(
    var format: MessageFormat = MessageFormat.PROTOBUF,
    var compressionEnabled: Boolean = false,
) {
    companion object {
        private const val TAG = "MessageCodec"
        private const val COMPRESSION_THRESHOLD = 100

        fun detectMessageFormat(data: ByteArray): MessageFormat {
            if (data.isEmpty()) return MessageFormat.JSON
            if (data[0] == '{'.code.toByte()) return MessageFormat.JSON
            return MessageFormat.PROTOBUF
        }

        private fun JsonObject.getObject(key: String): JsonObject? {
            val element = get(key) ?: return null
            if (element is JsonNull) return null
            return element as? JsonObject
        }

        private fun JsonObject.getArray(key: String): JsonArray? {
            val element = get(key) ?: return null
            if (element is JsonNull) return null
            return element as? JsonArray
        }

        private fun JsonObject.getPrimitive(key: String): JsonPrimitive? {
            val element = get(key) ?: return null
            if (element is JsonNull) return null
            return element as? JsonPrimitive
        }

        private fun JsonObject.getString(key: String): String? {
            val primitive = getPrimitive(key) ?: return null
            return primitive.content
        }

        private fun JsonObject.getLong(key: String): Long? {
            val primitive = getPrimitive(key) ?: return null
            return primitive.longOrNull
        }

        private fun JsonObject.getFloat(key: String): Float? {
            val primitive = getPrimitive(key) ?: return null
            return primitive.floatOrNull
        }

        private fun JsonObject.getBoolean(key: String): Boolean? {
            val primitive = getPrimitive(key) ?: return null
            return primitive.booleanOrNull
        }
    }

    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

    fun encode(
        messageType: String,
        payload: MessageLite?,
    ): ByteArray {
        return if (format == MessageFormat.PROTOBUF) {
            encodeProtobuf(messageType, payload)
        } else {
            encodeJson(messageType, payload)
        }
    }

    fun decode(
        data: ByteArray,
        detectedFormat: MessageFormat = detectMessageFormat(data),
    ): Pair<String, ByteArray> {
        return if (detectedFormat == MessageFormat.PROTOBUF) {
            decodeProtobuf(data)
        } else {
            decodeJson(data)
        }
    }

    private fun encodeProtobuf(
        messageType: String,
        payload: MessageLite?,
    ): ByteArray {
        var payloadBytes = payload?.toByteArray() ?: byteArrayOf()
        var compressed = false
        if (compressionEnabled && payloadBytes.size > COMPRESSION_THRESHOLD) {
            val compressedBytes = compress(payloadBytes)
            if (compressedBytes.size < payloadBytes.size) {
                payloadBytes = compressedBytes
                compressed = true
            }
        }

        return Listentogether.Envelope
            .newBuilder()
            .setType(messageType)
            .setPayload(ByteString.copyFrom(payloadBytes))
            .setCompressed(compressed)
            .build()
            .toByteArray()
    }

    private fun decodeProtobuf(data: ByteArray): Pair<String, ByteArray> {
        val envelope = Listentogether.Envelope.parseFrom(data)
        val payload = envelope.payload.toByteArray()
        return envelope.type to if (envelope.compressed) decompress(payload) ?: payload else payload
    }

    private fun encodeJson(
        messageType: String,
        payload: MessageLite?,
    ): ByteArray {
        val jsonPayloadString =
            if (payload != null) {
                payloadToJsonString(payload)
            } else {
                null
            }

        val msgJson =
            if (jsonPayloadString != null) {
                """{"type":"$messageType","payload":$jsonPayloadString}"""
            } else {
                """{"type":"$messageType","payload":null}"""
            }

        return msgJson.toByteArray(Charsets.UTF_8)
    }

    private fun decodeJson(data: ByteArray): Pair<String, ByteArray> {
        val str = data.toString(Charsets.UTF_8)
        try {
            val element = json.parseToJsonElement(str)
            if (element !is JsonObject) return "" to byteArrayOf()
            val type = element.getString("type").orEmpty()
            val payloadElement = element["payload"]
            val payloadBytes =
                if (payloadElement != null && payloadElement !is JsonNull) {
                    payloadElement.toString().toByteArray(Charsets.UTF_8)
                } else {
                    byteArrayOf()
                }
            return type to payloadBytes
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error decoding JSON message: $str")
            return "" to byteArrayOf()
        }
    }

    fun decodePayload(
        messageType: String,
        payloadBytes: ByteArray,
        detectedFormat: MessageFormat = MessageFormat.PROTOBUF,
    ): MessageLite? {
        if (payloadBytes.isEmpty()) return null
        return if (detectedFormat == MessageFormat.PROTOBUF) {
            decodeProtobufPayload(messageType, payloadBytes)
        } else {
            decodeJsonPayload(messageType, payloadBytes)
        }
    }

    private fun decodeProtobufPayload(
        messageType: String,
        payloadBytes: ByteArray,
    ): MessageLite? {
        return when (messageType) {
            MessageTypes.ROOM_CREATED -> Listentogether.RoomCreatedPayload.parseFrom(payloadBytes)
            MessageTypes.JOIN_REQUEST -> Listentogether.JoinRequestPayload.parseFrom(payloadBytes)
            MessageTypes.JOIN_APPROVED -> Listentogether.JoinApprovedPayload.parseFrom(payloadBytes)
            MessageTypes.JOIN_REJECTED -> Listentogether.JoinRejectedPayload.parseFrom(payloadBytes)
            MessageTypes.USER_JOINED -> Listentogether.UserJoinedPayload.parseFrom(payloadBytes)
            MessageTypes.USER_LEFT -> Listentogether.UserLeftPayload.parseFrom(payloadBytes)
            MessageTypes.SYNC_PLAYBACK -> Listentogether.PlaybackActionPayload.parseFrom(payloadBytes)
            MessageTypes.BUFFER_WAIT -> Listentogether.BufferWaitPayload.parseFrom(payloadBytes)
            MessageTypes.BUFFER_COMPLETE -> Listentogether.BufferCompletePayload.parseFrom(payloadBytes)
            MessageTypes.ERROR -> Listentogether.ErrorPayload.parseFrom(payloadBytes)
            MessageTypes.HOST_CHANGED -> Listentogether.HostChangedPayload.parseFrom(payloadBytes)
            MessageTypes.KICKED -> Listentogether.KickedPayload.parseFrom(payloadBytes)
            MessageTypes.SYNC_STATE -> Listentogether.SyncStatePayload.parseFrom(payloadBytes)
            MessageTypes.PONG -> Listentogether.PongPayload.parseFrom(payloadBytes)
            MessageTypes.RECONNECTED -> Listentogether.ReconnectedPayload.parseFrom(payloadBytes)
            MessageTypes.USER_RECONNECTED -> Listentogether.UserReconnectedPayload.parseFrom(payloadBytes)
            MessageTypes.USER_DISCONNECTED -> Listentogether.UserDisconnectedPayload.parseFrom(payloadBytes)
            MessageTypes.SUGGESTION_RECEIVED -> Listentogether.SuggestionReceivedPayload.parseFrom(payloadBytes)
            MessageTypes.SUGGESTION_APPROVED -> Listentogether.SuggestionApprovedPayload.parseFrom(payloadBytes)
            MessageTypes.SUGGESTION_REJECTED -> Listentogether.SuggestionRejectedPayload.parseFrom(payloadBytes)
            else -> null
        }
    }

    private fun decodeJsonPayload(
        messageType: String,
        payloadBytes: ByteArray,
    ): MessageLite? {
        val str = payloadBytes.toString(Charsets.UTF_8)
        if (str.isEmpty() || str == "null") return null
        return try {
            val root = json.parseToJsonElement(str).jsonObject
            when (messageType) {
                MessageTypes.ROOM_CREATED -> {
                    Listentogether.RoomCreatedPayload
                        .newBuilder()
                        .setRoomCode(root.getString("room_code").orEmpty())
                        .setUserId(root.getString("user_id").orEmpty())
                        .setSessionToken(root.getString("session_token").orEmpty())
                        .build()
                }

                MessageTypes.JOIN_REQUEST -> {
                    Listentogether.JoinRequestPayload
                        .newBuilder()
                        .setUserId(root.getString("user_id").orEmpty())
                        .setUsername(root.getString("username").orEmpty())
                        .build()
                }

                MessageTypes.JOIN_APPROVED -> {
                    val stateObj = root.getObject("state")
                    val state = stateObj?.let { parseRoomStateFromJson(it) }
                    val builder =
                        Listentogether.JoinApprovedPayload
                            .newBuilder()
                            .setRoomCode(root.getString("room_code").orEmpty())
                            .setUserId(root.getString("user_id").orEmpty())
                            .setSessionToken(root.getString("session_token").orEmpty())
                    if (state != null) builder.setState(state)
                    builder.build()
                }

                MessageTypes.JOIN_REJECTED -> {
                    Listentogether.JoinRejectedPayload
                        .newBuilder()
                        .setReason(root.getString("reason").orEmpty())
                        .build()
                }

                MessageTypes.USER_JOINED -> {
                    Listentogether.UserJoinedPayload
                        .newBuilder()
                        .setUserId(root.getString("user_id").orEmpty())
                        .setUsername(root.getString("username").orEmpty())
                        .build()
                }

                MessageTypes.USER_LEFT -> {
                    Listentogether.UserLeftPayload
                        .newBuilder()
                        .setUserId(root.getString("user_id").orEmpty())
                        .setUsername(root.getString("username").orEmpty())
                        .build()
                }

                MessageTypes.SYNC_PLAYBACK -> {
                    val builder = Listentogether.PlaybackActionPayload.newBuilder()
                    root.getString("action")?.let { builder.setAction(it) }
                    root.getString("track_id")?.let { builder.setTrackId(it) }
                    root.getLong("position")?.let { builder.setPosition(it) }
                    root.getObject("track_info")?.let { builder.setTrackInfo(parseTrackInfoFromJson(it)) }
                    root.getBoolean("insert_next")?.let { builder.setInsertNext(it) }
                    root.getArray("queue")?.forEach {
                        (it as? JsonObject)?.let { trackObj ->
                            builder.addQueue(parseTrackInfoFromJson(trackObj))
                        }
                    }
                    root.getString("queue_title")?.let { builder.setQueueTitle(it) }
                    root.getFloat("volume")?.let { builder.setVolume(it) }
                    root.getLong("server_time")?.let { builder.setServerTime(it) }
                    root.getLong("revision")?.let { builder.setRevision(it) }
                    root.getLong("captured_at_server_time")?.let { builder.setCapturedAtServerTime(it) }
                    builder.build()
                }

                MessageTypes.BUFFER_WAIT -> {
                    val builder = Listentogether.BufferWaitPayload.newBuilder()
                    root.getString("track_id")?.let { builder.setTrackId(it) }
                    root.getArray("waiting_for")?.forEach {
                        val prim = it as? JsonPrimitive
                        prim?.content?.let { userId -> builder.addWaitingFor(userId) }
                    }
                    builder.build()
                }

                MessageTypes.BUFFER_COMPLETE -> {
                    Listentogether.BufferCompletePayload
                        .newBuilder()
                        .setTrackId(root.getString("track_id").orEmpty())
                        .build()
                }

                MessageTypes.ERROR -> {
                    Listentogether.ErrorPayload
                        .newBuilder()
                        .setCode(root.getString("code").orEmpty())
                        .setMessage(root.getString("message").orEmpty())
                        .build()
                }

                MessageTypes.HOST_CHANGED -> {
                    Listentogether.HostChangedPayload
                        .newBuilder()
                        .setNewHostId(root.getString("new_host_id").orEmpty())
                        .setNewHostName(root.getString("new_host_name").orEmpty())
                        .build()
                }

                MessageTypes.KICKED -> {
                    Listentogether.KickedPayload
                        .newBuilder()
                        .setReason(root.getString("reason").orEmpty())
                        .build()
                }

                MessageTypes.SYNC_STATE -> {
                    val builder = Listentogether.SyncStatePayload.newBuilder()
                    root.getObject("current_track")?.let { builder.setCurrentTrack(parseTrackInfoFromJson(it)) }
                    root.getBoolean("is_playing")?.let { builder.setIsPlaying(it) }
                    root.getLong("position")?.let { builder.setPosition(it) }
                    root.getLong("last_update")?.let { builder.setLastUpdate(it) }
                    root.getArray("queue")?.forEach {
                        (it as? JsonObject)?.let { trackObj ->
                            builder.addQueue(parseTrackInfoFromJson(trackObj))
                        }
                    }
                    root.getFloat("volume")?.let { builder.setVolume(it) }
                    root.getLong("revision")?.let { builder.setRevision(it) }
                    builder.build()
                }

                MessageTypes.PONG -> {
                    val builder = Listentogether.PongPayload.newBuilder()
                    root.getLong("client_time")?.let { builder.setClientTime(it) }
                    root.getLong("server_receive_time")?.let { builder.setServerReceiveTime(it) }
                    root.getLong("server_send_time")?.let { builder.setServerSendTime(it) }
                    root.getLong("sequence")?.let { builder.setSequence(it) }
                    builder.build()
                }

                MessageTypes.RECONNECTED -> {
                    val stateObj = root.getObject("state")
                    val state = stateObj?.let { parseRoomStateFromJson(it) }
                    val builder =
                        Listentogether.ReconnectedPayload
                            .newBuilder()
                            .setRoomCode(root.getString("room_code").orEmpty())
                            .setUserId(root.getString("user_id").orEmpty())
                            .setIsHost(root.getBoolean("is_host") ?: false)
                    if (state != null) builder.setState(state)
                    builder.build()
                }

                MessageTypes.USER_RECONNECTED -> {
                    Listentogether.UserReconnectedPayload
                        .newBuilder()
                        .setUserId(root.getString("user_id").orEmpty())
                        .setUsername(root.getString("username").orEmpty())
                        .build()
                }

                MessageTypes.USER_DISCONNECTED -> {
                    Listentogether.UserDisconnectedPayload
                        .newBuilder()
                        .setUserId(root.getString("user_id").orEmpty())
                        .setUsername(root.getString("username").orEmpty())
                        .build()
                }

                MessageTypes.SUGGESTION_RECEIVED -> {
                    val builder = Listentogether.SuggestionReceivedPayload.newBuilder()
                    root.getString("suggestion_id")?.let { builder.setSuggestionId(it) }
                    root.getString("from_user_id")?.let { builder.setFromUserId(it) }
                    root.getString("from_username")?.let { builder.setFromUsername(it) }
                    root.getObject("track_info")?.let { builder.setTrackInfo(parseTrackInfoFromJson(it)) }
                    builder.build()
                }

                MessageTypes.SUGGESTION_APPROVED -> {
                    val builder = Listentogether.SuggestionApprovedPayload.newBuilder()
                    root.getString("suggestion_id")?.let { builder.setSuggestionId(it) }
                    root.getObject("track_info")?.let { builder.setTrackInfo(parseTrackInfoFromJson(it)) }
                    builder.build()
                }

                MessageTypes.SUGGESTION_REJECTED -> {
                    val builder = Listentogether.SuggestionRejectedPayload.newBuilder()
                    root.getString("suggestion_id")?.let { builder.setSuggestionId(it) }
                    root.getString("reason")?.let { builder.setReason(it) }
                    builder.build()
                }

                else -> null
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to parse JSON payload for $messageType: $str")
            null
        }
    }

    private fun parseTrackInfoFromJson(obj: JsonObject): Listentogether.TrackInfo {
        val builder = Listentogether.TrackInfo.newBuilder()
        obj.getString("id")?.let { builder.setId(it) }
        obj.getString("title")?.let { builder.setTitle(it) }
        obj.getString("artist")?.let { builder.setArtist(it) }
        obj.getString("album")?.let { builder.setAlbum(it) }
        obj.getLong("duration")?.let { builder.setDuration(it) }
        obj.getString("thumbnail")?.let { builder.setThumbnail(it) }
        obj.getString("suggested_by")?.let { builder.setSuggestedBy(it) }
        return builder.build()
    }

    private fun parseUserInfoFromJson(obj: JsonObject): Listentogether.UserInfo {
        val builder = Listentogether.UserInfo.newBuilder()
        obj.getString("user_id")?.let { builder.setUserId(it) }
        obj.getString("username")?.let { builder.setUsername(it) }
        obj.getBoolean("is_host")?.let { builder.setIsHost(it) }
        obj.getBoolean("is_connected")?.let { builder.setIsConnected(it) }
        return builder.build()
    }

    private fun parseRoomStateFromJson(obj: JsonObject): Listentogether.RoomState {
        val builder = Listentogether.RoomState.newBuilder()
        obj.getString("room_code")?.let { builder.setRoomCode(it) }
        obj.getString("host_id")?.let { builder.setHostId(it) }
        obj.getArray("users")?.forEach {
            (it as? JsonObject)?.let { userObj ->
                builder.addUsers(parseUserInfoFromJson(userObj))
            }
        }
        obj.getObject("current_track")?.let {
            builder.setCurrentTrack(parseTrackInfoFromJson(it))
        }
        obj.getBoolean("is_playing")?.let { builder.setIsPlaying(it) }
        obj.getLong("position")?.let { builder.setPosition(it) }
        obj.getLong("last_update")?.let { builder.setLastUpdate(it) }
        obj.getFloat("volume")?.let { builder.setVolume(it) }
        obj.getArray("queue")?.forEach {
            (it as? JsonObject)?.let { trackObj ->
                builder.addQueue(parseTrackInfoFromJson(trackObj))
            }
        }
        obj.getLong("revision")?.let { builder.setRevision(it) }
        return builder.build()
    }

    private fun payloadToJsonString(payload: MessageLite): String {
        return when (payload) {
            is Listentogether.CreateRoomPayload -> {
                """{"username":"${escapeJson(payload.username)}","avatar_index":0}"""
            }

            is Listentogether.JoinRoomPayload -> {
                """{"room_code":"${escapeJson(payload.roomCode)}","username":"${escapeJson(payload.username)}","avatar_index":0}"""
            }

            is Listentogether.ApproveJoinPayload -> {
                """{"user_id":"${escapeJson(payload.userId)}"}"""
            }

            is Listentogether.RejectJoinPayload -> {
                """{"user_id":"${escapeJson(payload.userId)}","reason":"${escapeJson(payload.reason)}"}"""
            }

            is Listentogether.PlaybackActionPayload -> {
                val sb = StringBuilder("{")
                sb.append(""""action":"${escapeJson(payload.action)}"""")
                if (payload.trackId.isNotEmpty()) sb.append(""","track_id":"${escapeJson(payload.trackId)}"""")
                sb.append(""","position":${payload.position}""")
                if (payload.hasTrackInfo()) sb.append(""","track_info":${trackInfoToJsonString(payload.trackInfo)}""")
                sb.append(""","insert_next":${payload.insertNext}""")
                if (payload.queueCount > 0) {
                    val queueJson = payload.queueList.joinToString(",", "[", "]") { trackInfoToJsonString(it) }
                    sb.append(""","queue":$queueJson""")
                }
                if (payload.queueTitle.isNotEmpty()) sb.append(""","queue_title":"${escapeJson(payload.queueTitle)}"""")
                sb.append(""","volume":${payload.volume}""")
                if (payload.serverTime > 0) sb.append(""","server_time":${payload.serverTime}""")
                if (payload.revision > 0) sb.append(""","revision":${payload.revision}""")
                sb.append("}")
                sb.toString()
            }

            is Listentogether.BufferReadyPayload -> {
                """{"track_id":"${escapeJson(payload.trackId)}"}"""
            }

            is Listentogether.KickUserPayload -> {
                """{"user_id":"${escapeJson(payload.userId)}","reason":"${escapeJson(payload.reason)}"}"""
            }

            is Listentogether.TransferHostPayload -> {
                """{"new_host_id":"${escapeJson(payload.newHostId)}"}"""
            }

            is Listentogether.SuggestTrackPayload -> {
                """{"track_info":${trackInfoToJsonString(payload.trackInfo)}}"""
            }

            is Listentogether.ApproveSuggestionPayload -> {
                """{"suggestion_id":"${escapeJson(payload.suggestionId)}"}"""
            }

            is Listentogether.RejectSuggestionPayload -> {
                """{"suggestion_id":"${escapeJson(payload.suggestionId)}","reason":"${escapeJson(payload.reason)}"}"""
            }

            is Listentogether.ReconnectPayload -> {
                """{"session_token":"${escapeJson(payload.sessionToken)}"}"""
            }

            is Listentogether.PingPayload -> "{}"

            else -> "{}"
        }
    }

    private fun trackInfoToJsonString(track: Listentogether.TrackInfo): String {
        val sb = StringBuilder("{")
        sb.append(""""id":"${escapeJson(track.id)}"""")
        sb.append(""","title":"${escapeJson(track.title)}"""")
        sb.append(""","artist":"${escapeJson(track.artist)}"""")
        if (track.album.isNotEmpty()) sb.append(""","album":"${escapeJson(track.album)}"""")
        sb.append(""","duration":${track.duration}""")
        if (track.thumbnail.isNotEmpty()) sb.append(""","thumbnail":"${escapeJson(track.thumbnail)}"""")
        if (track.suggestedBy.isNotEmpty()) sb.append(""","suggested_by":"${escapeJson(track.suggestedBy)}"""")
        sb.append("}")
        return sb.toString()
    }

    private fun escapeJson(s: String): String =
        s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

    private fun compress(data: ByteArray): ByteArray =
        ByteArrayOutputStream().use { output ->
            GZIPOutputStream(output).use { it.write(data) }
            output.toByteArray()
        }

    private fun decompress(data: ByteArray): ByteArray? =
        try {
            GZIPInputStream(ByteArrayInputStream(data)).use { it.readBytes() }
        } catch (error: Exception) {
            Timber.tag(TAG).e(error, "Failed to decompress Listen Together payload")
            null
        }
}
