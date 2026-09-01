package com.wowwee.revandroidsampleproject.simulator

import android.os.SystemClock
import com.wowwee.revandroidsampleproject.network.GameEventType
import com.wowwee.revandroidsampleproject.network.GameStatePacket
import com.wowwee.revandroidsampleproject.network.NetworkEventBus
import com.wowwee.revandroidsampleproject.network.PlayerProfile
import com.wowwee.revandroidsampleproject.utils.RevConnectionStateMachine

class SimulatorEventDispatcher(
    private val currentRemoteHits: () -> Int,
    private val onLocalHit: () -> Unit,
    private val onLocalBump: () -> Unit
) {
    fun dispatch(action: SimulatorMenuAction) {
        when (action) {
            SimulatorMenuAction.LOCAL_HIT -> onLocalHit()
            SimulatorMenuAction.REMOTE_HIT -> emitRemoteHit()
            SimulatorMenuAction.REMOTE_GAME_START -> emitRemoteGameStartOffer()
            SimulatorMenuAction.REMOTE_STALE_HIT_SEQUENCE -> emitStaleRemotePacketSequence()
            SimulatorMenuAction.BUMP -> onLocalBump()
            SimulatorMenuAction.REQUEST_PERMISSIONS -> emitUiEvent(RevConnectionStateMachine.UiEventType.REQUEST_PERMISSIONS)
            SimulatorMenuAction.REQUEST_ENABLE_BLUETOOTH -> emitUiEvent(RevConnectionStateMachine.UiEventType.REQUEST_ENABLE_BLUETOOTH)
            SimulatorMenuAction.DISCOVERY_RECOMMENDED -> emitUiEvent(RevConnectionStateMachine.UiEventType.DISCOVERY_RECOMMENDED)
            SimulatorMenuAction.NAVIGATE_DRIVER_MODE -> emitUiEvent(RevConnectionStateMachine.UiEventType.NAVIGATE_TO_DRIVER_MODE)
            SimulatorMenuAction.PRIMARY_DISCONNECTED -> emitUiEvent(RevConnectionStateMachine.UiEventType.PRIMARY_REV_DISCONNECTED)
        }
    }

    private fun emitRemoteHit() {
        val packet = GameStatePacket(
            senderId = REMOTE_SENDER_ID,
            senderName = REMOTE_DISPLAY_NAME,
            senderColorHex = REMOTE_COLOR,
            packetId = SystemClock.uptimeMillis(),
            timestamp = System.currentTimeMillis(),
            health = 100,
            eventType = GameEventType.IR_HIT_TAKEN,
            totalHitsReceived = currentRemoteHits().coerceAtLeast(0) + 1
        )
        NetworkEventBus.publishPacket(packet)
    }

    private fun emitRemoteGameStartOffer() {
        val now = System.currentTimeMillis()
        val session = "sim-session-${SystemClock.uptimeMillis()}"
        val packet = GameStatePacket(
            senderId = REMOTE_SENDER_ID,
            senderName = REMOTE_DISPLAY_NAME,
            senderColorHex = REMOTE_COLOR,
            senderInstanceId = "sim-remote-instance",
            packetId = now,
            timestamp = now,
            health = 100,
            eventType = GameEventType.GAME_START,
            sessionId = session,
            hostId = REMOTE_SENDER_ID,
            players = listOf(REMOTE_SENDER_ID),
            participantProfiles = listOf(
                PlayerProfile(REMOTE_SENDER_ID, REMOTE_DISPLAY_NAME, REMOTE_COLOR)
            )
        )
        NetworkEventBus.publishPacket(packet)
    }

    private fun emitStaleRemotePacketSequence() {
        val now = System.currentTimeMillis()
        val latestHits = currentRemoteHits().coerceAtLeast(0)
        NetworkEventBus.publishPacket(
            GameStatePacket(
                senderId = REMOTE_SENDER_ID,
                senderName = REMOTE_DISPLAY_NAME,
                senderColorHex = REMOTE_COLOR,
                packetId = now,
                timestamp = now,
                health = 95,
                eventType = GameEventType.IR_HIT_TAKEN,
                totalHitsReceived = latestHits + 1
            )
        )
        NetworkEventBus.publishPacket(
            GameStatePacket(
                senderId = REMOTE_SENDER_ID,
                senderName = REMOTE_DISPLAY_NAME,
                senderColorHex = REMOTE_COLOR,
                packetId = now + 1L,
                timestamp = now - 1_500L,
                health = 100,
                eventType = GameEventType.IR_HIT_TAKEN,
                totalHitsReceived = (latestHits - 1).coerceAtLeast(0)
            )
        )
    }

    private fun emitUiEvent(type: RevConnectionStateMachine.UiEventType) {
        RevConnectionStateMachine.getInstance().emitUiEventForSimulator(type)
    }

    companion object {
        private const val REMOTE_SENDER_ID = "SIM_REMOTE"
        private const val REMOTE_DISPLAY_NAME = "Remote Bot"
        private const val REMOTE_COLOR = "#F44336"
    }
}

