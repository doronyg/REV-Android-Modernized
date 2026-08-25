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
import com.wowwee.revandroidsampleproject.utils.AppPreferences;
import com.wowwee.revandroidsampleproject.utils.PermissionsFlowHelper;
import com.wowwee.revandroidsampleproject.utils.REVPlayer;
import com.wowwee.revandroidsampleproject.utils.RevScanStateMachine;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ScanFragment extends BaseViewFragment {

    private static final String TAG = "REV-ScanFragment";

    private final RevScanStateMachine scanStateMachine = RevScanStateMachine.getInstance();

    private TextView tvScanStatus;
    private Button btnScanRetry;
    private Button btnScanDiscovery;
    private Button btnScanSimulator;
    private boolean shouldShowDiscoveryButton;
    private final CompositeDisposable revEventDisposables = new CompositeDisposable();

    private final RevScanStateMachine.Listener scanListener = new RevScanStateMachine.Listener() {
        @Override
        public void onPermissionsRequired() {
            PermissionsFlowHelper.openPermissionsFragment(getActivity());
        }

        @Override
        public void onBluetoothEnableRequired() {
            PermissionsFlowHelper.openPermissionsFragment(getActivity());
        }

        @Override
        public void onScanStatusChanged(String status, boolean showRetry) {
            updateScanStatus(status, showRetry);
        }

        @Override
        public void onDiscoveryRecommended() {
            shouldShowDiscoveryButton = true;
            updateDiscoveryButtonVisibility();
        }

        @Override
        public void onNavigateToDriverMode(REVRobot connectedRev) {
            REVPlayer.getInstance().setPlayerRev(connectedRev);
            AppPreferences.markHasConnectedRev(getActivity());
            String connectedRevAddress = safeRevAddress(connectedRev);
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).onPrimaryRevConnected(connectedRevAddress);
            }
            FragmentHelper.switchFragment(getFragmentActivity().getSupportFragmentManager(), AdvancedDrivingFragment.newInstance(connectedRevAddress), R.id.view_id_content, false);
        }
    };

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

        if (REVPlayer.getInstance().isSimulatorMode()) {
            updateScanStatus(getString(R.string.scan_status_simulator_ready), false);
            shouldShowDiscoveryButton = false;
            updateDiscoveryButtonVisibility();
            btnScanSimulator.setText(R.string.scan_disable_simulator);
            if (getActivity() instanceof MainActivity) {
                String simulatorId = "SIMULATOR:" + REVPlayer.getInstance().getSimulatorName();
                ((MainActivity) getActivity()).onSimulatorIdentityConnected(simulatorId);
            }
            return;
        }

        btnScanSimulator.setText(R.string.scan_open_simulator);
        shouldShowDiscoveryButton = !AppPreferences.hasConnectedRevBefore(getActivity());
        updateDiscoveryButtonVisibility();
        bindRevEvents();
        scanStateMachine.start(getActivity(), scanListener, REVRobotEventBus.callbackInterface());
    }

    @Override
    public void onPause() {
        super.onPause();
        revEventDisposables.clear();
        if (!REVPlayer.getInstance().isSimulatorMode()) {
            scanStateMachine.stop();
        }
    }

    private void openSimulatorMode() {
        if (REVPlayer.getInstance().isSimulatorMode()) {
            REVPlayer.getInstance().setSimulatorMode(false);
            updateScanStatus(getString(R.string.scan_status_preparing), false);
            shouldShowDiscoveryButton = !AppPreferences.hasConnectedRevBefore(getActivity());
            updateDiscoveryButtonVisibility();
            btnScanSimulator.setText(R.string.scan_open_simulator);
            bindRevEvents();
            scanStateMachine.start(getActivity(), scanListener, REVRobotEventBus.callbackInterface());
            return;
        }

        REVPlayer.getInstance().setSimulatorMode(true);
        REVPlayer.getInstance().setPlayerRev(null);
        REVPlayer.getInstance().setSimulatorName(getString(R.string.scan_simulator_default_name));
        if (getActivity() instanceof MainActivity) {
            String simulatorId = "SIMULATOR:" + REVPlayer.getInstance().getSimulatorName();
            ((MainActivity) getActivity()).onSimulatorIdentityConnected(simulatorId);
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
                        } else if (event instanceof REVRobotEvent.DeviceDisconnected) {
                            scanStateMachine.onRobotDisconnected();
                        }
                    },
                    error -> Log.e(TAG, "REV event stream error", error)
                )
        );
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
}
