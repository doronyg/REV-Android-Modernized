package com.wowwee.revandroidsampleproject.pvp

import com.wowwee.revandroidsampleproject.network.PlayerProfile

data class GameSessionViewState(
    val sessionId: String?,
    val localId: String?,
    val remoteId: String?,
    val localDisplayName: String,
    val localColorHex: String,
    val remoteColorHex: String?,
    val localHitsTaken: Int,
    val remoteHitsTaken: Int,
    val isSessionActive: Boolean,
    val isStartPending: Boolean,
    val participants: List<String>,
    val participantProfiles: List<PlayerProfile>,
    val scoreByPlayer: Map<String, Int>
)

