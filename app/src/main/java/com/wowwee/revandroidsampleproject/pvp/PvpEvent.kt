package com.wowwee.revandroidsampleproject.pvp

import com.wowwee.revandroidsampleproject.network.GameStatePacket

sealed class PvpEvent {
    data class RemotePlayerStateUpdated(val packet: GameStatePacket) : PvpEvent()
    data class LocalPlayerHitReceived(val attackerId: String, val remainingHealth: Int) : PvpEvent()
    data class GameStartOffered(val packet: GameStatePacket) : PvpEvent()
    data class GameSessionActive(
        val sessionId: String,
        val localId: String,
        val participants: List<String>
    ) : PvpEvent()
    data class PvpNetworkError(val message: String, val throwable: Throwable? = null) : PvpEvent()
}

