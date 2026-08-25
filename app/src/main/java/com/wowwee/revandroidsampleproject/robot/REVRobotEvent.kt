package com.wowwee.revandroidsampleproject.robot

import com.wowwee.bluetoothrobotcontrollib.RobotCommand
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot
import com.wowwee.bluetoothrobotcontrollib.rev.REVTrackingStatus

sealed class REVRobotEvent {
    data class DeviceReady(val robot: REVRobot) : REVRobotEvent()
    data class DeviceDisconnected(val robot: REVRobot) : REVRobotEvent()

    data class BatteryInfoReceived(
        val robot: REVRobot?,
        val batteryLevel: Int,
        val voltage: Int
    ) : REVRobotEvent()

    data class HardwareVersionReceived(
        val robot: REVRobot?,
        val hardwareMainVersion: Int,
        val hardwareSubVersion: Int
    ) : REVRobotEvent()

    data class ToyActivationStatusReceived(
        val robot: REVRobot?,
        val toyActivated: Boolean,
        val cloudActivated: Boolean
    ) : REVRobotEvent()

    data class VolumeLevelReceived(
        val robot: REVRobot?,
        val volumeLevel: Int
    ) : REVRobotEvent()

    data class IrCommandReceived(
        val robot: REVRobot?,
        val irCommand: Byte,
        val rxSensor: Byte
    ) : REVRobotEvent()

    data class TrackingModeReceived(
        val robot: REVRobot?,
        val trackingMode: Byte
    ) : REVRobotEvent()

    data class TrackingStatusReceived(
        val status: REVTrackingStatus
    ) : REVRobotEvent()

    data class TrackingUpdateStatusReceived(
        val robot: REVRobot?,
        val updateStatus: Byte
    ) : REVRobotEvent()

    data class TrackingDistanceAndSpeedReceived(
        val robot: REVRobot?,
        val distance: Byte,
        val speed: Byte
    ) : REVRobotEvent()

    data class CurrentLedColorReceived(
        val robot: REVRobot?,
        val ledColor: Byte
    ) : REVRobotEvent()

    data class SoftwareVersionReceived(
        val robot: REVRobot?,
        val firmwareVersion: String,
        val hardwareVersion: String
    ) : REVRobotEvent()

    data class CurrentTractionReceived(
        val robot: REVRobot?,
        val traction: Byte
    ) : REVRobotEvent()

    data class UserStatusReceived(
        val robot: REVRobot?,
        val userId: Byte,
        val status: Byte
    ) : REVRobotEvent()

    data class BumpNotifyReceived(
        val robot: REVRobot
    ) : REVRobotEvent()

    data class RawDataReceived(
        val robot: REVRobot?,
        val payload: List<Byte>
    ) : REVRobotEvent()

    data class SpecialBroadcastIdChanged(
        val robot: REVRobot
    ) : REVRobotEvent()

    data class AvatarIconBroadcastDriverIdChanged(
        val robot: REVRobot
    ) : REVRobotEvent()

    data class RobotCommandProcessed(
        val robot: REVRobot?,
        val command: RobotCommand
    ) : REVRobotEvent()

    data class RobotJumpedOverRamp(
        val robot: REVRobot
    ) : REVRobotEvent()
}


