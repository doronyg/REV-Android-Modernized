package com.wowwee.revandroidsampleproject;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.view.WindowManager;

import com.wowwee.bluetoothrobotcontrollib.BluetoothRobot;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotFinder;
import com.wowwee.revandroidsampleproject.utils.PermissionsFlowHelper;

public class MainActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		PermissionsFlowHelper.openPermissionsFragment(this);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (!PermissionsFlowHelper.isBluetoothEnabled(this) || !PermissionsFlowHelper.hasRequiredBluetoothPermissions(this)) {
			PermissionsFlowHelper.openPermissionsFragment(this);

		} else {
			PermissionsFlowHelper.openScanFragment(this);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		for (REVRobot robot : REVRobotFinder.getInstance().getmRevRobotConnectedList()){
			robot.disconnect();
		}
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
	}
	
}
