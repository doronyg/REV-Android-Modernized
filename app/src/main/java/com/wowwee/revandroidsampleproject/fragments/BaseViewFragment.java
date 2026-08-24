package com.wowwee.revandroidsampleproject.fragments;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.wowwee.bluetoothrobotcontrollib.RobotCommand;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot.REVRobotInterface;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotFinder;
import com.wowwee.bluetoothrobotcontrollib.rev.REVTrackingStatus;

import java.util.ArrayList;

public abstract class BaseViewFragment extends Fragment implements REVRobotInterface {
	protected Rect viewRect;

	public REVRobot rev;
	
	public static FragmentActivity activity;
	
	public BaseViewFragment() {
		rev = REVRobotFinder.getInstance().firstConnectedREV();
		if(rev != null) {
			rev.setCallbackInterface(this);
		}
	}

	abstract protected int layoutId();

	public static FragmentActivity getFragmentActivity(){
		return activity;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null)
			return null;
		
		viewRect = new Rect();
		if (getActivity() != null){
			activity = getActivity();
		}
		activity.getWindowManager().getDefaultDisplay().getRectSize(viewRect);
		
		View view;
		int layoutId = layoutId();
		if (layoutId == -1) {
			view = super.onCreateView(inflater, container, savedInstanceState);
		} else {
			view = inflater.inflate(layoutId, container, false);
		}
		
		return view;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(null);
	}

	@Override
	public void revDeviceReady(REVRobot rev) {

	}

	@Override
	public void revDeviceDisconnected(REVRobot rev) {

	}

	@Override
	public void revDidReceiveBatteryInfo(REVRobot rev, int batteryLevel, int batteryType) {

	}

	@Override
	public void revDidReceiveHardwareVersion(REVRobot rev, int voiceChipVerison, int irChipVersion) {

	}

	@Override
	public void revDidReceiveToyActivationStatus(REVRobot mip, boolean activated, boolean uploadedActivation) {

	}

	@Override
	public void revDidReceiveVolumeLevel(REVRobot rev, int mipVolume) {

	}

	@Override
	public void revDidReceiveIRCommand(REVRobot rev, byte irCommand, byte rxSensor) {

	}

	@Override
	public void revDidReceiveTrackingMode(REVRobot rev, byte mode) {

	}

	@Override
	public void revDidReceiveTrackingStatus(REVTrackingStatus status) {

	}

	@Override
	public void revDidReceiveTrackingUpdateStatus(REVRobot rev, byte status) {

	}

	@Override
	public void revDidReceiveTrackingDistanceAndSpeed(REVRobot rev, byte distance, byte speed) {

	}

	@Override
	public void revDidReceiveCurrentLEDColor(REVRobot rev, byte color) {

	}

	@Override
	public void revDidReceiveSoftwareVersion(REVRobot rev, String softwareVersion, String bootloaderVersion) {

	}

	@Override
	public void revDidReceiveCurrentTraction(REVRobot rev, byte traction) {

	}

	@Override
	public void revDidReceiveUserStatus(REVRobot rev, byte userDataAddress, byte data) {

	}

	@Override
	public void revDidReceiveBumpNotify(REVRobot rev) {

	}

	@Override
	public void revDidReceiveRawData(REVRobot rev, ArrayList<Byte> data) {

	}

	@Override
	public boolean revBluetoothDidProcessedReceiveRobotCommand(REVRobot rev, RobotCommand command) {
		return false;
	}

	@Override
	public void revSpecialBroadcastIDChanged(REVRobot rev) {

	}

	@Override
	public void revRobotDidJumpedOverRamp(REVRobot rev) {

	}

	@Override
	public void revAvatarIconBroadcastDriverIDChanged(REVRobot rev) {

	}
}
