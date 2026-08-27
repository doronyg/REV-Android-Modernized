package com.wowwee.revandroidsampleproject.fragments;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotFinder;
import com.wowwee.revandroidsampleproject.R;
import com.wowwee.revandroidsampleproject.utils.AppPreferences;

public abstract class BaseViewFragment extends Fragment {
	protected Rect viewRect;

	@Nullable
	public REVRobot rev;
	
	public static FragmentActivity activity;
	
	public BaseViewFragment() {
		rev = REVRobotFinder.getInstance().firstConnectedREV();
	}

	abstract protected int layoutId();

	public static FragmentActivity getFragmentActivity(){
		return activity;
	}

	public boolean isKioskLockDisabledByUser() {
		FragmentActivity hostActivity = getActivity();
		if (hostActivity instanceof KioskLockHost) {
			return ((KioskLockHost) hostActivity).isKioskLockDisabledByUser();
		}
		return AppPreferences.isKioskLockDisabledByUser(getContext());
	}

	public void setKioskLockDisabledByUser(boolean disabled) {
		FragmentActivity hostActivity = getActivity();
		if (hostActivity instanceof KioskLockHost) {
			((KioskLockHost) hostActivity).setKioskLockDisabledByUser(disabled);
			return;
		}
		AppPreferences.setKioskLockDisabledByUser(getContext(), disabled);
	}

	public int kioskModeToggleLabelResId() {
		return isKioskLockDisabledByUser()
				? R.string.driver_mode_enable_kiosk
				: R.string.driver_mode_disable_kiosk;
	}

	public void toggleKioskLockDisabledByUser() {
		setKioskLockDisabledByUser(!isKioskLockDisabledByUser());
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

}
