package com.wowwee.revandroidsampleproject.pvp

import com.wowwee.revandroidsampleproject.network.GameEventType
import com.wowwee.revandroidsampleproject.network.GameSessionConfig
import com.wowwee.revandroidsampleproject.network.GameStatePacket
import com.wowwee.revandroidsampleproject.network.HitRecord
import com.wowwee.revandroidsampleproject.network.NetworkEvent
import com.wowwee.revandroidsampleproject.network.NetworkEventBus
import com.wowwee.revandroidsampleproject.network.UdpGameEngine
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.ArrayDeque
import java.util.UUID
import kotlin.math.max

/**
 * Owns match/session state and decides what packets to emit via UdpGameEngine.
 * Transport concerns stay inside UdpGameEngine.
 */
class GameSessionStateMachine {
    private val stateLock = Any()
    private val hitHistory = ArrayDeque<HitRecord>()
    private val networkDisposables = CompositeDisposable()

    @Volatile
    private var localId: String? = null

    @Volatile
    private var currentHealth: Int = 100

    @Volatile
    private var totalHitsReceived: Int = 0

    @Volatile
    private var lastHitBy: String? = null

    @Volatile
    private var gameConfig: GameSessionConfig = GameSessionConfig()

    @Volatile
    private var sessionId: String? = null

    @Volatile
    private var hostId: String? = null

    @Volatile
    private var sessionActive: Boolean = false

    fun bindLocalIdentity(playerId: String) {
        localId = playerId.trim()
    }

    fun attachTransport() {
        if (networkDisposables.size() > 0) {
            return
        }
        networkDisposables.add(
            NetworkEventBus.events
                .observeOn(Schedulers.io())
                .subscribe({ event ->
                    when (event) {
                        is NetworkEvent.PacketReceived -> handleIncomingPacket(event.packet)
                        is NetworkEvent.TransportError -> PvpEventBus.publish(
                            PvpEvent.PvpNetworkError(event.message, event.throwable)
                        )
                    }
                }, { throwable ->
                    PvpEventBus.publish(PvpEvent.PvpNetworkError("Network stream failed", throwable))
                })
        )
    }

    fun detachTransport() {
        networkDisposables.clear()
    }

    fun resetGame(initialHealth: Int = 100) {
        synchronized(stateLock) {
            currentHealth = initialHealth.coerceIn(0, 100)
            totalHitsReceived = 0
            lastHitBy = null
            hitHistory.clear()
        }
    }

    fun startGame(config: GameSessionConfig = GameSessionConfig()) {
        val senderId = localId ?: return
        val newSessionId = UUID.randomUUID().toString()

        synchronized(stateLock) {
            gameConfig = config
            resetGame(config.initialHealth)
            sessionId = newSessionId
            hostId = senderId
            sessionActive = false
        }

        UdpGameEngine.sendPacket(
            buildPacket(
                senderId = senderId,
                eventType = GameEventType.GAME_START,
                players = listOf(senderId),
                sessionIdOverride = newSessionId,
                hostIdOverride = senderId,
                configOverride = config
            )
        )
    }

    fun acknowledgeGameStart(startPacket: GameStatePacket) {
        val senderId = localId ?: return
        if (startPacket.eventType != GameEventType.GAME_START) return
        if (startPacket.sessionId.isNullOrBlank() || startPacket.hostId.isNullOrBlank()) return

        synchronized(stateLock) {
            gameConfig = startPacket.gameConfig ?: GameSessionConfig()
            resetGame(gameConfig.initialHealth)
            sessionId = startPacket.sessionId
            hostId = startPacket.hostId
            sessionActive = true
        }

        UdpGameEngine.sendPacket(
            buildPacket(
                senderId = senderId,
                eventType = GameEventType.GAME_JOIN_ACK,
                players = mergePlayers(startPacket.players, senderId),
                targetId = startPacket.senderId,
                ackForPacketId = startPacket.packetId,
                sessionIdOverride = startPacket.sessionId,
                hostIdOverride = startPacket.hostId,
                configOverride = gameConfig
            )
        )

        notifySessionActive(mergePlayers(startPacket.players, senderId))
    }

    fun registerHitTaken(attackerRevId: String, damage: Int) {
        val senderId = localId ?: return
        if (!sessionActive) return

        val boundedDamage = max(0, damage)
        val remainingHealth: Int
        val historySnapshot: List<HitRecord>

        synchronized(stateLock) {
            currentHealth = (currentHealth - boundedDamage).coerceIn(0, 100)
            totalHitsReceived += 1
            lastHitBy = attackerRevId

            hitHistory.addFirst(
                HitRecord(
                    attackerId = attackerRevId,
                    timestamp = System.currentTimeMillis(),
                    damage = boundedDamage
                )
            )
            while (hitHistory.size > gameConfig.hitHistoryLimit.coerceAtLeast(1)) {
                hitHistory.removeLast()
            }
            remainingHealth = currentHealth
            historySnapshot = hitHistory.toList()
        }

        PvpEventBus.publish(PvpEvent.LocalPlayerHitReceived(attackerRevId, remainingHealth))

        UdpGameEngine.sendPacket(
            buildPacket(
                senderId = senderId,
                eventType = GameEventType.IR_HIT_TAKEN,
                lastHitBy = attackerRevId,
                hitHistoryOverride = historySnapshot
            )
        )
    }

    fun sendHeartbeat() {
        val senderId = localId ?: return
        if (!sessionActive) return

        UdpGameEngine.sendPacket(
            buildPacket(
                senderId = senderId,
                eventType = GameEventType.HEARTBEAT
            )
        )
    }

    fun sendGameOver() {
        val senderId = localId ?: return
        if (!sessionActive) return

        UdpGameEngine.sendPacket(
            buildPacket(
                senderId = senderId,
                eventType = GameEventType.GAME_OVER
            )
        )
    }

    private fun handleIncomingPacket(packet: GameStatePacket) {
        val senderId = localId ?: return

        when (packet.eventType) {
            GameEventType.GAME_START -> {
                PvpEventBus.publish(PvpEvent.GameStartOffered(packet))
            }

            GameEventType.GAME_JOIN_ACK -> {
                if (packet.targetId != null && packet.targetId != senderId) return
                if (packet.hostId != senderId) return
                val currentSession = sessionId ?: return
                if (packet.sessionId != currentSession) return

                synchronized(stateLock) {
                    sessionActive = true
                }
                notifySessionActive(mergePlayers(packet.players, senderId))
            }

            GameEventType.HEARTBEAT,
            GameEventType.IR_HIT_TAKEN,
            GameEventType.GAME_OVER -> {
                if (!isInActiveSession(packet)) return
                PvpEventBus.publish(PvpEvent.RemotePlayerStateUpdated(packet))
            }
        }
    }

    private fun isInActiveSession(packet: GameStatePacket): Boolean {
        val activeSession = sessionId ?: return false
        return sessionActive && packet.sessionId == activeSession
    }

    private fun notifySessionActive(participants: List<String>) {
        val activeSession = sessionId ?: return
        val senderId = localId ?: return
        PvpEventBus.publish(
            PvpEvent.GameSessionActive(
                sessionId = activeSession,
                localId = senderId,
                participants = participants
            )
        )
    }

    private fun buildPacket(
        senderId: String,
        eventType: GameEventType,
        players: List<String> = defaultPlayers(senderId),
        lastHitBy: String? = this.lastHitBy,
        targetId: String? = null,
        ackForPacketId: Long? = null,
        sessionIdOverride: String? = sessionId,
        hostIdOverride: String? = hostId,
        configOverride: GameSessionConfig? = gameConfig,
        hitHistoryOverride: List<HitRecord> = synchronized(stateLock) { hitHistory.toList() }
    ): GameStatePacket {
        val healthSnapshot = currentHealth
        val totalHitsSnapshot = totalHitsReceived
        return GameStatePacket(
            senderId = senderId,
            packetId = UdpGameEngine.nextPacketId(),
            timestamp = System.currentTimeMillis(),
            health = healthSnapshot,
            eventType = eventType,
            lastHitBy = lastHitBy,
            totalHitsReceived = totalHitsSnapshot,
            hitHistory = hitHistoryOverride,
            sessionId = sessionIdOverride,
            hostId = hostIdOverride,
            targetId = targetId,
            ackForPacketId = ackForPacketId,
            players = players,
            gameConfig = configOverride
        )
    }

    private fun defaultPlayers(senderId: String): List<String> {
        return listOf(senderId).distinct()
    }

    private fun mergePlayers(existing: List<String>, localPlayerId: String): List<String> {
        return (existing + localPlayerId).map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }
}


