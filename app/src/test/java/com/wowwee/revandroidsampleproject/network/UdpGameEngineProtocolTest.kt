package com.wowwee.revandroidsampleproject.network

import com.google.gson.Gson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UdpGameEngineProtocolTest {
    private val gson = Gson()

    @Test
    fun `game state packet round trips with ack fields`() {
        val packet = GameStatePacket(
            senderId = "AA:BB:CC:DD:EE:FF",
            packetId = 42L,
            timestamp = 1_725_000_000_000L,
            health = 88,
            eventType = GameEventType.GAME_JOIN_ACK,
            lastHitBy = "11:22:33:44:55:66",
            totalHitsReceived = 3,
            hitHistory = listOf(HitRecord("11:22:33:44:55:66", 1_725_000_000_100L, 12)),
            sessionId = "session-1",
            hostId = "AA:BB:CC:DD:EE:FF",
            targetId = "22:33:44:55:66:77",
            ackForPacketId = 12L,
            players = listOf("11:22:33:44:55:66", "AA:BB:CC:DD:EE:FF"),
            gameConfig = GameSessionConfig(initialHealth = 100)
        )

        val json = gson.toJson(packet)
        val decoded = gson.fromJson(json, GameStatePacket::class.java)

        assertEquals(GameEventType.GAME_JOIN_ACK, decoded.eventType)
        assertEquals("session-1", decoded.sessionId)
        assertEquals(12L, decoded.ackForPacketId)
        assertEquals(1, decoded.hitHistory.size)
        assertEquals(88, decoded.health)
    }

    @Test
    fun `packet supports dynamic participant ids`() {
        val packet = GameStatePacket(
            senderId = "AA:BB:CC:DD:EE:FF",
            packetId = 99L,
            timestamp = 1_725_000_000_200L,
            health = 100,
            eventType = GameEventType.GAME_START,
            players = listOf("AA:BB:CC:DD:EE:FF", "11:22:33:44:55:66", "77:88:99:AA:BB:CC")
        )

        val decoded = gson.fromJson(gson.toJson(packet), GameStatePacket::class.java)
        assertEquals(3, decoded.players.size)
        assertTrue(decoded.players.contains("11:22:33:44:55:66"))
    }

    @Test
    fun `rev player id normalizes casing`() {
        val playerId = RevPlayerId.from("aa:bb:cc:dd:ee:ff")
        assertTrue(playerId.normalized().startsWith("AA:"))
    }
}



