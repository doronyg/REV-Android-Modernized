package com.wowwee.revandroidsampleproject.simulator;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wowwee.revandroidsampleproject.MainActivity;
import com.wowwee.revandroidsampleproject.R;
import com.wowwee.revandroidsampleproject.carprofile.CarProfileEditorDialog;
import com.wowwee.revandroidsampleproject.utils.AppPreferences;
import com.wowwee.revandroidsampleproject.utils.REVPlayer;

public class SimulatorModeController {

    public interface IdentityConsumer {
        void onIdentityReady(@NonNull SimulatorIdentity identity);
    }

    private static final String SIMULATOR_PROFILE_ID = "SIMULATOR:LOCAL";

    public boolean isEnabled() {
        return REVPlayer.getInstance().isSimulatorMode();
    }

    @Nullable
    public SimulatorIdentity activeIdentity(@Nullable Context context, @NonNull String fallbackName) {
        if (context == null) {
            return null;
        }
        String assignedRevName = AppPreferences.getOrCreateSimulatorAssignedRevName(context);
        String effectiveFallback = assignedRevName != null && !assignedRevName.trim().isEmpty()
                ? assignedRevName
                : fallbackName;
        String name = AppPreferences.carProfileName(context, SIMULATOR_PROFILE_ID, effectiveFallback);
        String colorHex = AppPreferences.carProfileColorHex(context, SIMULATOR_PROFILE_ID, AppPreferences.defaultCarColorHex());
        String id = "SIMULATOR:" + name;
        return new SimulatorIdentity(id, name, colorHex);
    }

    public void connectIfEnabled(@Nullable Context context, @Nullable MainActivity activity, @NonNull String fallbackName) {
        if (!isEnabled()) {
            return;
        }
        SimulatorIdentity identity = activeIdentity(context, fallbackName);
        if (identity == null) {
            return;
        }
        REVPlayer.getInstance().setSimulatorName(identity.getName());
        if (activity != null) {
            activity.onSimulatorIdentityConnected(identity.getId(), identity.getName(), identity.getColorHex());
        }
    }

    public void disable() {
        REVPlayer.getInstance().setSimulatorMode(false);
    }

    @Nullable
    public SimulatorIdentity enableWithStoredIdentity(@Nullable Context context, @NonNull String fallbackName) {
        SimulatorIdentity identity = activeIdentity(context, fallbackName);
        if (identity == null) {
            return null;
        }
        REVPlayer.getInstance().setSimulatorMode(true);
        REVPlayer.getInstance().setPlayerRev(null);
        REVPlayer.getInstance().setSimulatorName(identity.getName());
        return identity;
    }

    public void showEnableDialog(@NonNull Context context, @NonNull IdentityConsumer onEnabled) {
        String defaultSimulatorName = AppPreferences.getOrCreateSimulatorAssignedRevName(context);
        SimulatorIdentity initial = activeIdentity(context, defaultSimulatorName);
        String initialName = initial != null ? initial.getName() : defaultSimulatorName;
        String initialColor = initial != null ? initial.getColorHex() : AppPreferences.defaultCarColorHex();

        CarProfileEditorDialog.show(
            context,
            context.getString(R.string.scan_edit_simulator_profile),
            initialName,
            initialColor,
            defaultSimulatorName,
            (displayName, colorHex) -> {
                AppPreferences.saveCarProfile(context, SIMULATOR_PROFILE_ID, displayName, colorHex);
                REVPlayer.getInstance().setSimulatorMode(true);
                REVPlayer.getInstance().setPlayerRev(null);
                REVPlayer.getInstance().setSimulatorName(displayName);
                onEnabled.onIdentityReady(new SimulatorIdentity("SIMULATOR:" + displayName, displayName, colorHex));
            }
        );
    }

    public void showEditProfileDialog(@NonNull Context context) {
        showEditProfileDialog(context, null);
    }

    public void showEditProfileDialog(@NonNull Context context, @Nullable IdentityConsumer onSaved) {
        String defaultSimulatorName = AppPreferences.getOrCreateSimulatorAssignedRevName(context);
        SimulatorIdentity initial = activeIdentity(context, defaultSimulatorName);
        String initialName = initial != null ? initial.getName() : defaultSimulatorName;
        String initialColor = initial != null ? initial.getColorHex() : AppPreferences.defaultCarColorHex();

        CarProfileEditorDialog.show(
            context,
            context.getString(R.string.scan_edit_simulator_profile),
            initialName,
            initialColor,
            defaultSimulatorName,
            (displayName, colorHex) -> {
                AppPreferences.saveCarProfile(context, SIMULATOR_PROFILE_ID, displayName, colorHex);
                REVPlayer.getInstance().setSimulatorName(displayName);
                if (onSaved != null) {
                    onSaved.onIdentityReady(new SimulatorIdentity("SIMULATOR:" + displayName, displayName, colorHex));
                }
            }
        );
    }
}





