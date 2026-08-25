package com.wowwee.revandroidsampleproject.network

data class GameStatePacket(
    val senderId: String,
    val packetId: Long,
    val timestamp: Long,
    val health: Int,
    val eventType: GameEventType,
    val lastHitBy: String? = null,
    val totalHitsReceived: Int = 0,
    val hitHistory: List<HitRecord> = emptyList(),
    val sessionId: String? = null,
    val hostId: String? = null,
    val targetId: String? = null,
    val ackForPacketId: Long? = null,
    val players: List<String> = emptyList(),
    val gameConfig: GameSessionConfig? = null
)

