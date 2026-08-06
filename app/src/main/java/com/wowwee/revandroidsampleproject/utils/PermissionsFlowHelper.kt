package com.wowwee.revandroidsampleproject.utils

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import com.wowwee.revandroidsampleproject.R
import com.wowwee.revandroidsampleproject.fragments.FragmentHelper
import com.wowwee.revandroidsampleproject.fragments.PermissionsFragment
import com.wowwee.revandroidsampleproject.fragments.ScanFragment

object PermissionsFlowHelper {

    @JvmStatic
    fun requiredRuntimePermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

    @JvmStatic
    fun hasRequiredBluetoothPermissions(context: Context?): Boolean =
        context != null && requiredRuntimePermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    @JvmStatic
    fun isBluetoothEnabled(context: Context?): Boolean {
        val adapter = (context?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        if (adapter != null) {
            return adapter.isEnabled
        }

        @Suppress("DEPRECATION")
        return BluetoothAdapter.getDefaultAdapter()?.isEnabled == true
    }

    @JvmStatic
    fun openPermissionsFragment(activity: FragmentActivity?) =
        openContentFragment(activity, PermissionsFragment())

    @JvmStatic
    fun openScanFragment(activity: FragmentActivity?) =
        openContentFragment(activity, ScanFragment())

    private fun openContentFragment(activity: FragmentActivity?, fragment: Fragment) {
        activity?.let {
            FragmentHelper.switchFragment(it.supportFragmentManager, fragment, R.id.view_id_content, false)
        }
    }
}


