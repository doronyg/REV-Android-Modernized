package com.wowwee.revandroidsampleproject.fragments

import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotFinder
import com.wowwee.revandroidsampleproject.R
import com.wowwee.revandroidsampleproject.utils.REVPlayer

abstract class ConnectedRevFragment : BaseViewFragment() {

    protected fun resolveTargetRev(argumentKey: String): REVRobot? {
        val requestedAddress = arguments?.getString(argumentKey)
        if (!requestedAddress.isNullOrEmpty()) {
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

    protected fun navigateBackToScan() {
        val activity = activity ?: return
        FragmentHelper.switchFragment(activity.supportFragmentManager, ScanFragment(), R.id.view_id_content, false)
    }

    private fun safeAddress(robot: REVRobot): String? {
        return try {
            robot.bluetoothDevice?.address
        } catch (_: SecurityException) {
            null
        }
    }
}

