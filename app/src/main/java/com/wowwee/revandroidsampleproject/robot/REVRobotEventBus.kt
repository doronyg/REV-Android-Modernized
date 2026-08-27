package com.wowwee.revandroidsampleproject.robot

import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot.REVRobotInterface
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotFinder
import io.reactivex.rxjava3.core.Observable

/**
 * App-level singleton that bridges REV callbacks into a shared Rx event stream.
 */
object REVRobotEventBus {
    private val broadcaster = REVRobotEventBroadcaster()

    @JvmStatic
    val events: Observable<REVRobotEvent> = broadcaster.events

    @JvmStatic
    fun callbackInterface(): REVRobotInterface = broadcaster

    @JvmStatic
    fun attachToRobot(robot: REVRobot?) {
        robot?.setCallbackInterface(broadcaster)
    }

    @JvmStatic
    fun attachToConnectedRobots() {
        for (robot in REVRobotFinder.getInstance().getmRevRobotConnectedList()) {
            robot.setCallbackInterface(broadcaster)
        }
    }

    @JvmStatic
    fun emitBatteryInfo(robot: REVRobot?, batteryLevel: Int, voltage: Int) {
        broadcaster.emitBatteryInfo(robot, batteryLevel, voltage)
    }
}
