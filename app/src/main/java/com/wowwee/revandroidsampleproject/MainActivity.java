package com.wowwee.revandroidsampleproject;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.WindowManager;
import com.wowwee.bluetoothrobotcontrollib.BluetoothRobot;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotFinder;
import com.wowwee.revandroidsampleproject.fragments.FragmentHelper;
import com.wowwee.revandroidsampleproject.fragments.ScanFragment;

public class MainActivity extends FragmentActivity {

	private static final int REQUEST_CODE_BLUETOOTH_PERMISSIONS = 2001;
	private static final String TAG = "REV-MainActivity";
	private boolean didStartBluetoothFlow = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		ensureBluetoothPermissionsAndStart();
	}

	public void ensureBluetoothPermissionsAndStart() {
		Log.d(TAG, "ensureBluetoothPermissionsAndStart() called; didStartBluetoothFlow=" + didStartBluetoothFlow + ", sdk=" + Build.VERSION.SDK_INT);
		if (didStartBluetoothFlow) {
			Log.d(TAG, "Bluetooth flow already started; skipping.");
			return;
		}

		if (hasRequiredBluetoothPermissions()) {
			Log.d(TAG, "All required Bluetooth runtime permissions are granted.");
			startBluetoothFlow();
			return;
		}

		Log.d(TAG, "Requesting runtime permissions: " + java.util.Arrays.toString(requiredRuntimePermissions()));
		ActivityCompat.requestPermissions(this, requiredRuntimePermissions(), REQUEST_CODE_BLUETOOTH_PERMISSIONS);
	}

	private boolean hasRequiredBluetoothPermissions() {
		for (String permission : requiredRuntimePermissions()) {
			int status = ContextCompat.checkSelfPermission(this, permission);
			Log.d(TAG, "Permission check: " + permission + " => " + (status == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));
			if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
				return false;
			}
		}
		return true;
	}

	private String[] requiredRuntimePermissions() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			return new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
		}
        // BLE scan on Android 6-11 requires a location runtime permission.
        return new String[]{Manifest.permission.ACCESS_COARSE_LOCATION};
    }

	private void startBluetoothFlow() {
		if (didStartBluetoothFlow) {
			Log.d(TAG, "startBluetoothFlow() ignored; already started.");
			return;
		}
		didStartBluetoothFlow = true;
		Log.d(TAG, "Starting Bluetooth flow; creating ScanFragment.");
		FragmentHelper.switchFragment(getSupportFragmentManager(), new ScanFragment(), R.id.view_id_content, false);
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		Log.d(TAG, "BluetoothAdapter.getDefaultAdapter() returned " + (adapter == null ? "null" : "non-null") + ", enabled=" + (adapter != null && adapter.isEnabled()));
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode != REQUEST_CODE_BLUETOOTH_PERMISSIONS) {
			return;
		}

		Log.d(TAG, "onRequestPermissionsResult() received for Bluetooth request.");
		boolean granted = grantResults.length > 0;
		for (int i = 0; i < grantResults.length; i++) {
			String permissionName = (permissions != null && permissions.length > i) ? permissions[i] : "<unknown>";
			Log.d(TAG, "Permission result: " + permissionName + " => " + (grantResults[i] == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));
			int grantResult = grantResults[i];
			if (grantResult != PackageManager.PERMISSION_GRANTED) {
				granted = false;
				break;
			}
		}

		if (granted) {
			Log.d(TAG, "Permission request granted; proceeding with Bluetooth flow.");
			startBluetoothFlow();
		} else {
			Log.w(TAG, "Required Bluetooth permissions were denied.");
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		for (REVRobot robot : REVRobotFinder.getInstance().getmRevRobotConnectedList()){
			robot.disconnect();
		}
		System.exit(0);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		// disable idle timer
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		for (REVRobot robot : REVRobotFinder.getInstance().getmRevRobotConnectedList()){
			robot.disconnect();
		}
		
		BluetoothRobot.unbindBluetoothLeService(MainActivity.this);
		
		System.exit(0);
	}
	
}
