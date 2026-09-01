package com.wowwee.revandroidsampleproject.pvp

import com.wowwee.revandroidsampleproject.network.GameSessionConfig
import com.wowwee.revandroidsampleproject.network.UdpGameEngine
import io.reactivex.rxjava3.core.Observable

/**
 * Lifecycle-aware bridge between app state and networking/session components.
 *
 * Starts transport only when host is resumed and a player identity is available.
 */
object GameSessionCoordinator {
    private const val DEFAULT_PORT = 8888

    private val stateLock = Any()
    private val stateMachine = GameSessionStateMachine()

    @Volatile
    private var hostResumed: Boolean = false

    @Volatile
    private var connectedPlayerId: String? = null

    @Volatile
    private var connectedPlayerName: String? = null

    @Volatile
    private var connectedPlayerColorHex: String? = null

    @Volatile
    private var requestedPort: Int = DEFAULT_PORT

    @Volatile
    private var requestedSelfLoopback: Boolean = false

    @Volatile
    private var listening: Boolean = false

    @Volatile
    private var activePlayerId: String? = null

    @Volatile
    private var activePlayerName: String? = null

    @Volatile
    private var activePlayerColorHex: String? = null

    @Volatile
    private var activePort: Int = DEFAULT_PORT

    @Volatile
    private var activeSelfLoopback: Boolean = false

    @JvmStatic
    val events: Observable<PvpEvent> = PvpEventBus.events

    @JvmStatic
    fun onHostResumed() {
        synchronized(stateLock) {
            hostResumed = true
        }
        refreshNetworkingState()
    }

    @JvmStatic
    fun onHostPaused() {
        synchronized(stateLock) {
            hostResumed = false
        }
        refreshNetworkingState()
    }

    @JvmStatic
    @JvmOverloads
    fun onCarConnected(
        revId: String,
        playerName: String? = null,
        playerColorHex: String? = null,
        port: Int = DEFAULT_PORT,
        allowSelfLoopback: Boolean = false
    ) {
        synchronized(stateLock) {
            connectedPlayerId = revId.trim().takeIf { it.isNotEmpty() }
            connectedPlayerName = playerName?.trim()?.takeIf { it.isNotEmpty() }
            connectedPlayerColorHex = playerColorHex?.trim()?.takeIf { it.isNotEmpty() }
            requestedPort = port
            requestedSelfLoopback = allowSelfLoopback
        }
        refreshNetworkingState()
    }

    @JvmStatic
    fun onCarDisconnected() {
        synchronized(stateLock) {
            connectedPlayerId = null
            connectedPlayerName = null
            connectedPlayerColorHex = null
            requestedSelfLoopback = false
        }
        refreshNetworkingState()
    }

    @JvmStatic
    fun isListening(): Boolean = listening

    @JvmStatic
    fun startGame(config: GameSessionConfig = GameSessionConfig()) {
        stateMachine.startGame(config)
    }

    @JvmStatic
    fun registerHitTaken(attackerRevId: String, damage: Int) {
        stateMachine.registerHitTaken(attackerRevId, damage)
    }

    @JvmStatic
    fun currentViewState(): GameSessionViewState {
        return stateMachine.currentViewState()
    }

    @JvmStatic
    fun resetGame(initialHealth: Int = 100) {
        stateMachine.resetGame(initialHealth)
    }

    @JvmStatic
    fun sendGameOver() {
        stateMachine.sendGameOver()
    }

    @JvmStatic
    fun stopGameNetworking() {
        synchronized(stateLock) {
            listening = false
            activePlayerId = null
        }
        stateMachine.detachTransport()
        UdpGameEngine.stop()
    }

    private fun refreshNetworkingState() {
        val shouldListen: Boolean
        val targetPlayerId: String?
        val targetPlayerName: String?
        val targetPlayerColorHex: String?
        val targetPort: Int
        val targetSelfLoopback: Boolean

        synchronized(stateLock) {
            shouldListen = hostResumed && !connectedPlayerId.isNullOrBlank()
            targetPlayerId = connectedPlayerId
            targetPlayerName = connectedPlayerName
            targetPlayerColorHex = connectedPlayerColorHex
            targetPort = requestedPort
            targetSelfLoopback = requestedSelfLoopback
        }

        if (!shouldListen) {
            stopIfRunning()
            return
        }

        val playerId = targetPlayerId ?: run {
            stopIfRunning()
            return
        }

        if (!listening) {
            stateMachine.bindLocalIdentity(playerId, targetPlayerName ?: playerId, targetPlayerColorHex ?: "")
            stateMachine.attachTransport()
            UdpGameEngine.start(playerId, targetPort, targetSelfLoopback)
            synchronized(stateLock) {
                listening = true
                activePlayerId = playerId
                activePlayerName = targetPlayerName
                activePlayerColorHex = targetPlayerColorHex
                activePort = targetPort
                activeSelfLoopback = targetSelfLoopback
            }
            return
        }

        val requiresRestart =
            activePlayerId != playerId ||
                activePlayerName != targetPlayerName ||
                activePlayerColorHex != targetPlayerColorHex ||
                activePort != targetPort ||
                activeSelfLoopback != targetSelfLoopback

        if (requiresRestart) {
            UdpGameEngine.stop()
            stateMachine.bindLocalIdentity(playerId, targetPlayerName ?: playerId, targetPlayerColorHex ?: "")
            UdpGameEngine.start(playerId, targetPort, targetSelfLoopback)
            synchronized(stateLock) {
                listening = true
                activePlayerId = playerId
                activePlayerName = targetPlayerName
                activePlayerColorHex = targetPlayerColorHex
                activePort = targetPort
                activeSelfLoopback = targetSelfLoopback
            }
        }
    }

    private fun stopIfRunning() {
        if (!listening) {
            return
        }
        stateMachine.detachTransport()
        UdpGameEngine.stop()
        synchronized(stateLock) {
            listening = false
            activePlayerId = null
            activePlayerName = null
            activePlayerColorHex = null
        }
    }
}


