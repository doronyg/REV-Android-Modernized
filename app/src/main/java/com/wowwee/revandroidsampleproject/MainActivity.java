package com.wowwee.revandroidsampleproject;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.wowwee.bluetoothrobotcontrollib.BluetoothRobot;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotFinder;
import com.wowwee.revandroidsampleproject.fragments.KioskLockInterface;
import com.wowwee.revandroidsampleproject.utils.KioskLockManager;
import com.wowwee.revandroidsampleproject.utils.PermissionsFlowHelper;

public class MainActivity extends FragmentActivity {
	private static final String TAG = "MainActivity";
	private final KioskLockManager kioskLockManager = new KioskLockManager();
	@Nullable
	private OnBackInvokedCallback onBackInvokedCallback;

	private final FragmentManager.FragmentLifecycleCallbacks kioskFragmentCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
		@Override
		public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
			syncKioskLockState();
		}

		@Override
		public void onFragmentPaused(@NonNull FragmentManager fm, @NonNull Fragment f) {
			syncKioskLockState();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getSupportFragmentManager().registerFragmentLifecycleCallbacks(kioskFragmentCallbacks, true);
		registerModernBackCallbackIfSupported();

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
		kioskLockManager.onHostResume(this, currentKioskLockTarget());
		// disable idle timer
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onPause() {
		kioskLockManager.onHostPause(this, currentKioskLockTarget());
		super.onPause();
	}

	@Override
	public void onDestroy() {
		unregisterModernBackCallbackIfSupported();
		getSupportFragmentManager().unregisterFragmentLifecycleCallbacks(kioskFragmentCallbacks);
		kioskLockManager.onHostDestroy(this);
		super.onDestroy();
		
		for (REVRobot robot : REVRobotFinder.getInstance().getmRevRobotConnectedList()){
			robot.disconnect();
		}
		
		BluetoothRobot.unbindBluetoothLeService(MainActivity.this);
	}

	private boolean handleBackPress() {
		if (kioskLockManager.onBackPressed(this, currentKioskLockTarget())) {
			Log.d(TAG, "handleBackPress(): consumed by kiosk manager");
			return true;
		}
		Log.d(TAG, "handleBackPress(): using default navigation");
		performDefaultBackNavigation();
		return true;
	}

	private void registerModernBackCallbackIfSupported() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || onBackInvokedCallback != null) {
			return;
		}

		onBackInvokedCallback = this::handleBackPress;
		getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
				OnBackInvokedDispatcher.PRIORITY_DEFAULT,
				onBackInvokedCallback
		);
	}

	private void unregisterModernBackCallbackIfSupported() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || onBackInvokedCallback == null) {
			return;
		}

		getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(onBackInvokedCallback);
		onBackInvokedCallback = null;
	}

	private void syncKioskLockState() {
		kioskLockManager.syncKioskState(this, currentKioskLockTarget());
	}

	public boolean isKioskLockGloballyEnabled() {
		return kioskLockManager.isKioskLockGloballyEnabled(this);
	}

	public void setKioskLockGloballyEnabled(boolean enabled) {
		kioskLockManager.setKioskLockGloballyEnabled(this, enabled, currentKioskLockTarget());
	}

	private void performDefaultBackNavigation() {
		if (!getSupportFragmentManager().popBackStackImmediate()) {
			finish();
		}
	}

	private KioskLockInterface currentKioskLockTarget() {
		Fragment contentFragment = getSupportFragmentManager().findFragmentById(R.id.view_id_content);
		if (contentFragment instanceof KioskLockInterface) {
			return (KioskLockInterface) contentFragment;
		}
		return null;
	}

}
