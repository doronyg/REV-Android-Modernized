package com.wowwee.revandroidsampleproject.pvp

import com.wowwee.revandroidsampleproject.network.GameStatePacket
import com.wowwee.revandroidsampleproject.network.PlayerProfile

sealed class PvpEvent {
    data class RemotePlayerStateUpdated(val packet: GameStatePacket) : PvpEvent()
    data class LocalPlayerHitReceived(val attackerId: String, val remainingHealth: Int) : PvpEvent()
    data class GameStartOffered(val packet: GameStatePacket) : PvpEvent()
    data class GameSessionActive(
        val sessionId: String,
        val localId: String,
        val remoteId: String?,
        val localHitsTaken: Int,
        val remoteHitsTaken: Int,
        val participants: List<String>,
        val participantProfiles: List<PlayerProfile>,
        val scoreByPlayer: Map<String, Int>
    ) : PvpEvent()
    data class PvpNetworkError(val message: String, val throwable: Throwable? = null) : PvpEvent()
}
