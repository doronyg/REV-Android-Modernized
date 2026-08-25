package com.wowwee.revandroidsampleproject.pvp

import com.wowwee.revandroidsampleproject.network.GameEventType
import com.wowwee.revandroidsampleproject.network.GameSessionConfig
import com.wowwee.revandroidsampleproject.network.GameStatePacket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

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
}


