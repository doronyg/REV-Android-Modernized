package com.wowwee.revandroidsampleproject.pvp

import com.wowwee.revandroidsampleproject.network.GameSessionConfig
import com.wowwee.revandroidsampleproject.network.GameStatePacket
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
    private var requestedPort: Int = DEFAULT_PORT

    @Volatile
    private var requestedSelfLoopback: Boolean = false

    @Volatile
    private var listening: Boolean = false

    @Volatile
    private var activePlayerId: String? = null

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
    fun onCarConnected(revId: String, port: Int = DEFAULT_PORT, allowSelfLoopback: Boolean = false) {
        synchronized(stateLock) {
            connectedPlayerId = revId.trim().takeIf { it.isNotEmpty() }
            requestedPort = port
            requestedSelfLoopback = allowSelfLoopback
        }
        refreshNetworkingState()
    }

    @JvmStatic
    fun onCarDisconnected() {
        synchronized(stateLock) {
            connectedPlayerId = null
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
    fun acknowledgeGameStart(packet: GameStatePacket) {
        stateMachine.acknowledgeGameStart(packet)
    }

    @JvmStatic
    fun sendHeartbeat() {
        stateMachine.sendHeartbeat()
    }

    @JvmStatic
    fun registerHitTaken(attackerRevId: String, damage: Int) {
        stateMachine.registerHitTaken(attackerRevId, damage)
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
        val targetPort: Int
        val targetSelfLoopback: Boolean

        synchronized(stateLock) {
            shouldListen = hostResumed && !connectedPlayerId.isNullOrBlank()
            targetPlayerId = connectedPlayerId
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
            stateMachine.bindLocalIdentity(playerId)
            stateMachine.attachTransport()
            UdpGameEngine.start(playerId, targetPort, targetSelfLoopback)
            synchronized(stateLock) {
                listening = true
                activePlayerId = playerId
                activePort = targetPort
                activeSelfLoopback = targetSelfLoopback
            }
            return
        }

        val requiresRestart =
            activePlayerId != playerId || activePort != targetPort || activeSelfLoopback != targetSelfLoopback

        if (requiresRestart) {
            UdpGameEngine.stop()
            stateMachine.bindLocalIdentity(playerId)
            UdpGameEngine.start(playerId, targetPort, targetSelfLoopback)
            synchronized(stateLock) {
                listening = true
                activePlayerId = playerId
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
        }
    }
}


