package com.wowwee.revandroidsampleproject.network

data class GameSessionConfig(
    val initialHealth: Int = 100,
    val maxPlayers: Int = 2,
    val heartbeatIntervalMs: Long = 1000L,
    val friendlyFireEnabled: Boolean = true,
    val hitHistoryLimit: Int = 20
)

