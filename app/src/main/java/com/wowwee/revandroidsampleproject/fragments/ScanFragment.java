package com.wowwee.revandroidsampleproject.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot;
import com.wowwee.revandroidsampleproject.MainActivity;
import com.wowwee.revandroidsampleproject.R;
import com.wowwee.revandroidsampleproject.robot.REVRobotEvent;
import com.wowwee.revandroidsampleproject.robot.REVRobotEventBus;
import com.wowwee.revandroidsampleproject.simulator.SimulatorIdentity;
import com.wowwee.revandroidsampleproject.simulator.SimulatorModeController;
import com.wowwee.revandroidsampleproject.utils.AppPreferences;
import com.wowwee.revandroidsampleproject.utils.PermissionsFlowHelper;
import com.wowwee.revandroidsampleproject.utils.REVPlayer;
import com.wowwee.revandroidsampleproject.utils.RevConnectionStateMachine;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ScanFragment extends BaseViewFragment {

    private static final String TAG = "REV-ScanFragment";

    private final RevConnectionStateMachine scanStateMachine = RevConnectionStateMachine.getInstance();
    private final SimulatorModeController simulatorController = new SimulatorModeController();

    private TextView tvScanStatus;
    private Button btnScanRetry;
    private Button btnScanDiscovery;
    private Button btnScanSimulator;
    private boolean shouldShowDiscoveryButton;
    private final CompositeDisposable revEventDisposables = new CompositeDisposable();
    private final CompositeDisposable connectionUiDisposables = new CompositeDisposable();

    public ScanFragment() {
    }

    @Override
    protected int layoutId() {
        return R.layout.fragment_scan;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null)
            return null;

        View view = super.onCreateView(inflater, container, savedInstanceState);
        tvScanStatus = view.findViewById(R.id.tvScanStatus);
        btnScanRetry = view.findViewById(R.id.btnScanRetry);
        btnScanDiscovery = view.findViewById(R.id.btnScanDiscovery);
        btnScanSimulator = view.findViewById(R.id.btnScanSimulator);
        btnScanRetry.setOnClickListener(v -> scanStateMachine.retry());
        btnScanDiscovery.setOnClickListener(v -> PermissionsFlowHelper.openDiscoveryFragment(getActivity()));
        btnScanSimulator.setOnClickListener(v -> openSimulatorMode());
        updateDiscoveryButtonVisibility();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() start.");

        if (simulatorController.isEnabled()) {
            updateScanStatus(getString(R.string.scan_status_simulator_ready), false);
            shouldShowDiscoveryButton = false;
            updateDiscoveryButtonVisibility();
            btnScanSimulator.setText(R.string.scan_disable_simulator);
            simulatorController.connectIfEnabled(getActivity(), asMainActivity(), getString(R.string.scan_simulator_default_name));
            return;
        }

        btnScanSimulator.setText(R.string.scan_open_simulator);
        shouldShowDiscoveryButton = !AppPreferences.hasConnectedRevBefore(getActivity());
        updateDiscoveryButtonVisibility();
        bindConnectionUiEvents();
        bindRevEvents();
        scanStateMachine.start(getActivity(), REVRobotEventBus.callbackInterface());
    }

    @Override
    public void onPause() {
        super.onPause();
        revEventDisposables.clear();
        connectionUiDisposables.clear();
        if (!simulatorController.isEnabled()) {
            scanStateMachine.stop();
        }
    }

    private void openSimulatorMode() {
        if (simulatorController.isEnabled()) {
            simulatorController.disable();
            updateScanStatus(getString(R.string.scan_status_preparing), false);
            shouldShowDiscoveryButton = !AppPreferences.hasConnectedRevBefore(getActivity());
            updateDiscoveryButtonVisibility();
            btnScanSimulator.setText(R.string.scan_open_simulator);
            bindConnectionUiEvents();
            bindRevEvents();
            scanStateMachine.start(getActivity(), REVRobotEventBus.callbackInterface());
            return;
        }

        SimulatorIdentity identity = simulatorController.enableWithStoredIdentity(getActivity(), getString(R.string.scan_simulator_default_name));
        if (identity == null) {
            return;
        }
        MainActivity activity = asMainActivity();
        if (activity != null) {
            activity.onSimulatorIdentityConnected(identity.getId(), identity.getName(), identity.getColorHex());
        }
        scanStateMachine.stop();
        btnScanSimulator.setText(R.string.scan_disable_simulator);
        FragmentHelper.switchFragment(getFragmentActivity().getSupportFragmentManager(), AdvancedDrivingFragment.newInstance(null), R.id.view_id_content, false);
    }

    private void updateScanStatus(String status, boolean showRetry) {
        if (tvScanStatus != null) {
            tvScanStatus.setText(status);
        }
        if (btnScanRetry != null) {
            btnScanRetry.setVisibility(showRetry ? View.VISIBLE : View.GONE);
        }
        updateDiscoveryButtonVisibility();
    }

    private void bindRevEvents() {
        if (revEventDisposables.size() > 0) {
            return;
        }

        revEventDisposables.add(
            REVRobotEventBus.getEvents()
                .observeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    event -> {
                        if (event instanceof REVRobotEvent.DeviceReady) {
                            scanStateMachine.onRobotReady(((REVRobotEvent.DeviceReady) event).getRobot());
                        }
                    },
                    error -> Log.e(TAG, "REV event stream error", error)
                )
        );
    }

    private void bindConnectionUiEvents() {
        if (connectionUiDisposables.size() > 0) {
            return;
        }

        connectionUiDisposables.add(
            scanStateMachine.observeScanUiState()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    state -> updateScanStatus(state.status, state.showRetry),
                    error -> Log.e(TAG, "Scan state stream error", error)
                )
        );

        connectionUiDisposables.add(
            scanStateMachine.observeUiEvents()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    event -> {
                        if (event.type == RevConnectionStateMachine.UiEventType.REQUEST_PERMISSIONS
                                || event.type == RevConnectionStateMachine.UiEventType.REQUEST_ENABLE_BLUETOOTH) {
                            PermissionsFlowHelper.openPermissionsFragment(getActivity());
                        } else if (event.type == RevConnectionStateMachine.UiEventType.DISCOVERY_RECOMMENDED) {
                            shouldShowDiscoveryButton = true;
                            updateDiscoveryButtonVisibility();
                        } else if (event.type == RevConnectionStateMachine.UiEventType.NAVIGATE_TO_DRIVER_MODE && event.robot != null) {
                            navigateToDriverMode(event.robot);
                        }
                    },
                    error -> Log.e(TAG, "Scan UI event stream error", error)
                )
        );
    }

    private void navigateToDriverMode(REVRobot connectedRev) {
        REVPlayer.getInstance().setPlayerRev(connectedRev);
        AppPreferences.markHasConnectedRev(getActivity());
        String connectedRevAddress = safeRevAddress(connectedRev);
        String connectedRevName = connectedRev != null ? connectedRev.getName() : null;
        String fallbackName = connectedRevName != null && !connectedRevName.trim().isEmpty()
            ? connectedRevName
            : getString(R.string.scan_profile_default_car_name);
        AppPreferences.setLastPrimaryCarId(getActivity(), connectedRevAddress);
        if (getActivity() instanceof MainActivity) {
            String profileName = connectedRevAddress != null && AppPreferences.hasCarProfile(getActivity(), connectedRevAddress)
                ? AppPreferences.carProfileName(getActivity(), connectedRevAddress, fallbackName)
                : fallbackName;
            String profileColor = connectedRevAddress != null && AppPreferences.hasCarProfile(getActivity(), connectedRevAddress)
                ? AppPreferences.carProfileColorHex(getActivity(), connectedRevAddress, AppPreferences.defaultCarColorHex())
                : AppPreferences.defaultCarColorHex();
            ((MainActivity) getActivity()).onPrimaryRevConnected(connectedRevAddress, profileName, profileColor);
        }
        FragmentHelper.switchFragment(getFragmentActivity().getSupportFragmentManager(), AdvancedDrivingFragment.newInstance(connectedRevAddress), R.id.view_id_content, false);
    }

    private String safeRevAddress(REVRobot rev) {
        if (rev == null || rev.getBluetoothDevice() == null) {
            return null;
        }

        try {
            return rev.getBluetoothDevice().getAddress();
        } catch (SecurityException ex) {
            return null;
        }
    }

    private void updateDiscoveryButtonVisibility() {
        if (btnScanDiscovery == null) {
            return;
        }
        btnScanDiscovery.setVisibility(shouldShowDiscoveryButton ? View.VISIBLE : View.GONE);
    }

    private MainActivity asMainActivity() {
        return getActivity() instanceof MainActivity ? (MainActivity) getActivity() : null;
    }
}
