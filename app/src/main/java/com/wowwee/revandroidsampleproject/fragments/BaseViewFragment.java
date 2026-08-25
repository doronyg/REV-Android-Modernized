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
