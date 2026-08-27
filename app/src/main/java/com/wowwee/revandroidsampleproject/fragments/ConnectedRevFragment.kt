package com.wowwee.revandroidsampleproject.fragments

import android.os.Handler
import android.os.Looper
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotFinder
import com.wowwee.revandroidsampleproject.R
import com.wowwee.revandroidsampleproject.robot.REVRobotEvent
import com.wowwee.revandroidsampleproject.robot.REVRobotEventBus
import com.wowwee.revandroidsampleproject.utils.REVPlayer
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

abstract class ConnectedRevFragment : BaseViewFragment(), KioskLockInterface {

    companion object {
        private const val SIMULATOR_BATTERY_EVENT_DELAY_MS = 5000L
        private const val SIMULATOR_BATTERY_LEVEL = 76
        private const val SIMULATOR_BATTERY_VOLTAGE = 0
    }

    private var navigationUnlocked = false
    private val revEventDisposables = CompositeDisposable()
    private val simulatorEventHandler = Handler(Looper.getMainLooper())
    private val simulatorBatteryEventRunnable = Runnable {
        REVRobotEventBus.emitBatteryInfo(robot = null, batteryLevel = SIMULATOR_BATTERY_LEVEL, voltage = SIMULATOR_BATTERY_VOLTAGE)
    }

    protected fun resolveTargetRev(argumentKey: String): REVRobot? {
        val requestedAddress : String? = arguments?.getString(argumentKey)
        if (requestedAddress != null && requestedAddress != "") {
            for (robot in REVRobotFinder.getInstance().getmRevRobotConnectedList()) {
                val address = safeAddress(robot)
                if (requestedAddress.equals(address, ignoreCase = true)) {
                    return robot
                }
            }
        }

        return REVPlayer.getInstance().playerRev ?: REVRobotFinder.getInstance().firstConnectedREV()
    }

    protected fun currentDeviceAddress(argumentKey: String): String? {
        return safeAddress(rev ?: return arguments?.getString(argumentKey))
            ?: arguments?.getString(argumentKey)
    }

    protected fun isSimulatorMode(): Boolean = REVPlayer.getInstance().isSimulatorMode

    protected fun displayRevName(defaultName: String = "REV"): String {
        return rev?.name ?: REVPlayer.getInstance().simulatorName ?: defaultName
    }

    protected fun navigateBackToScan() {
        val activity = activity ?: return
        FragmentHelper.switchFragment(activity.supportFragmentManager, ScanFragment(), R.id.view_id_content, false)
    }

    protected fun attachRevEventSource(robot: REVRobot?) {
        REVRobotEventBus.attachToRobot(robot)
    }

    protected fun prepareConnectedRev(argumentKey: String): Boolean {
        rev = resolveTargetRev(argumentKey)
        if (rev == null && !isSimulatorMode()) {
            navigateBackToScan()
            return false
        }

        attachRevEventSource(rev)
        REVPlayer.getInstance().setPlayerRev(rev)
        return true
    }

    override fun onResume() {
        super.onResume()
        bindRevEventsIfNeeded()
        scheduleSimulatorBatteryEventIfNeeded()
    }

    override fun onPause() {
        simulatorEventHandler.removeCallbacks(simulatorBatteryEventRunnable)
        revEventDisposables.clear()
        super.onPause()
    }

    protected open fun onRevEvent(event: REVRobotEvent) {
        if (isCurrentRevDisconnected(event)) {
            navigateBackToScan()
        }
    }

    protected fun isCurrentRevDisconnected(event: REVRobotEvent): Boolean {
        return event is REVRobotEvent.DeviceDisconnected && event.robot == rev
    }

    override fun isKioskLockEnabled(): Boolean = true

    override fun onKioskBackPressed(): Boolean {
        if (!navigationUnlocked) {
            return true
        }

        navigationUnlocked = false
        navigateBackToScan()
        return true
    }

    private fun bindRevEventsIfNeeded() {
        if (revEventDisposables.size() > 0) return

        revEventDisposables.add(
            REVRobotEventBus.events
                .observeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ event ->
                    onRevEvent(event)
                }, {
                })
        )
    }

    private fun scheduleSimulatorBatteryEventIfNeeded() {
        simulatorEventHandler.removeCallbacks(simulatorBatteryEventRunnable)
        if (!isSimulatorMode()) {
            return
        }
        simulatorEventHandler.postDelayed(simulatorBatteryEventRunnable, SIMULATOR_BATTERY_EVENT_DELAY_MS)
    }

    private fun safeAddress(robot: REVRobot): String? {
        return try {
            robot.bluetoothDevice?.address
        } catch (_: SecurityException) {
            null
        }
    }
}
