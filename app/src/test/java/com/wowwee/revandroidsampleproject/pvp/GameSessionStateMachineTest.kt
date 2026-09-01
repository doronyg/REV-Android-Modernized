package com.wowwee.revandroidsampleproject.pvp

import com.wowwee.revandroidsampleproject.network.GameEventType
import com.wowwee.revandroidsampleproject.network.GameSessionConfig
import com.wowwee.revandroidsampleproject.network.GameStatePacket
import com.wowwee.revandroidsampleproject.network.NetworkEventBus
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameSessionStateMachineTest {

    @Test
    fun `registerHitTaken notifies local hit listener when session is active`() {
        val machine = GameSessionStateMachine()
        machine.bindLocalIdentity("LOCAL-1")

        val callbackHealth = AtomicInteger(-1)
        val disposable = PvpEventBus.events.subscribe { event ->
            if (event is PvpEvent.LocalPlayerHitReceived) {
                callbackHealth.set(event.remainingHealth)
            }
        }

        machine.acknowledgeGameStart(
            GameStatePacket(
                senderId = "HOST-1",
                packetId = 10L,
                timestamp = 100L,
                health = 100,
                eventType = GameEventType.GAME_START,
                sessionId = "session-1",
                hostId = "HOST-1",
                players = listOf("HOST-1"),
                gameConfig = GameSessionConfig(initialHealth = 100)
            )
        )

        machine.registerHitTaken(attackerRevId = "HOST-1", damage = 15)

        assertEquals(85, callbackHealth.get())
        disposable.dispose()
    }

    @Test
    fun `drops stale remote gameplay packet based on timestamp and hit count within active session`() {
        val machine = GameSessionStateMachine()
        machine.bindLocalIdentity("LOCAL-1", "Local", "#3F51B5")
        machine.attachTransport()

        val updates = CopyOnWriteArrayList<Long>()
        val disposable = PvpEventBus.events.subscribe { event ->
            if (
                event is PvpEvent.RemotePlayerStateUpdated &&
                event.packet.senderId == "REMOTE-1" &&
                event.packet.eventType == GameEventType.IR_HIT_TAKEN
            ) {
                updates.add(event.packet.packetId)
            }
        }

        try {
            NetworkEventBus.publishPacket(
                GameStatePacket(
                    senderId = "REMOTE-1",
                    packetId = 19L,
                    timestamp = 1_900L,
                    health = 100,
                    eventType = GameEventType.HEARTBEAT,
                    sessionId = "session-stale-test",
                    hostId = "REMOTE-1",
                    players = listOf("LOCAL-1", "REMOTE-1"),
                    scoreByPlayer = mapOf("LOCAL-1" to 0, "REMOTE-1" to 1)
                )
            )
            NetworkEventBus.publishPacket(
                GameStatePacket(
                    senderId = "REMOTE-1",
                    packetId = 20L,
                    timestamp = 2_000L,
                    health = 90,
                    eventType = GameEventType.IR_HIT_TAKEN,
                    totalHitsReceived = 2,
                    sessionId = "session-stale-test",
                    hostId = "REMOTE-1"
                )
            )
            NetworkEventBus.publishPacket(
                GameStatePacket(
                    senderId = "REMOTE-1",
                    packetId = 21L,
                    timestamp = 1_500L,
                    health = 95,
                    eventType = GameEventType.IR_HIT_TAKEN,
                    totalHitsReceived = 1,
                    sessionId = "session-stale-test",
                    hostId = "REMOTE-1"
                )
            )

            Thread.sleep(250)
            assertEquals(listOf(20L), updates.toList())
        } finally {
            disposable.dispose()
            machine.detachTransport()
        }
    }

    @Test
    fun `auto acknowledges incoming game start and activates session`() {
        val machine = GameSessionStateMachine()
        machine.bindLocalIdentity("LOCAL-1", "Local", "#3F51B5")
        machine.attachTransport()

        val sessionActiveLatch = CountDownLatch(1)
        val disposable = PvpEventBus.events.subscribe { event ->
            if (event is PvpEvent.GameSessionActive && event.localId == "LOCAL-1") {
                sessionActiveLatch.countDown()
            }
        }

        try {
            NetworkEventBus.publishPacket(
                GameStatePacket(
                    senderId = "HOST-1",
                    senderName = "Host",
                    packetId = 30L,
                    timestamp = 3_000L,
                    health = 100,
                    eventType = GameEventType.GAME_START,
                    sessionId = "session-auto-ack",
                    hostId = "HOST-1",
                    players = listOf("HOST-1")
                )
            )

            assertTrue(sessionActiveLatch.await(2, TimeUnit.SECONDS))
        } finally {
            disposable.dispose()
            machine.detachTransport()
        }
    }

    @Test
    fun `rejoin restores local score from host score map`() {
        val machine = GameSessionStateMachine()
        machine.bindLocalIdentity("LOCAL-1", "Local", "#3F51B5")

        val observedScores = mutableListOf<Int>()
        val disposable = PvpEventBus.events.subscribe { event ->
            if (event is PvpEvent.GameSessionActive && event.localId == "LOCAL-1") {
                observedScores.add(event.scoreByPlayer["LOCAL-1"] ?: -1)
            }
        }

        try {
            machine.acknowledgeGameStart(
                GameStatePacket(
                    senderId = "HOST-1",
                    packetId = 100L,
                    timestamp = 10_000L,
                    health = 100,
                    eventType = GameEventType.GAME_START,
                    sessionId = "session-restore",
                    hostId = "HOST-1",
                    players = listOf("HOST-1", "LOCAL-1"),
                    scoreByPlayer = mapOf("HOST-1" to 4, "LOCAL-1" to 2),
                    gameConfig = GameSessionConfig(initialHealth = 100)
                )
            )

            assertTrue(observedScores.isNotEmpty())
            assertEquals(2, observedScores.last())
        } finally {
            disposable.dispose()
        }
    }

    @Test
    fun `host hit right after startGame is counted before join ack`() {
        val machine = GameSessionStateMachine()
        machine.bindLocalIdentity("LOCAL-1")

        val callbackHealth = AtomicInteger(-1)
        val disposable = PvpEventBus.events.subscribe { event ->
            if (event is PvpEvent.LocalPlayerHitReceived) {
                callbackHealth.set(event.remainingHealth)
            }
        }

        try {
            machine.startGame(GameSessionConfig(initialHealth = 100))
            machine.registerHitTaken(attackerRevId = "REMOTE-1", damage = 10)
            assertEquals(90, callbackHealth.get())
        } finally {
            disposable.dispose()
        }
    }

    @Test
    fun `reconnect restores session and score from heartbeat`() {
        val machine = GameSessionStateMachine()
        machine.bindLocalIdentity("LOCAL-1", "Local", "#3F51B5")
        machine.attachTransport()

        val latch = CountDownLatch(1)
        val localScore = AtomicInteger(-1)
        val remoteScore = AtomicInteger(-1)
        val disposable = PvpEventBus.events.subscribe { event ->
            if (event is PvpEvent.GameSessionActive && event.localId == "LOCAL-1") {
                localScore.set(event.scoreByPlayer["LOCAL-1"] ?: -1)
                remoteScore.set(event.scoreByPlayer["REMOTE-1"] ?: -1)
                latch.countDown()
            }
        }

        try {
            NetworkEventBus.publishPacket(
                GameStatePacket(
                    senderId = "REMOTE-1",
                    packetId = 400L,
                    timestamp = 40_000L,
                    health = 100,
                    eventType = GameEventType.HEARTBEAT,
                    sessionId = "session-recover",
                    hostId = "REMOTE-1",
                    scoreByPlayer = mapOf("LOCAL-1" to 2, "REMOTE-1" to 5),
                    players = listOf("LOCAL-1", "REMOTE-1")
                )
            )

            assertTrue(latch.await(2, TimeUnit.SECONDS))
            assertEquals(2, localScore.get())
            assertEquals(5, remoteScore.get())
        } finally {
            disposable.dispose()
            machine.detachTransport()
        }
    }
}








