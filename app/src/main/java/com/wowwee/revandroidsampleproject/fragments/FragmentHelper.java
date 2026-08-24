package com.wowwee.revandroidsampleproject.fragments;

import android.util.Log;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class FragmentHelper {
	private static final String TAG = "FragmentHelper";
	public static List<WeakReference<Fragment>> fragments = new ArrayList<WeakReference<Fragment>>();
	
	private static ArrayList<String> backStackKeys = new ArrayList<String>();
	private static int backStackIndex = 0;
	
	public static void switchFragment(FragmentManager fragmentManager, Fragment fragment, int containViewId, boolean addToBackStack) {
		if (fragmentManager == null || fragment == null) {
			Log.w(TAG, "switchFragment(): fragmentManager/fragment is null, skip.");
			return;
		}

		// Clean dead references first to keep the tracking list meaningful.
		for (int i = fragments.size() - 1; i >= 0; i--) {
			if (fragments.get(i).get() == null) {
				fragments.remove(i);
			}
		}

		boolean isContain = false;
		for (int i = 0; i < fragments.size(); i++){
			if (fragments.get(i).get() != null && fragments.get(i).get().getClass() == fragment.getClass()){
				isContain = true;
				break;
			}
		}
		if (!isContain) {
			fragments.add(new WeakReference<Fragment>(fragment));
		}
		
		if (fragment.getView() != null) {
			fragment.getView().setClickable(true);
		}
		
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		String tag = "" + backStackIndex;
		if (addToBackStack) {
			backStackIndex++;
			String key = "" + backStackIndex;
			transaction.addToBackStack(key);
			backStackKeys.add(key);
			tag = key;
		}
		transaction.replace(containViewId, fragment, tag);

		try {
			if (fragmentManager.isStateSaved()) {
				Log.w(TAG, "switchFragment(): state is already saved; using commitAllowingStateLoss for " + fragment.getClass().getSimpleName());
				transaction.commitAllowingStateLoss();
			} else {
				transaction.commit();
			}
		} catch (IllegalStateException ex) {
			Log.w(TAG, "switchFragment(): commit failed, fallback to commitAllowingStateLoss for " + fragment.getClass().getSimpleName(), ex);
			transaction.commitAllowingStateLoss();
		}

		Log.d(TAG, "switchFragment(): switched to " + fragment.getClass().getSimpleName() + ", container=" + containViewId + ", addToBackStack=" + addToBackStack + ", tag=" + tag);
	}

	public static void removeFragment(FragmentManager fragmentManager, int containViewId) {
		if(fragmentManager != null) {
			Fragment fragment = fragmentManager.findFragmentById(containViewId);
			if (fragment != null) {
				for (int i = fragments.size() - 1; i >= 0; i--) {
					Fragment tracked = fragments.get(i).get();
					if (tracked == null || tracked == fragment) {
						fragments.remove(i);
					}
				}
				FragmentTransaction transaction = fragmentManager.beginTransaction();
				transaction.remove(fragment);
				if (fragmentManager.isStateSaved()) {
					transaction.commitAllowingStateLoss();
				} else {
					transaction.commit();
				}
				Log.d(TAG, "removeFragment(): removed " + fragment.getClass().getSimpleName() + " from container=" + containViewId);
			}
		}
	}
}
