package com.wowwee.revandroidsampleproject.pvp

import com.wowwee.revandroidsampleproject.network.UdpGameEngine
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

class GameSessionCoordinatorTest {

    @Before
    fun setUp() {
        resetCoordinatorAndEngine()
    }

    @After
    fun tearDown() {
        resetCoordinatorAndEngine()
    }

    @Test
    fun `starts networking only when host resumed and car connected`() {
        GameSessionCoordinator.onHostPaused()
        GameSessionCoordinator.onCarConnected(revId = "CAR-1", port = 0)

        assertFalse(GameSessionCoordinator.isListening())

        GameSessionCoordinator.onHostResumed()

        assertTrue(GameSessionCoordinator.isListening())
    }

    @Test
    fun `host pause stops active networking`() {
        GameSessionCoordinator.onHostResumed()
        GameSessionCoordinator.onCarConnected(revId = "CAR-1", port = 0)
        assertTrue(GameSessionCoordinator.isListening())

        GameSessionCoordinator.onHostPaused()

        assertFalse(GameSessionCoordinator.isListening())
    }

    @Test
    fun `car disconnect stops active networking`() {
        GameSessionCoordinator.onHostResumed()
        GameSessionCoordinator.onCarConnected(revId = "CAR-1", port = 0)
        assertTrue(GameSessionCoordinator.isListening())

        GameSessionCoordinator.onCarDisconnected()

        assertFalse(GameSessionCoordinator.isListening())
    }

    @Test
    fun `identity change while resumed keeps coordinator listening`() {
        GameSessionCoordinator.onHostResumed()
        GameSessionCoordinator.onCarConnected(revId = "CAR-1", port = 0)
        assertTrue(GameSessionCoordinator.isListening())

        GameSessionCoordinator.onCarConnected(revId = "CAR-2", port = 0)

        assertTrue(GameSessionCoordinator.isListening())
    }

    private fun resetCoordinatorAndEngine() {
        GameSessionCoordinator.onCarDisconnected()
        GameSessionCoordinator.onHostPaused()
        GameSessionCoordinator.stopGameNetworking()
        UdpGameEngine.stop()
    }
}
