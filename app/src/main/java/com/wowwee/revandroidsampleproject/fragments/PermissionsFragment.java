package com.wowwee.revandroidsampleproject.fragments;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.AndroidRuntimeException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.wowwee.revandroidsampleproject.R;
import com.wowwee.revandroidsampleproject.utils.PermissionsFlowHelper;

public class PermissionsFragment extends Fragment {

    private static final int REQUEST_CODE_BLUETOOTH_PERMISSIONS = 2001;

    private TextView tvStatus;
    private Button btnGrantPermissions;
    private Button btnEnableBluetooth;

    @Override
    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }

        View view = inflater.inflate(R.layout.fragment_permissions, container, false);
        tvStatus = view.findViewById(R.id.tvPermissionStatus);
        btnGrantPermissions = view.findViewById(R.id.btnGrantPermissions);
        btnEnableBluetooth = view.findViewById(R.id.btnEnableBluetooth);

        btnGrantPermissions.setOnClickListener(v -> requestPermissions(PermissionsFlowHelper.requiredRuntimePermissions(), REQUEST_CODE_BLUETOOTH_PERMISSIONS));

        btnEnableBluetooth.setOnClickListener(v -> requestBluetoothEnable());

        refreshUiState();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUiState();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_BLUETOOTH_PERMISSIONS) {
            refreshUiState();
        }
    }

    private void refreshUiState() {
        if (getActivity() == null) {
            return;
        }

        if (!PermissionsFlowHelper.hasRequiredBluetoothPermissions(getActivity())) {
            tvStatus.setText(getString(R.string.permissions_status_permissions_required));
            btnGrantPermissions.setVisibility(View.VISIBLE);
            btnEnableBluetooth.setVisibility(View.GONE);
            return;
        }

        if (!PermissionsFlowHelper.isBluetoothEnabled(getActivity())) {
            tvStatus.setText(getString(R.string.permissions_status_bluetooth_required));
            btnGrantPermissions.setVisibility(View.GONE);
            btnEnableBluetooth.setVisibility(View.VISIBLE);
            return;
        }

        tvStatus.setText(getString(R.string.permissions_status_ready));
        btnGrantPermissions.setVisibility(View.GONE);
        btnEnableBluetooth.setVisibility(View.GONE);
        PermissionsFlowHelper.openScanFragment(getActivity());
    }

    private void requestBluetoothEnable() {
        try {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableIntent);
        } catch (ActivityNotFoundException | AndroidRuntimeException ex) {
            askBluetoothActivationManually();
        }
    }

    private void askBluetoothActivationManually() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(true);
        builder.setMessage("bluetooth_enable_question");
        builder.setTitle("bluetooth_enable_dialog_title");
        builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
            try {
                BluetoothAdapter.getDefaultAdapter().enable();
            } catch (SecurityException ex) {
                refreshUiState();
            }
        });
        builder.setNegativeButton(android.R.string.no, null);
        builder.show();
    }
}
