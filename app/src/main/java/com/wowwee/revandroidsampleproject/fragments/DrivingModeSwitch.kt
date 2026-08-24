package com.wowwee.revandroidsampleproject.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.wowwee.revandroidsampleproject.R

enum class DrivingModeOption(val labelResId: Int) {
    MANUAL(R.string.driver_mode_option_manual),
    ADVANCED(R.string.driver_mode_option_advanced),
    PATH(R.string.driver_mode_option_path),
    EXPERIMENTS(R.string.driver_mode_option_experiments)
}

object DrivingModeSwitch {

    fun showModeSelectionDialog(
        host: ConnectedRevFragment,
        currentMode: DrivingModeOption,
        deviceAddress: String?
    ) {
        val context = host.context ?: return
        val activity = host.activity ?: return

        val options = DrivingModeOption.entries.filter { it != currentMode }
        val labels = options.map { context.getString(it.labelResId) }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(R.string.driver_mode_switch_title)
            .setItems(labels) { _, which ->
                val selectedMode = options.getOrNull(which) ?: return@setItems
                navigateToMode(activity, selectedMode, deviceAddress)
            }
            .show()
    }

    private fun navigateToMode(activity: FragmentActivity, mode: DrivingModeOption, deviceAddress: String?) {
        val destination: Fragment = when (mode) {
            DrivingModeOption.MANUAL -> DriverModeFragment.newInstance(deviceAddress)
            DrivingModeOption.ADVANCED -> AdvancedDrivingFragment.newInstance(deviceAddress)
            DrivingModeOption.PATH -> PathDriveFragment.newInstance(deviceAddress)
            DrivingModeOption.EXPERIMENTS -> ExperimentsDriveFragment.newInstance(deviceAddress)
        }

        FragmentHelper.switchFragment(
            activity.supportFragmentManager,
            destination,
            R.id.view_id_content,
            false
        )
    }
}

