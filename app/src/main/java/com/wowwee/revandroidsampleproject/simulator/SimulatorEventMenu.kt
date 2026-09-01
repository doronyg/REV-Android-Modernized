package com.wowwee.revandroidsampleproject.simulator

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.wowwee.revandroidsampleproject.R

object SimulatorEventMenu {
    fun show(context: Context, onActionSelected: (SimulatorMenuAction) -> Unit) {
        val labels = arrayOf(
            context.getString(R.string.advanced_mode_simulate_hit),
            context.getString(R.string.advanced_mode_simulate_other_hit),
            context.getString(R.string.advanced_mode_simulate_remote_game_start),
            context.getString(R.string.advanced_mode_simulate_stale_remote_hit),
            context.getString(R.string.advanced_mode_simulate_bump),
            context.getString(R.string.simulator_event_request_permissions),
            context.getString(R.string.simulator_event_request_enable_bluetooth),
            context.getString(R.string.simulator_event_discovery_recommended),
            context.getString(R.string.simulator_event_navigate_driver_mode),
            context.getString(R.string.simulator_event_primary_disconnected)
        )

        AlertDialog.Builder(context)
            .setTitle(R.string.advanced_mode_simulator_menu)
            .setItems(labels) { _, which ->
                val action = when (which) {
                    0 -> SimulatorMenuAction.LOCAL_HIT
                    1 -> SimulatorMenuAction.REMOTE_HIT
                    2 -> SimulatorMenuAction.REMOTE_GAME_START
                    3 -> SimulatorMenuAction.REMOTE_STALE_HIT_SEQUENCE
                    4 -> SimulatorMenuAction.BUMP
                    5 -> SimulatorMenuAction.REQUEST_PERMISSIONS
                    6 -> SimulatorMenuAction.REQUEST_ENABLE_BLUETOOTH
                    7 -> SimulatorMenuAction.DISCOVERY_RECOMMENDED
                    8 -> SimulatorMenuAction.NAVIGATE_DRIVER_MODE
                    9 -> SimulatorMenuAction.PRIMARY_DISCONNECTED
                    else -> null
                }
                if (action != null) {
                    onActionSelected(action)
                }
            }
            .show()
    }
}

