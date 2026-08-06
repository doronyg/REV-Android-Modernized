package com.wowwee.revandroidsampleproject.utils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.lang.ref.WeakReference

object BleDiscoveryScanner {

    interface Listener {
        fun onScanStateChanged(scanning: Boolean, message: String)
        fun onDeviceDetected(device: BluetoothDevice, rssi: Int, connectable: Boolean, source: String)
    }

    private const val TAG = "REV-BleDiscovery"
    private const val NO_RESULT_FALLBACK_MS = 4000L

    private val mainHandler = Handler(Looper.getMainLooper())
    private var listenerRef: WeakReference<Listener> = WeakReference(null)

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanning = false
    private var usingLegacyScan = false
    private var callbackEventCount = 0L
    private var scanStartEventCount = 0L

    @JvmStatic
    fun getInstance(): BleDiscoveryScanner = this

    private val noResultFallbackRunnable = Runnable {
        if (!scanning || usingLegacyScan) return@Runnable
        if (callbackEventCount != scanStartEventCount) return@Runnable

        Log.w(TAG, "No scan callbacks after ${NO_RESULT_FALLBACK_MS}ms; switching to legacy scanner.")
        notifyState(true, "No results yet, trying legacy scan fallback...")
        startLegacyScan()
    }

    private val legacyLeScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, _ ->
        dispatchDevice(device, rssi, false, "legacy")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return
            dispatchDevice(result.device, result.rssi, result.isConnectable, "scanner")
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { result ->
                dispatchDevice(result.device, result.rssi, result.isConnectable, "scanner-batch")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "onScanFailed(): $errorCode")
            notifyState(true, "Modern scan failed ($errorCode), trying legacy fallback...")
            startLegacyScan()
        }
    }

    fun start(context: Context?, listener: Listener) {
        listenerRef = WeakReference(listener)
        stop()

        if (context == null) {
            notifyState(false, "Context unavailable")
            return
        }

        if (!PermissionsFlowHelper.hasRequiredBluetoothPermissions(context)) {
            notifyState(false, "Missing Bluetooth permissions")
            return
        }

        if (!isLocationServicesEnabled(context)) {
            notifyState(true, "Location services OFF (some devices block BLE results)")
            Log.w(TAG, "Location services appear disabled; BLE scan callbacks may be suppressed on some devices.")
        }

        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        if (manager == null) {
            notifyState(false, "Bluetooth manager unavailable")
            return
        }

        bluetoothAdapter = manager.adapter
        if (bluetoothAdapter == null) {
            notifyState(false, "Bluetooth adapter unavailable")
            return
        }
        if (bluetoothAdapter?.isEnabled != true) {
            notifyState(false, "Bluetooth is disabled")
            return
        }

        try {
            bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
            if (bluetoothLeScanner != null) {
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()
                bluetoothLeScanner?.startScan(null, settings, scanCallback)
                scanning = true
                usingLegacyScan = false
                scanStartEventCount = callbackEventCount
                mainHandler.removeCallbacks(noResultFallbackRunnable)
                mainHandler.postDelayed(noResultFallbackRunnable, NO_RESULT_FALLBACK_MS)
                notifyState(true, "Scanning (modern scanner)")
                return
            }

            startLegacyScan()
        } catch (_: SecurityException) {
            notifyState(false, "Scan blocked by permissions")
        } catch (_: Throwable) {
            notifyState(false, "Scan failed to start")
        }
    }

    fun stop() {
        mainHandler.removeCallbacks(noResultFallbackRunnable)
        if (!scanning) return

        try {
            if (usingLegacyScan) {
                bluetoothAdapter?.stopLeScan(legacyLeScanCallback)
            } else {
                bluetoothLeScanner?.stopScan(scanCallback)
            }
        } catch (ex: SecurityException) {
            Log.w(TAG, "stop(): scan stop blocked by permissions", ex)
        } catch (ex: Throwable) {
            Log.w(TAG, "stop(): scan stop failed", ex)
        }

        scanning = false
        notifyState(false, "Scan stopped")
    }

    private fun dispatchDevice(device: BluetoothDevice?, rssi: Int, connectable: Boolean, source: String) {
        val listener = listenerRef.get() ?: return
        device ?: return

        callbackEventCount++
        if (callbackEventCount > scanStartEventCount) {
            mainHandler.removeCallbacks(noResultFallbackRunnable)
        }

        mainHandler.post {
            listenerRef.get()?.onDeviceDetected(device, rssi, connectable, source)
        }
    }

    private fun notifyState(isScanning: Boolean, message: String) {
        mainHandler.post {
            listenerRef.get()?.onScanStateChanged(isScanning, message)
        }
    }

    private fun startLegacyScan() {
        mainHandler.removeCallbacks(noResultFallbackRunnable)

        val adapter = bluetoothAdapter
        if (adapter == null) {
            notifyState(false, "Bluetooth adapter unavailable")
            return
        }

        try {
            if (scanning && bluetoothLeScanner != null) {
                bluetoothLeScanner?.stopScan(scanCallback)
            }

            adapter.startLeScan(legacyLeScanCallback)
            scanning = true
            usingLegacyScan = true
            notifyState(true, "Scanning (legacy fallback)")
        } catch (_: SecurityException) {
            notifyState(false, "Legacy scan blocked by permissions")
        } catch (_: Throwable) {
            notifyState(false, "Legacy scan failed to start")
        }
    }

    private fun isLocationServicesEnabled(context: Context): Boolean {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                locationManager.isLocationEnabled
            } else {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }
        } catch (ex: Throwable) {
            Log.w(TAG, "Failed to read location service state", ex)
            false
        }
    }
}

