package com.mixify.music.listentogether

import com.google.protobuf.ByteString
import com.mixify.music.listentogether.proto.Listentogether
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageCodecTest {
    private val protobufCodec = MessageCodec(format = MessageFormat.PROTOBUF, compressionEnabled = true)
    private val jsonCodec = MessageCodec(format = MessageFormat.JSON, compressionEnabled = false)

    @Test
    fun `playback timing fields survive a protobuf round trip`() {
        val action =
            PlaybackActionPayload(
                action = PlaybackActions.PLAY,
                trackId = "track",
                position = 1_234L,
                serverTime = 9_000L,
                revision = 12L,
                capturedAtServerTime = 8_950L,
            )

        val (type, payload) = protobufCodec.decode(protobufCodec.encode(MessageTypes.PLAYBACK_ACTION, action))
        val decoded = protobufCodec.decodePayload(MessageTypes.SYNC_PLAYBACK, payload, MessageFormat.PROTOBUF) as PlaybackActionPayload

        assertEquals(MessageTypes.PLAYBACK_ACTION, type)
        assertEquals(action.action, decoded.action)
        assertEquals(action.trackId, decoded.trackId)
        assertEquals(action.position, decoded.position)
        assertEquals(action.serverTime, decoded.serverTime)
        assertEquals(action.revision, decoded.revision)
        assertEquals(action.capturedAtServerTime, decoded.capturedAtServerTime)
    }

    @Test
    fun `timestamped ping is encoded and pong is decoded in protobuf`() {
        val ping = PingPayload(clientTime = 1_000L, sequence = 3L)
        val (_, pingBytes) = protobufCodec.decode(protobufCodec.encode(MessageTypes.PING, ping))
        val encodedPing = Listentogether.PingPayload.parseFrom(pingBytes)
        assertEquals(1_000L, encodedPing.clientTime)
        assertEquals(3L, encodedPing.sequence)

        val pong =
            Listentogether.PongPayload
                .newBuilder()
                .setClientTime(1_000L)
                .setServerReceiveTime(10_000L)
                .setServerSendTime(10_001L)
                .setSequence(3L)
                .build()
        val envelope =
            Listentogether.Envelope
                .newBuilder()
                .setType(MessageTypes.PONG)
                .setPayload(ByteString.copyFrom(pong.toByteArray()))
                .build()
        val (type, pongBytes) = protobufCodec.decode(envelope.toByteArray())
        val decoded = protobufCodec.decodePayload(type, pongBytes, MessageFormat.PROTOBUF) as PongPayload

        assertEquals(pong, decoded)
        assertTrue(decoded.serverSendTime >= decoded.serverReceiveTime)
    }

    @Test
    fun `json room_created message decodes to room created payload`() {
        val jsonMsg = """{"type":"room_created","payload":{"room_code":"J2EYEG","user_id":"uuid-1","session_token":"token-1"}}"""
        val (type, payloadBytes) = jsonCodec.decode(jsonMsg.toByteArray(Charsets.UTF_8))
        val payload = jsonCodec.decodePayload(type, payloadBytes, MessageFormat.JSON) as Listentogether.RoomCreatedPayload

        assertEquals("room_created", type)
        assertEquals("J2EYEG", payload.roomCode)
        assertEquals("uuid-1", payload.userId)
        assertEquals("token-1", payload.sessionToken)
    }

    @Test
    fun `json join_request message decodes correctly`() {
        val jsonMsg = """{"type":"join_request","payload":{"user_id":"uuid-2","username":"RahulGuest","avatar_index":0}}"""
        val (type, payloadBytes) = jsonCodec.decode(jsonMsg.toByteArray(Charsets.UTF_8))
        val payload = jsonCodec.decodePayload(type, payloadBytes, MessageFormat.JSON) as Listentogether.JoinRequestPayload

        assertEquals("join_request", type)
        assertEquals("uuid-2", payload.userId)
        assertEquals("RahulGuest", payload.username)
    }

    @Test
    fun `json join_approved message with null current_track decodes safely`() {
        val jsonMsg = """{"type":"join_approved","payload":{"room_code":"J2EYEG","user_id":"uuid-2","session_token":"token-2","state":{"room_code":"J2EYEG","host_id":"uuid-1","users":[{"user_id":"uuid-1","username":"AmanHost","is_host":true,"is_connected":true}],"current_track":null,"is_playing":false,"position":0,"last_update":1790762281551,"volume":1,"queue":[]}}}"""
        val (type, payloadBytes) = jsonCodec.decode(jsonMsg.toByteArray(Charsets.UTF_8))
        val payload = jsonCodec.decodePayload(type, payloadBytes, MessageFormat.JSON) as Listentogether.JoinApprovedPayload

        assertEquals("join_approved", type)
        assertEquals("J2EYEG", payload.roomCode)
        assertEquals("uuid-2", payload.userId)
        assertEquals("token-2", payload.sessionToken)
        assertTrue(payload.hasState())
        assertFalse(payload.state.hasCurrentTrack())
        assertEquals("uuid-1", payload.state.hostId)
        assertEquals(1, payload.state.usersCount)
    }

    @Test
    fun `json user_joined message decodes correctly`() {
        val jsonMsg = """{"type":"user_joined","payload":{"user_id":"uuid-2","username":"RahulGuest","avatar_index":0}}"""
        val (type, payloadBytes) = jsonCodec.decode(jsonMsg.toByteArray(Charsets.UTF_8))
        val payload = jsonCodec.decodePayload(type, payloadBytes, MessageFormat.JSON) as Listentogether.UserJoinedPayload

        assertEquals("user_joined", type)
        assertEquals("uuid-2", payload.userId)
        assertEquals("RahulGuest", payload.username)
    }

    @Test
    fun `json sync_playback message decodes correctly`() {
        val jsonMsg = """{"type":"sync_playback","payload":{"action":"play","position":12000,"track_id":"song_123","server_time":1790762281567}}"""
        val (type, payloadBytes) = jsonCodec.decode(jsonMsg.toByteArray(Charsets.UTF_8))
        val payload = jsonCodec.decodePayload(type, payloadBytes, MessageFormat.JSON) as Listentogether.PlaybackActionPayload

        assertEquals("sync_playback", type)
        assertEquals("play", payload.action)
        assertEquals(12000L, payload.position)
        assertEquals("song_123", payload.trackId)
        assertEquals(1790762281567L, payload.serverTime)
    }

    @Test
    fun `json buffer_wait and buffer_complete decode correctly`() {
        val waitJson = """{"type":"buffer_wait","payload":{"track_id":"song_123","waiting_for":["uuid-2"]}}"""
        val (waitType, waitBytes) = jsonCodec.decode(waitJson.toByteArray(Charsets.UTF_8))
        val waitPayload = jsonCodec.decodePayload(waitType, waitBytes, MessageFormat.JSON) as Listentogether.BufferWaitPayload

        assertEquals("buffer_wait", waitType)
        assertEquals("song_123", waitPayload.trackId)
        assertEquals(listOf("uuid-2"), waitPayload.waitingForList)

        val completeJson = """{"type":"buffer_complete","payload":{"track_id":"song_123"}}"""
        val (compType, compBytes) = jsonCodec.decode(completeJson.toByteArray(Charsets.UTF_8))
        val compPayload = jsonCodec.decodePayload(compType, compBytes, MessageFormat.JSON) as Listentogether.BufferCompletePayload

        assertEquals("buffer_complete", compType)
        assertEquals("song_123", compPayload.trackId)
    }

    @Test
    fun `json pong with empty payload decodes safely without NPEs and does not corrupt clock`() {
        val jsonMsg = """{"type":"pong","payload":{}}"""
        val (type, payloadBytes) = jsonCodec.decode(jsonMsg.toByteArray(Charsets.UTF_8))
        val payload = jsonCodec.decodePayload(type, payloadBytes, MessageFormat.JSON) as Listentogether.PongPayload

        assertEquals("pong", type)
        assertNotNull(payload)
        assertEquals(0L, payload.clientTime)
        assertEquals(0L, payload.serverReceiveTime)
        assertEquals(0L, payload.serverSendTime)

        // Verify ServerClock safely rejects 0 timestamps without modifying clock offset
        val clock = ServerClock { 100_000L }
        val accepted = clock.recordPong(payload.clientTime, payload.serverReceiveTime, payload.serverSendTime)
        assertFalse(accepted)
        assertEquals(null, clock.now())
    }

    @Test
    fun `json reconnected message decodes correctly`() {
        val jsonMsg = """{"type":"reconnected","payload":{"room_code":"J2EYEG","user_id":"uuid-2","is_host":false,"state":{"room_code":"J2EYEG","host_id":"uuid-1","users":[],"current_track":null,"is_playing":false,"position":0,"last_update":1000,"volume":1,"queue":[]}}}"""
        val (type, payloadBytes) = jsonCodec.decode(jsonMsg.toByteArray(Charsets.UTF_8))
        val payload = jsonCodec.decodePayload(type, payloadBytes, MessageFormat.JSON) as Listentogether.ReconnectedPayload

        assertEquals("reconnected", type)
        assertEquals("J2EYEG", payload.roomCode)
        assertEquals("uuid-2", payload.userId)
        assertFalse(payload.isHost)
        assertTrue(payload.hasState())
    }

    @Test
    fun `json error message decodes correctly`() {
        val jsonMsg = """{"type":"error","payload":{"code":"ROOM_NOT_FOUND","message":"Room does not exist"}}"""
        val (type, payloadBytes) = jsonCodec.decode(jsonMsg.toByteArray(Charsets.UTF_8))
        val payload = jsonCodec.decodePayload(type, payloadBytes, MessageFormat.JSON) as Listentogether.ErrorPayload

        assertEquals("error", type)
        assertEquals("ROOM_NOT_FOUND", payload.code)
        assertEquals("Room does not exist", payload.message)
    }

    @Test
    fun `json encoding create_room produces expected json string`() {
        val createPayload = Listentogether.CreateRoomPayload.newBuilder().setUsername("AmanHost").build()
        val jsonBytes = jsonCodec.encode(MessageTypes.CREATE_ROOM, createPayload)
        val jsonStr = jsonBytes.toString(Charsets.UTF_8)

        assertTrue(jsonStr.contains(""""type":"create_room""""))
        assertTrue(jsonStr.contains(""""username":"AmanHost""""))
    }
}
