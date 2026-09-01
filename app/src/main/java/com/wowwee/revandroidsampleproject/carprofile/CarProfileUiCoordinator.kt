package com.wowwee.revandroidsampleproject.carprofile

import android.content.Context
import com.wowwee.revandroidsampleproject.R
import com.wowwee.revandroidsampleproject.simulator.SimulatorModeController
import com.wowwee.revandroidsampleproject.utils.AppPreferences

object CarProfileUiCoordinator {
    private const val SIMULATOR_PROFILE_ID = "SIMULATOR:LOCAL"

    fun currentProfileCarId(isSimulatorMode: Boolean, currentDeviceAddress: String?): String? {
        return if (isSimulatorMode) {
            SIMULATOR_PROFILE_ID
        } else {
            currentDeviceAddress
        }
    }

    fun shouldPromptForMissingProfile(
        context: Context?,
        isSimulatorMode: Boolean,
        currentDeviceAddress: String?
    ): Boolean {
        if (context == null || isSimulatorMode) {
            return false
        }
        val carId = currentProfileCarId(isSimulatorMode, currentDeviceAddress) ?: return false
        return !AppPreferences.hasCarProfile(context, carId)
    }

    fun preferredLocalColorHex(
        context: Context?,
        isSimulatorMode: Boolean,
        currentDeviceAddress: String?,
        fallbackHex: String
    ): String {
        if (context == null) return fallbackHex
        val carId = currentProfileCarId(isSimulatorMode, currentDeviceAddress) ?: return fallbackHex
        return AppPreferences.carProfileColorHex(context, carId, fallbackHex)
    }

    fun openCarProfileEditor(
        context: Context,
        isSimulatorMode: Boolean,
        currentDeviceAddress: String?,
        displayRevName: String,
        simulatorModeController: SimulatorModeController,
        onUiRefresh: () -> Unit,
        onSimulatorProfileSaved: (id: String, name: String, colorHex: String) -> Unit,
        onPrimaryProfileSaved: (carId: String, name: String, colorHex: String) -> Unit
    ) {
        if (isSimulatorMode) {
            simulatorModeController.showEditProfileDialog(context) { identity ->
                onUiRefresh()
                onSimulatorProfileSaved(identity.id, identity.name, identity.colorHex)
            }
            return
        }

        val carId = currentProfileCarId(false, currentDeviceAddress) ?: return
        val initialName = AppPreferences.carProfileName(context, carId, displayRevName)
        val initialColor = AppPreferences.carProfileColorHex(context, carId, AppPreferences.defaultCarColorHex())
        CarProfileEditorDialog.show(
            context = context,
            title = context.getString(R.string.scan_edit_car_profile),
            initialName = initialName,
            initialColorHex = initialColor,
            fallbackDisplayName = displayRevName
        ) { displayName, colorHex ->
            AppPreferences.saveCarProfile(context, carId, displayName, colorHex)
            AppPreferences.setLastPrimaryCarId(context, carId)
            onUiRefresh()
            onPrimaryProfileSaved(carId, displayName, colorHex)
        }
    }
}

