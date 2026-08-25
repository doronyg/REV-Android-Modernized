package com.wowwee.revandroidsampleproject.robot

import android.util.Log
import com.wowwee.bluetoothrobotcontrollib.RobotCommand
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot.REVRobotInterface
import com.wowwee.bluetoothrobotcontrollib.rev.REVTrackingStatus
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

class REVRobotEventBroadcaster : REVRobotInterface {
    private val subject = PublishSubject.create<REVRobotEvent>().toSerialized()

    val events: Observable<REVRobotEvent> = subject.hide()

    override fun revDeviceReady(robot: REVRobot) {
        subject.onNext(REVRobotEvent.DeviceReady(robot))
    }

    override fun revDeviceDisconnected(robot: REVRobot) {
        subject.onNext(REVRobotEvent.DeviceDisconnected(robot))
    }

    override fun revDidReceiveBatteryInfo(robot: REVRobot, batteryLevel: Int, voltage: Int) {
        subject.onNext(REVRobotEvent.BatteryInfoReceived(robot, batteryLevel, voltage))
    }

    override fun revDidReceiveHardwareVersion(
        robot: REVRobot,
        hardwareMainVersion: Int,
        hardwareSubVersion: Int
    ) {
        subject.onNext(
            REVRobotEvent.HardwareVersionReceived(robot, hardwareMainVersion, hardwareSubVersion)
        )
    }

    override fun revDidReceiveToyActivationStatus(robot: REVRobot, toyActivated: Boolean, cloudActivated: Boolean) {
        subject.onNext(REVRobotEvent.ToyActivationStatusReceived(robot, toyActivated, cloudActivated))
    }

    override fun revDidReceiveVolumeLevel(robot: REVRobot, volumeLevel: Int) {
        subject.onNext(REVRobotEvent.VolumeLevelReceived(robot, volumeLevel))
    }

    override fun revDidReceiveIRCommand(robot: REVRobot, irCommand: Byte, rxSensor: Byte) {
        subject.onNext(REVRobotEvent.IrCommandReceived(robot, irCommand, rxSensor))
    }

    override fun revDidReceiveTrackingMode(robot: REVRobot, trackingMode: Byte) {
        subject.onNext(REVRobotEvent.TrackingModeReceived(robot, trackingMode))
    }

    override fun revDidReceiveTrackingStatus(status: REVTrackingStatus) {
        subject.onNext(REVRobotEvent.TrackingStatusReceived(status))
    }

    override fun revDidReceiveTrackingUpdateStatus(robot: REVRobot, updateStatus: Byte) {
        subject.onNext(REVRobotEvent.TrackingUpdateStatusReceived(robot, updateStatus))
    }

    override fun revDidReceiveTrackingDistanceAndSpeed(robot: REVRobot, distance: Byte, speed: Byte) {
        subject.onNext(REVRobotEvent.TrackingDistanceAndSpeedReceived(robot, distance, speed))
    }

    override fun revDidReceiveCurrentLEDColor(robot: REVRobot, ledColor: Byte) {
        subject.onNext(REVRobotEvent.CurrentLedColorReceived(robot, ledColor))
    }

    override fun revDidReceiveSoftwareVersion(robot: REVRobot, firmwareVersion: String, hardwareVersion: String) {
        subject.onNext(REVRobotEvent.SoftwareVersionReceived(robot, firmwareVersion, hardwareVersion))
    }

    override fun revDidReceiveCurrentTraction(robot: REVRobot, traction: Byte) {
        subject.onNext(REVRobotEvent.CurrentTractionReceived(robot, traction))
    }

    override fun revDidReceiveUserStatus(robot: REVRobot, userId: Byte, status: Byte) {
        subject.onNext(REVRobotEvent.UserStatusReceived(robot, userId, status))
    }

    override fun revDidReceiveBumpNotify(robot: REVRobot) {
        subject.onNext(REVRobotEvent.BumpNotifyReceived(robot))
    }

    override fun revDidReceiveRawData(robot: REVRobot, payload: ArrayList<Byte>) {
        subject.onNext(REVRobotEvent.RawDataReceived(robot, payload.toList()))
    }

    override fun revSpecialBroadcastIDChanged(robot: REVRobot) {
        subject.onNext(REVRobotEvent.SpecialBroadcastIdChanged(robot))
    }

    override fun revAvatarIconBroadcastDriverIDChanged(robot: REVRobot) {
        subject.onNext(REVRobotEvent.AvatarIconBroadcastDriverIdChanged(robot))
    }

    override fun revBluetoothDidProcessedReceiveRobotCommand(robot: REVRobot, command: RobotCommand): Boolean {
        Log.d("REVRobotEventBroadcaster", "revBluetoothDidProcessedReceiveRobotCommand: ${command.description()}")
        subject.onNext(REVRobotEvent.RobotCommandProcessed(robot, command))
        return true
    }

    override fun revRobotDidJumpedOverRamp(robot: REVRobot) {
        subject.onNext(REVRobotEvent.RobotJumpedOverRamp(robot))
    }
}


