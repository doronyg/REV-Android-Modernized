package com.wowwee.revandroidsampleproject;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.wowwee.bluetoothrobotcontrollib.BluetoothRobot;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotFinder;
import com.wowwee.revandroidsampleproject.fragments.KioskLockHost;
import com.wowwee.revandroidsampleproject.fragments.KioskLockInterface;
import com.wowwee.revandroidsampleproject.fragments.ScanFragment;
import com.wowwee.revandroidsampleproject.pvp.GameSessionCoordinator;
import com.wowwee.revandroidsampleproject.robot.REVRobotEventBus;
import com.wowwee.revandroidsampleproject.utils.KioskLockManager;
import com.wowwee.revandroidsampleproject.utils.PermissionsFlowHelper;
import com.wowwee.revandroidsampleproject.utils.RevConnectionStateMachine;
import com.wowwee.revandroidsampleproject.utils.SoundEffects;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class MainActivity extends FragmentActivity implements KioskLockHost {
	private static final String TAG = "MainActivity";
	private final KioskLockManager kioskLockManager = new KioskLockManager();
	private final CompositeDisposable connectionUiDisposables = new CompositeDisposable();
	@Nullable
	private OnBackInvokedCallback onBackInvokedCallback;

	public MainActivity() {
		super();
		SoundEffects.INSTANCE.warmUpCache();
	}

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
		RevConnectionStateMachine.getInstance().bindAppLevelRevEvents();
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
		GameSessionCoordinator.onCarDisconnected();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		kioskLockManager.onHostResume(this, currentKioskLockTarget());
		GameSessionCoordinator.onHostResumed();
		REVRobotEventBus.attachToConnectedRobots();
		bindConnectionUiEvents();
		syncFromConnectionStateMachine();
		// disable idle timer
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onPause() {
		kioskLockManager.onHostPause(this, currentKioskLockTarget());
		GameSessionCoordinator.onHostPaused();
		connectionUiDisposables.clear();
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
		GameSessionCoordinator.onCarDisconnected();
		RevConnectionStateMachine.getInstance().unbindAppLevelRevEvents();

		BluetoothRobot.unbindBluetoothLeService(MainActivity.this);
	}

	public void onPrimaryRevConnected(@Nullable String revId) {
		if (revId == null || revId.trim().isEmpty()) {
			return;
		}
		REVRobotEventBus.attachToConnectedRobots();
		GameSessionCoordinator.onCarConnected(revId);
	}

	public void onSimulatorIdentityConnected(@Nullable String simulatorId) {
		if (simulatorId == null || simulatorId.trim().isEmpty()) {
			return;
		}
		REVRobotEventBus.attachToConnectedRobots();
		GameSessionCoordinator.onCarConnected(simulatorId, 8888, true);
	}

	public void onPrimaryRevDisconnected() {
		GameSessionCoordinator.onCarDisconnected();
		Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.view_id_content);
		if (!(currentFragment instanceof ScanFragment)) {
			PermissionsFlowHelper.openScanFragment(this);
		}
		RevConnectionStateMachine.getInstance().acknowledgePrimaryDisconnectUiHandled();
	}

	private void bindConnectionUiEvents() {
		if (connectionUiDisposables.size() > 0) {
			return;
		}

		connectionUiDisposables.add(
				RevConnectionStateMachine.getInstance().observeUiEvents()
						.observeOn(AndroidSchedulers.mainThread())
						.subscribe(event -> {
							if (event.type == RevConnectionStateMachine.UiEventType.PRIMARY_REV_DISCONNECTED) {
								onPrimaryRevDisconnected();
							}
						}, error -> Log.e(TAG, "Connection UI event stream error", error))
		);
	}

	private void syncFromConnectionStateMachine() {
		RevConnectionStateMachine stateMachine = RevConnectionStateMachine.getInstance();
		if (stateMachine.consumePrimaryDisconnectUiPending()) {
			onPrimaryRevDisconnected();
		}
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

	@Override
	public boolean isKioskLockDisabledByUser() {
		return kioskLockManager.isKioskLockDisabledByUser(getApplicationContext());
	}

	public void setKioskLockGloballyEnabled(boolean enabled) {
		kioskLockManager.setKioskLockGloballyEnabled(this, enabled, currentKioskLockTarget());
	}

	@Override
	public void setKioskLockDisabledByUser(boolean disabled) {
		kioskLockManager.setKioskLockDisabledByUser(this, disabled, currentKioskLockTarget());
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
