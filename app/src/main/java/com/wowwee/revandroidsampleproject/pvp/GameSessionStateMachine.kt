package com.wowwee.revandroidsampleproject.pvp

import android.util.Log
import com.wowwee.revandroidsampleproject.network.GameEventType
import com.wowwee.revandroidsampleproject.network.GameSessionConfig
import com.wowwee.revandroidsampleproject.network.GameStatePacket
import com.wowwee.revandroidsampleproject.network.HitRecord
import com.wowwee.revandroidsampleproject.network.NetworkEvent
import com.wowwee.revandroidsampleproject.network.NetworkEventBus
import com.wowwee.revandroidsampleproject.network.PlayerProfile
import com.wowwee.revandroidsampleproject.network.UdpGameEngine
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.ArrayDeque
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.UUID
import kotlin.math.max

/**
 * Owns match/session state and decides what packets to emit via UdpGameEngine.
 * Transport concerns stay inside UdpGameEngine.
 */
class GameSessionStateMachine {
    companion object {
        private const val TAG = "PvpStateMachine"
        private const val DEFAULT_COLOR_HEX = "#000000"
        private const val MIN_RESYNC_INTERVAL_MS = 1500L
        private const val STALE_HEARTBEAT_MULTIPLIER = 4
        private const val HEARTBEAT_TICK_INTERVAL_MS = 250L
    }

    private data class SenderProgress(
        val timestamp: Long,
        val packetId: Long,
        val totalHitsReceived: Int
    )

    private val stateLock = Any()
    private val hitHistory = ArrayDeque<HitRecord>()
    private val networkDisposables = CompositeDisposable()

    @Volatile
    private var localId: String? = null

    @Volatile
    private var localDisplayName: String = "REV"

    @Volatile
    private var localColorHex: String = DEFAULT_COLOR_HEX

    @Volatile
    private var localInstanceId: String = UUID.randomUUID().toString()

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

    @Volatile
    private var lastRemotePacketUtcMs: Long? = null

    @Volatile
    private var lastResyncRequestUtcMs: Long = 0L

    @Volatile
    private var lastHeartbeatSentUtcMs: Long = 0L

    private val senderProgressById = linkedMapOf<String, SenderProgress>()
    private val participantProfilesById = linkedMapOf<String, PlayerProfile>()
    private val scoreByPlayer = linkedMapOf<String, Int>()

    fun bindLocalIdentity(playerId: String, playerName: String = playerId, playerColorHex: String = DEFAULT_COLOR_HEX) {
        localId = playerId.trim()
        localDisplayName = playerName.trim().ifEmpty { playerId.trim() }
        localColorHex = sanitizeColorHex(playerColorHex)
        localInstanceId = UUID.randomUUID().toString()
        synchronized(stateLock) {
            val normalizedId = localId
            if (!normalizedId.isNullOrBlank()) {
                participantProfilesById[normalizedId] = localProfile()
            }
            senderProgressById.clear()
            participantProfilesById.clear()
            scoreByPlayer.clear()
            if (!normalizedId.isNullOrBlank()) {
                participantProfilesById[normalizedId] = localProfile()
                scoreByPlayer[normalizedId] = totalHitsReceived
            }
            lastRemotePacketUtcMs = null
            lastResyncRequestUtcMs = 0L
            lastHeartbeatSentUtcMs = 0L
        }
        runCatching { Log.i(TAG, "bindLocalIdentity id=$localId name=$localDisplayName color=$localColorHex instance=$localInstanceId") }
    }

    fun attachTransport() {
        if (networkDisposables.size() > 0) {
            return
        }
        runCatching { Log.i(TAG, "attachTransport subscribing to network events and heartbeat ticker") }
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
        networkDisposables.add(
            Observable.interval(HEARTBEAT_TICK_INTERVAL_MS, TimeUnit.MILLISECONDS, Schedulers.io())
                .subscribe({
                    sendHeartbeatIfDue(force = false)
                }, { throwable ->
                    PvpEventBus.publish(PvpEvent.PvpNetworkError("Heartbeat scheduler failed", throwable))
                })
        )
    }

    fun detachTransport() {
        networkDisposables.clear()
        runCatching { Log.i(TAG, "detachTransport cleared subscriptions") }
    }

    fun currentViewState(): GameSessionViewState {
        return synchronized(stateLock) { buildViewStateLocked() }
    }

    fun resetGame(initialHealth: Int = 100) {
        synchronized(stateLock) {
            currentHealth = initialHealth.coerceIn(0, 100)
            totalHitsReceived = 0
            lastHitBy = null
            hitHistory.clear()
            senderProgressById.clear()
            scoreByPlayer.clear()
            val senderId = localId
            if (!senderId.isNullOrBlank()) {
                scoreByPlayer[senderId] = totalHitsReceived
            }
            lastRemotePacketUtcMs = null
            lastResyncRequestUtcMs = 0L
            lastHeartbeatSentUtcMs = 0L
        }
        runCatching { Log.i(TAG, "resetGame health=$initialHealth") }
    }

    fun startGame(config: GameSessionConfig = GameSessionConfig()) {
        val senderId = localId ?: run {
            runCatching { Log.w(TAG, "startGame ignored: localId is not bound") }
            return
        }
        val newSessionId = UUID.randomUUID().toString()

        synchronized(stateLock) {
            gameConfig = config
            resetGame(config.initialHealth)
            sessionId = newSessionId
            hostId = senderId
            sessionActive = false
            participantProfilesById[senderId] = localProfile()
            scoreByPlayer[senderId] = totalHitsReceived
            lastHeartbeatSentUtcMs = 0L
        }
        runCatching { Log.i(TAG, "startGame requested by=$senderId sessionId=$newSessionId") }
        runCatching { Log.i(TAG, "host waiting for GAME_JOIN_ACK sessionId=$newSessionId") }

        UdpGameEngine.sendPacket(
            buildPacket(
                senderId = senderId,
                eventType = GameEventType.GAME_START,
                players = listOf(senderId),
                participantProfiles = listOf(localProfile()),
                sessionIdOverride = newSessionId,
                hostIdOverride = senderId,
                configOverride = config
            )
        )
    }

    fun acknowledgeGameStart(startPacket: GameStatePacket) {
        val senderId = localId ?: run {
            runCatching { Log.w(TAG, "acknowledgeGameStart ignored: localId is not bound") }
            return
        }
        if (startPacket.eventType != GameEventType.GAME_START) {
            runCatching { Log.w(TAG, "acknowledgeGameStart ignored: eventType=${startPacket.eventType}") }
            return
        }
        if (startPacket.sessionId.isNullOrBlank() || startPacket.hostId.isNullOrBlank()) {
            runCatching { Log.w(TAG, "acknowledgeGameStart ignored: missing sessionId/hostId") }
            return
        }

        synchronized(stateLock) {
            gameConfig = startPacket.gameConfig ?: GameSessionConfig()
            resetGame(gameConfig.initialHealth)
            sessionId = startPacket.sessionId
            hostId = startPacket.hostId
            sessionActive = true
            mergeProfiles(startPacket.participantProfiles)
            mergeProfiles(listOfNotNull(startPacket.toSenderProfile()))
            participantProfilesById[senderId] = localProfile()
            applyAuthoritativeScores(startPacket.scoreByPlayer)
            scoreByPlayer[senderId] = totalHitsReceived
            lastHeartbeatSentUtcMs = 0L
        }
        runCatching { Log.i(TAG, "acknowledgeGameStart local=$senderId sessionId=${startPacket.sessionId} hostId=${startPacket.hostId}") }

        UdpGameEngine.sendPacket(
            buildPacket(
                senderId = senderId,
                eventType = GameEventType.GAME_JOIN_ACK,
                players = mergePlayers(startPacket.players, senderId),
                participantProfiles = mergedProfiles(startPacket.participantProfiles),
                targetId = startPacket.senderId,
                ackForPacketId = startPacket.packetId,
                sessionIdOverride = startPacket.sessionId,
                hostIdOverride = startPacket.hostId,
                configOverride = gameConfig
            )
        )

        notifySessionActive()
    }

    fun registerHitTaken(attackerRevId: String, damage: Int) {
        val senderId = localId ?: return
        val hasStartedSession = !sessionId.isNullOrBlank()
        if (!sessionActive && !hasStartedSession) {
            runCatching { Log.d(TAG, "registerHitTaken ignored: no active/pending session") }
            return
        }

        val boundedDamage = max(0, damage)
        val remainingHealth: Int
        val historySnapshot: List<HitRecord>

        synchronized(stateLock) {
            currentHealth = (currentHealth - boundedDamage).coerceIn(0, 100)
            totalHitsReceived += 1
            lastHitBy = attackerRevId
            scoreByPlayer[senderId] = totalHitsReceived

            hitHistory.addFirst(
                HitRecord(
                    attackerId = attackerRevId,
                    timestamp = utcNow(),
                    damage = boundedDamage
                )
            )
            while (hitHistory.size > gameConfig.hitHistoryLimit.coerceAtLeast(1)) {
                hitHistory.removeLast()
            }
            remainingHealth = currentHealth
            historySnapshot = hitHistory.toList()
        }
        runCatching { Log.d(TAG, "registerHitTaken by=$attackerRevId damage=$boundedDamage health=$remainingHealth hits=$totalHitsReceived") }

        PvpEventBus.publish(PvpEvent.LocalPlayerHitReceived(attackerRevId, remainingHealth))
        notifySessionActive()

        UdpGameEngine.sendPacket(
            buildPacket(
                senderId = senderId,
                eventType = GameEventType.IR_HIT_TAKEN,
                lastHitBy = attackerRevId,
                hitHistoryOverride = historySnapshot
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

        synchronized(stateLock) {
            sessionActive = false
            lastHeartbeatSentUtcMs = 0L
        }
        runCatching { Log.i(TAG, "sendGameOver sender=$senderId session=${sessionId ?: "none"}") }
    }

    private fun handleIncomingPacket(packet: GameStatePacket) {
        val senderId = localId ?: return
        if (packet.senderId == senderId) return

        runCatching { Log.d(TAG, "incoming type=${packet.eventType} from=${packet.senderId} session=${packet.sessionId ?: "none"} packet=${packet.packetId}") }

        when (packet.eventType) {
            GameEventType.GAME_START -> {
                PvpEventBus.publish(PvpEvent.GameStartOffered(packet))
                runCatching { Log.i(TAG, "received GAME_START from=${packet.senderId} session=${packet.sessionId}") }
                acknowledgeGameStart(packet)
            }

            GameEventType.GAME_JOIN_ACK -> {
                if (packet.targetId != null && packet.targetId != senderId) {
                    runCatching { Log.d(TAG, "ignore GAME_JOIN_ACK: target mismatch target=${packet.targetId} local=$senderId") }
                    return
                }
                if (packet.hostId != senderId) {
                    runCatching { Log.d(TAG, "ignore GAME_JOIN_ACK: host mismatch packetHost=${packet.hostId} local=$senderId") }
                    return
                }
                val currentSession = sessionId ?: run {
                    runCatching { Log.d(TAG, "ignore GAME_JOIN_ACK: no active host session") }
                    return
                }
                if (packet.sessionId != currentSession) {
                    runCatching { Log.d(TAG, "ignore GAME_JOIN_ACK: session mismatch packet=${packet.sessionId} local=$currentSession") }
                    return
                }

                synchronized(stateLock) {
                    sessionActive = true
                    mergeProfiles(packet.participantProfiles)
                    mergeProfiles(listOfNotNull(packet.toSenderProfile()))
                    applyAuthoritativeScores(packet.scoreByPlayer)
                    lastHeartbeatSentUtcMs = 0L
                }
                runCatching { Log.i(TAG, "received GAME_JOIN_ACK session=${packet.sessionId} participants=${packet.players.size}") }
                notifySessionActive()
            }

            GameEventType.HEARTBEAT,
            GameEventType.IR_HIT_TAKEN,
            GameEventType.STATE_SNAPSHOT,
            GameEventType.GAME_OVER -> {
                if (packet.eventType == GameEventType.HEARTBEAT || packet.eventType == GameEventType.STATE_SNAPSHOT) {
                    maybeRecoverSessionFromGameplayPacket(packet)
                }
                if (!canProcessGameplayPacket(packet)) return
                updateGameplayStateFromPacket(packet)
                PvpEventBus.publish(PvpEvent.RemotePlayerStateUpdated(packet))
                notifySessionActive()
            }

            GameEventType.RESYNC_REQUEST -> {
                if (!canProcessControlPacket(packet)) return
                if (packet.targetId != null && packet.targetId != senderId) return
                runCatching { Log.i(TAG, "received RESYNC_REQUEST from=${packet.senderId}; sending snapshot") }
                sendStateSnapshot(targetId = packet.senderId)
            }
        }
    }

    private fun canProcessGameplayPacket(packet: GameStatePacket): Boolean {
        return isInActiveSession(packet) && isNewerGameplayState(packet)
    }

    private fun canProcessControlPacket(packet: GameStatePacket): Boolean {
        return isInActiveSession(packet)
    }

    private fun isInActiveSession(packet: GameStatePacket): Boolean {
        val activeSession = sessionId ?: return false
        return sessionActive && packet.sessionId == activeSession
    }

    private fun notifySessionActive() {
        val snapshot = currentViewState()
        if (!snapshot.isSessionActive || snapshot.sessionId.isNullOrBlank() || snapshot.localId.isNullOrBlank()) return
        val activeSessionId = snapshot.sessionId
        val activeLocalId = snapshot.localId
        runCatching {
            Log.i(
                TAG,
                "sessionActive=true session=${snapshot.sessionId} local=${snapshot.localId} participants=${snapshot.participants.joinToString()} score=${snapshot.localHitsTaken}-${snapshot.remoteHitsTaken}"
            )
        }
        PvpEventBus.publish(
            PvpEvent.GameSessionActive(
                sessionId = activeSessionId,
                localId = activeLocalId,
                remoteId = snapshot.remoteId,
                localHitsTaken = snapshot.localHitsTaken,
                remoteHitsTaken = snapshot.remoteHitsTaken,
                participants = snapshot.participants,
                participantProfiles = snapshot.participantProfiles,
                scoreByPlayer = snapshot.scoreByPlayer
            )
        )
    }

    private fun buildViewStateLocked(): GameSessionViewState {
        val senderId = localId
        val participants = (participantProfilesById.keys + listOfNotNull(senderId))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
        val profiles = participantProfilesById.values.toList()
        val scoreSnapshot = scoreByPlayer.toMap()
        val remoteId = if (senderId.isNullOrBlank()) null else participants.firstOrNull { it != senderId }
        val localHits = if (senderId.isNullOrBlank()) 0 else (scoreSnapshot[senderId] ?: totalHitsReceived).coerceAtLeast(0)
        val remoteHits = if (!remoteId.isNullOrBlank()) {
            (scoreSnapshot[remoteId] ?: 0).coerceAtLeast(0)
        } else {
            scoreSnapshot
                .filterKeys { it != senderId }
                .values
                .maxOrNull()
                ?.coerceAtLeast(0)
                ?: 0
        }
        val remoteColorHex = remoteId?.let { participantProfilesById[it]?.colorHex }

        return GameSessionViewState(
            sessionId = sessionId,
            localId = senderId,
            remoteId = remoteId,
            localDisplayName = localDisplayName,
            localColorHex = localColorHex,
            remoteColorHex = remoteColorHex,
            localHitsTaken = localHits,
            remoteHitsTaken = remoteHits,
            isSessionActive = sessionActive && !sessionId.isNullOrBlank(),
            isStartPending = !sessionActive && !sessionId.isNullOrBlank(),
            participants = participants,
            participantProfiles = profiles,
            scoreByPlayer = scoreSnapshot
        )
    }

    private fun updateGameplayStateFromPacket(packet: GameStatePacket) {
        synchronized(stateLock) {
            mergeProfiles(packet.participantProfiles)
            participantProfilesById[packet.senderId] = packet.toSenderProfile() ?: participantProfilesById[packet.senderId] ?: fallbackProfile(packet.senderId)
            val incomingHits = packet.totalHitsReceived.coerceAtLeast(0)
            scoreByPlayer[packet.senderId] = max(scoreByPlayer[packet.senderId] ?: 0, incomingHits)
            applyAuthoritativeScores(packet.scoreByPlayer)
            lastRemotePacketUtcMs = utcNow()
        }
    }

    private fun maybeRecoverSessionFromGameplayPacket(packet: GameStatePacket) {
        if (packet.eventType != GameEventType.HEARTBEAT && packet.eventType != GameEventType.STATE_SNAPSHOT) {
            return
        }
        if (sessionActive) {
            return
        }
        val recoveredSessionId = packet.sessionId?.takeIf { it.isNotBlank() } ?: return
        val recoveredHostId = packet.hostId?.takeIf { it.isNotBlank() } ?: return
        val senderId = localId ?: return

        synchronized(stateLock) {
            if (sessionActive) return@synchronized
            sessionId = recoveredSessionId
            hostId = recoveredHostId
            sessionActive = true
            gameConfig = packet.gameConfig ?: gameConfig
            mergeProfiles(packet.participantProfiles)
            mergeProfiles(listOfNotNull(packet.toSenderProfile()))
            participantProfilesById[senderId] = localProfile()
            val incomingHits = packet.totalHitsReceived.coerceAtLeast(0)
            scoreByPlayer[packet.senderId] = max(scoreByPlayer[packet.senderId] ?: 0, incomingHits)
            applyAuthoritativeScores(packet.scoreByPlayer)
            scoreByPlayer[senderId] = max(scoreByPlayer[senderId] ?: 0, totalHitsReceived)
            lastRemotePacketUtcMs = utcNow()
            lastHeartbeatSentUtcMs = 0L
        }

        runCatching { Log.i(TAG, "recovered session from ${packet.eventType} session=$recoveredSessionId host=$recoveredHostId") }
        notifySessionActive()
    }

    private fun buildPacket(
        senderId: String,
        eventType: GameEventType,
        players: List<String> = defaultPlayers(senderId),
        participantProfiles: List<PlayerProfile> = participantProfilesSnapshot(),
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
        val scoreSnapshot = synchronized(stateLock) {
            val local = scoreByPlayer.toMutableMap()
            local[senderId] = totalHitsSnapshot
            local.toMap()
        }
        return GameStatePacket(
            senderId = senderId,
            senderName = localDisplayName,
            senderColorHex = localColorHex,
            senderInstanceId = localInstanceId,
            packetId = UdpGameEngine.nextPacketId(),
            timestamp = utcNow(),
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
            participantProfiles = participantProfiles,
            scoreByPlayer = scoreSnapshot,
            gameConfig = configOverride
        )
    }

    private fun defaultPlayers(senderId: String): List<String> {
        return listOf(senderId).distinct()
    }

    private fun mergePlayers(existing: List<String>, localPlayerId: String): List<String> {
        return (existing + localPlayerId).map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }

    private fun maybeRequestResync(senderId: String) {
        val now = utcNow()
        val staleAfterMs = gameConfig.heartbeatIntervalMs.coerceAtLeast(250L) * STALE_HEARTBEAT_MULTIPLIER
        val lastRemoteSeen = lastRemotePacketUtcMs ?: return
        if (now - lastRemoteSeen < staleAfterMs) return

        val minInterval = max(staleAfterMs, MIN_RESYNC_INTERVAL_MS)
        if (now - lastResyncRequestUtcMs < minInterval) return

        lastResyncRequestUtcMs = now
        runCatching { Log.w(TAG, "stale remote detected; requesting resync sender=$senderId host=${hostId ?: "none"}") }
        UdpGameEngine.sendPacket(
            buildPacket(
                senderId = senderId,
                eventType = GameEventType.RESYNC_REQUEST,
                targetId = hostId
            )
        )
    }

    private fun sendStateSnapshot(targetId: String?) {
        val senderId = localId ?: return
        if (!sessionActive) return
        runCatching { Log.i(TAG, "sendStateSnapshot from=$senderId to=${targetId ?: "broadcast"}") }
        UdpGameEngine.sendPacket(
            buildPacket(
                senderId = senderId,
                eventType = GameEventType.STATE_SNAPSHOT,
                targetId = targetId
            )
        )
    }

    private fun sendHeartbeatIfDue(force: Boolean) {
        val senderId = localId ?: return
        if (!sessionActive) return

        val now = utcNow()
        val heartbeatInterval = gameConfig.heartbeatIntervalMs.coerceAtLeast(250L)
        if (!force && now - lastHeartbeatSentUtcMs < heartbeatInterval) {
            return
        }

        lastHeartbeatSentUtcMs = now
        maybeRequestResync(senderId)
        if (force) {
            runCatching { Log.d(TAG, "heartbeat forced sender=$senderId session=${sessionId ?: "none"}") }
        }

        UdpGameEngine.sendPacket(
            buildPacket(
                senderId = senderId,
                eventType = GameEventType.HEARTBEAT
            )
        )
    }

    private fun isNewerGameplayState(packet: GameStatePacket): Boolean {
        synchronized(stateLock) {
            val previous = senderProgressById[packet.senderId]
            if (previous != null) {
                val timestampAdvanced = packet.timestamp > previous.timestamp
                val packetAdvanced = packet.packetId > previous.packetId
                val hitsAdvanced = packet.totalHitsReceived > previous.totalHitsReceived
                val clearlyOlder = packet.timestamp < previous.timestamp && !hitsAdvanced
                if (clearlyOlder) {
                    runCatching { Log.d(TAG, "drop stale packet sender=${packet.senderId} ts=${packet.timestamp} < ${previous.timestamp}") }
                    return false
                }
                if (!timestampAdvanced && !packetAdvanced && !hitsAdvanced) {
                    runCatching { Log.d(TAG, "drop non-advancing packet sender=${packet.senderId} packet=${packet.packetId} prev=${previous.packetId}") }
                    return false
                }
            }

            senderProgressById[packet.senderId] = SenderProgress(
                timestamp = packet.timestamp,
                packetId = packet.packetId,
                totalHitsReceived = packet.totalHitsReceived
            )
            return true
        }
    }

    private fun mergedProfiles(remoteProfiles: List<PlayerProfile>): List<PlayerProfile> {
        synchronized(stateLock) {
            mergeProfiles(remoteProfiles)
            val senderId = localId
            if (!senderId.isNullOrBlank()) {
                participantProfilesById[senderId] = localProfile()
            }
            return participantProfilesById.values.toList()
        }
    }

    private fun participantProfilesSnapshot(): List<PlayerProfile> {
        synchronized(stateLock) {
            return participantProfilesById.values.toList()
        }
    }

    private fun mergeProfiles(profiles: List<PlayerProfile>) {
        for (profile in profiles) {
            val normalizedId = profile.playerId.trim()
            if (normalizedId.isEmpty()) continue
            participantProfilesById[normalizedId] = profile.copy(
                playerId = normalizedId,
                displayName = profile.displayName.trim().ifEmpty { normalizedId },
                colorHex = sanitizeColorHex(profile.colorHex)
            )
        }
    }

    private fun localProfile(): PlayerProfile {
        val senderId = localId.orEmpty()
        return PlayerProfile(
            playerId = senderId,
            displayName = localDisplayName,
            colorHex = localColorHex
        )
    }

    private fun fallbackProfile(playerId: String): PlayerProfile {
        return PlayerProfile(
            playerId = playerId,
            displayName = playerId,
            colorHex = DEFAULT_COLOR_HEX
        )
    }

    private fun GameStatePacket.toSenderProfile(): PlayerProfile? {
        val normalizedId = senderId.trim()
        if (normalizedId.isEmpty()) return null
        return PlayerProfile(
            playerId = normalizedId,
            displayName = senderName?.trim().orEmpty().ifEmpty { normalizedId },
            colorHex = sanitizeColorHex(senderColorHex)
        )
    }

    private fun sanitizeColorHex(value: String?): String {
        val normalized = value?.trim()?.uppercase(Locale.US)
        val candidate = if (normalized.isNullOrEmpty()) null else normalized
        return if (candidate != null && Regex("^#[0-9A-F]{6}$").matches(candidate)) {
            candidate
        } else {
            DEFAULT_COLOR_HEX
        }
    }

    private fun utcNow(): Long = System.currentTimeMillis()

    private fun applyAuthoritativeScores(incoming: Map<String, Int>) {
        val senderId = localId
        for ((playerIdRaw, scoreRaw) in incoming) {
            val playerId = playerIdRaw.trim()
            if (playerId.isEmpty()) continue
            val score = scoreRaw.coerceAtLeast(0)
            scoreByPlayer[playerId] = max(scoreByPlayer[playerId] ?: 0, score)
            if (!senderId.isNullOrBlank() && playerId == senderId) {
                totalHitsReceived = max(totalHitsReceived, score)
            }
        }
    }

}
