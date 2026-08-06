package com.wowwee.revandroidsampleproject.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot;
import com.wowwee.revandroidsampleproject.R;
import com.wowwee.revandroidsampleproject.utils.PermissionsFlowHelper;
import com.wowwee.revandroidsampleproject.utils.REVPlayer;
import com.wowwee.revandroidsampleproject.utils.RevScanStateMachine;

public class ScanFragment extends BaseViewFragment {

    private static final String TAG = "REV-ScanFragment";
    private final RevScanStateMachine scanStateMachine = RevScanStateMachine.getInstance();
    private TextView tvScanStatus;
    private Button btnScanRetry;
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
        public void onNavigateToDriverMode(REVRobot connectedRev) {
            REVPlayer.getInstance().setPlayerRev(connectedRev);
            String connectedRevAddress = safeRevAddress(connectedRev);
            FragmentHelper.switchFragment(getFragmentActivity().getSupportFragmentManager(), DriverModeFragment.newInstance(connectedRevAddress), R.id.view_id_content, false);
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
        btnScanRetry.setOnClickListener(v -> scanStateMachine.retry());
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() start.");
        scanStateMachine.start(getActivity(), scanListener, this);
    }

    @Override
    public void onPause() {
        super.onPause();
        scanStateMachine.stop();
    }

    private void updateScanStatus(String status, boolean showRetry) {
        if (tvScanStatus != null) {
            tvScanStatus.setText(status);
        }
        if (btnScanRetry != null) {
            btnScanRetry.setVisibility(showRetry ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void revDeviceReady(REVRobot rev) {
        scanStateMachine.onRobotReady(rev);
    }

    @Override
    public void revDeviceDisconnected(REVRobot rev) {
        scanStateMachine.onRobotDisconnected();
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
}
