package com.wowwee.revandroidsampleproject.simulator

import android.os.Handler
import android.os.Looper
import com.wowwee.revandroidsampleproject.robot.REVRobotEventBus

class SimulatorBatteryBridge(
    private val handler: Handler = Handler(Looper.getMainLooper())
) {
    private val emitBatteryRunnable = Runnable {
        REVRobotEventBus.emitBatteryInfo(
            robot = null,
            batteryLevel = SIMULATOR_BATTERY_LEVEL,
            voltage = SIMULATOR_BATTERY_VOLTAGE
        )
    }

    fun scheduleIfSimulator(simulatorMode: Boolean) {
        cancel()
        if (!simulatorMode) {
            return
        }
        handler.postDelayed(emitBatteryRunnable, SIMULATOR_BATTERY_EVENT_DELAY_MS)
    }

    fun cancel() {
        handler.removeCallbacks(emitBatteryRunnable)
    }

    companion object {
        private const val SIMULATOR_BATTERY_EVENT_DELAY_MS = 5000L
        private const val SIMULATOR_BATTERY_LEVEL = 76
        private const val SIMULATOR_BATTERY_VOLTAGE = 0
    }
}

