package com.wowwee.revandroidsampleproject.fragments

import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotFinder
import com.wowwee.revandroidsampleproject.R
import com.wowwee.revandroidsampleproject.utils.REVPlayer

abstract class ConnectedRevFragment : BaseViewFragment(), KioskLockInterface {

    private var navigationUnlocked = false

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

    override fun isKioskLockEnabled(): Boolean = true

    override fun onKioskBackPressed(): Boolean {
        if (!navigationUnlocked) {
            return true
        }

        navigationUnlocked = false
        navigateBackToScan()
        return true
    }

    private fun safeAddress(robot: REVRobot): String? {
        return try {
            robot.bluetoothDevice?.address
        } catch (_: SecurityException) {
            null
        }
    }
}
