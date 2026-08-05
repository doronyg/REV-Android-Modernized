/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.bluetooth.BluetoothAdapter$LeScanCallback
 *  android.bluetooth.BluetoothDevice
 *  android.content.Intent
 *  android.content.IntentFilter
 *  android.os.Handler
 *  android.os.Looper
 *  android.os.Parcelable
 *  android.util.Log
 */
package com.wowwee.bluetoothrobotcontrollib.rev;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.wowwee.bluetoothrobotcontrollib.BluetoothRobot;
import com.wowwee.bluetoothrobotcontrollib.BluetoothRobotFinder;
import com.wowwee.bluetoothrobotcontrollib.util.AdRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class REVRobotFinder
extends BluetoothRobotFinder {
    private static final String TAG = "REVRobotFinder";
    // Temporary debug override: force discovered/connected robots to be treated as REV.
    // Set to false once productId/revType parsing is fixed.
    private static final boolean FORCE_ASSUME_REV_FOR_DEBUG = true;
    private static final long MIN_START_SCAN_INTERVAL_MS = 5000L;
    private static REVRobotFinder instance = null;
    private static final int SCAN_INTERVAL = 800;
    private static final int STOP_INTERVAL = 300;
    public static final int MRFScanOptionMask_ShowAllDevices = 0;
    public static final int MRFScanOptionMask_FilterByProductId = 1;
    public static final int MRFScanOptionMask_FilterByServices = 2;
    public static final int MRFScanOptionMask_FilterByDeviceName = 4;
    public static final String REVRobotFinder_REVFound = "com.wowwee.bluetoothrobotcontrollib.REVRobotFinder_REVFound";
    public static final String REVRobotFinder_REVListCleared = "com.wowwee.bluetoothrobotcontrollib.REVRobotFinder_REVListCleared";
    public static final String REVRobotFinder_BluetoothError = "com.wowwee.bluetoothrobotcontrollib.REVRobotFinder_BluetoothError";
    public static final String REVRobotFinder_REVPairedFound = "com.wowwee.bluetoothrobotcontrollib.REVRobotFinder_REVPairedFound";
    public static final String REVRobotFinder_RAMPFound = "com.wowwee.bluetoothrobotcontrollib.REVRobotFinder_RAMFound";
    public static final String REVRobotFinder_ConnectedBroadcastREVFound = "com.wowwee.bluetoothrobotcontrollib.REVRobotFinder_ConnectedBroadcastREVFound";
    public static final String REVRobotFinder_ConnectedBroadcastREVUpdated = "com.wowwee.bluetoothrobotcontrollib.REVRobotFinder_ConnectedBroadcastREVUpdated";
    private int mScanOptionsFlagMask = 1;
    private final List<REVRobot> mRevFound;
    private final List<REVRobot> mRevRobotConnected;
    private List<REVRobot> mRampFound;
    private List<REVRobot> mRampConnected;
    private ArrayList<BluetoothDevice> pairedDeviceList;
    private final Handler mScanHandler;
    private boolean isScanningForConnectedBroadcastREV;
    private final List<REVRobot> devicesWithConnectedBroadcast;
    private boolean autoRescanConnectedBroadcast;
    private final Handler rampFoundHandler;
    private final Handler revFoundHandler;
    private long leScanEventCount = 0L;
    private boolean isLeScanActive = false;
    private long lastStartScanTimestampMs = 0L;
    private final Runnable startScanRunnable = new Runnable(){

        @Override
        public void run() {
            REVRobotFinder.this.startScan();
        }
    };
    private final Runnable stopScanRunnable = new Runnable(){

        @Override
        public void run() {
            try {
                if (REVRobotFinder.this.mBluetoothAdapter != null && REVRobotFinder.this.mLeScanCallback != null) {
                    REVRobotFinder.this.mBluetoothAdapter.stopLeScan(REVRobotFinder.this.mLeScanCallback);
                    REVRobotFinder.this.isLeScanActive = false;
                }
            }
            catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    };
    private final Runnable startScanRunnableForConnectedBroadcastREV = new Runnable(){

        @Override
        public void run() {
            REVRobotFinder.this.startScan();
            REVRobotFinder.this.mScanHandler.postDelayed(REVRobotFinder.this.stopScanRunnableForConnectedBroadcastREV, 1000L);
        }
    };
    private final Runnable stopScanRunnableForConnectedBroadcastREV = new Runnable(){

        @Override
        public void run() {
            try {
                if (REVRobotFinder.this.mBluetoothAdapter != null && REVRobotFinder.this.mBluetoothAdapter.isEnabled()) {
                    Log.d(TAG, "stopScanRunnableForConnectedBroadcastREV: stopLeScan()");
                    REVRobotFinder.this.mBluetoothAdapter.stopLeScan(REVRobotFinder.this.mLeScanCallback);
                    REVRobotFinder.this.isLeScanActive = false;
                }
                if (REVRobotFinder.this.startScanRunnableForConnectedBroadcastREV != null && REVRobotFinder.this.mScanHandler != null) {
                    REVRobotFinder.this.mScanHandler.postDelayed(REVRobotFinder.this.startScanRunnableForConnectedBroadcastREV, STOP_INTERVAL);
                }
            }
            catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    };
    private final BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback(){

        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            ++REVRobotFinder.this.leScanEventCount;
            String name = REVRobotFinder.this.safeDeviceName(device);
            String address = device != null ? device.getAddress() : "<null-address>";
            int advLen = scanRecord != null ? scanRecord.length : 0;
            String type = REVRobotFinder.this.safeDeviceType(device);
            String bond = REVRobotFinder.this.safeBondState(device);
            String advPreview = REVRobotFinder.this.bytesToHexPreview(scanRecord, 24);
            Log.d(TAG, "onLeScan(): #" + REVRobotFinder.this.leScanEventCount + ", name=" + name + ", address=" + address + ", type=" + type + ", bond=" + bond + ", rssi=" + rssi + ", advLen=" + advLen + ", adv=" + advPreview + ", thread=" + Thread.currentThread().getName());

            if (device == null) {
                Log.w(TAG, "onLeScan(): device is null, skipping callback processing");
                return;
            }
            if (scanRecord == null) {
                Log.w(TAG, "onLeScan(): scanRecord is null for " + address + ", skipping callback processing");
                return;
            }

            try {
                REVRobotFinder.this.handleFoundBluetoothDevice(device, scanRecord, rssi);
            }
            catch (Throwable ex) {
                Log.e(TAG, "onLeScan(): failed while handling device " + address, ex);
            }
        }
    };

    public REVRobotFinder() {
        this.mRevFound = new ArrayList<REVRobot>();
        this.mRevRobotConnected = new ArrayList<REVRobot>();
        this.mScanHandler = new Handler(Looper.getMainLooper());
        this.mRampFound = new ArrayList<REVRobot>();
        this.mRampConnected = new ArrayList<REVRobot>();
        this.devicesWithConnectedBroadcast = new ArrayList<REVRobot>();
        this.rampFoundHandler = new Handler();
        this.revFoundHandler = new Handler();
    }

    public static REVRobotFinder getInstance() {
        if (instance == null) {
            instance = new REVRobotFinder();
        }
        return instance;
    }

    public static IntentFilter getRevRobotFinderIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(REVRobotFinder_REVFound);
        intentFilter.addAction(REVRobotFinder_REVListCleared);
        intentFilter.addAction(REVRobotFinder_BluetoothError);
        intentFilter.addAction(REVRobotFinder_REVPairedFound);
        intentFilter.addAction(REVRobotFinder_RAMPFound);
        intentFilter.addAction(REVRobotFinder_ConnectedBroadcastREVFound);
        intentFilter.addAction(REVRobotFinder_ConnectedBroadcastREVUpdated);
        return intentFilter;
    }

    public void revDidConnect(REVRobot rev) {
        if (rev != null && !this.mRevRobotConnected.contains(rev)) {
            if (FORCE_ASSUME_REV_FOR_DEBUG && rev.revType == null) {
                Log.w(TAG, "FORCE_ASSUME_REV_FOR_DEBUG active: revType was null in revDidConnect(), forcing REV for " + rev.getName());
                rev.setRevType(REVRobot.REVType.REV);
            }
            if (rev.revType == REVRobot.REVType.REV) {
                this.mRevRobotConnected.add(rev);
            } else {
                this.mRampConnected.add(rev);
            }
            String btType = rev.getBluetoothDevice() != null ? this.safeDeviceType(rev.getBluetoothDevice()) : "<no-bt-device>";
            String address = rev.getBluetoothDevice() != null ? this.safeDeviceAddress(rev.getBluetoothDevice()) : "<no-address>";
            Log.d(TAG, "revDidConnect(): name=" + rev.getName() + ", productId=" + rev.getProductId() + ", revType=" + rev.getRevType() + ", btType=" + btType + ", address=" + address + ", revConnected=" + this.mRevRobotConnected.size() + ", rampConnected=" + this.mRampConnected.size());
        }
    }

    public void revDidDisconnect(REVRobot rev) {
        if (rev != null) {
            if (rev.revType == REVRobot.REVType.REV) {
                this.mRevRobotConnected.remove(rev);
            } else {
                this.mRampConnected.remove(rev);
            }
            if (rev.revType == REVRobot.REVType.REV) {
                this.mRevFound.remove(rev);
            } else {
                this.mRampFound.remove(rev);
            }
            Log.d(TAG, "revDidDisconnect(): removed " + rev.getName() + ", revConnected=" + this.mRevRobotConnected.size() + ", rampConnected=" + this.mRampConnected.size() + ", revFound=" + this.mRevFound.size() + ", rampFound=" + this.mRampFound.size());
        }
    }

    public void scanForREVContinuous() {
        Log.d(TAG, "scanForREVContinuous() requested; optionsMask=" + this.mScanOptionsFlagMask);
        this.isScanningForConnectedBroadcastREV = false;
        this.mScanHandler.removeCallbacks(this.startScanRunnable);
        this.mScanHandler.removeCallbacks(this.stopScanRunnable);
        this.mScanHandler.post(this.startScanRunnable);
    }

    public void stopScanForREVContinuous() {
        Log.d(TAG, "stopScanForREVContinuous() requested");
        this.isScanningForConnectedBroadcastREV = false;
        this.mScanHandler.removeCallbacks(this.startScanRunnable);
        this.mScanHandler.removeCallbacks(this.stopScanRunnable);
        this.stopScanForREV();
    }

    public void scanForREV() {
        Log.d(TAG, "scanForREV() requested");
        this.isScanningForConnectedBroadcastREV = false;
        this.startScan();
    }

    public void scanForREVforDuration(int pSeconds) {
        this.startScan();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable(){

            @Override
            public void run() {
                REVRobotFinder.this.stopScanForREV();
            }
        }, pSeconds * 1000L);
    }

    public void stopScanForREV() {
        try {
            if (this.mBluetoothAdapter != null && this.mBluetoothAdapter.isEnabled()) {
                Log.d(TAG, "stopScanForREV(): stopLeScan() called");
                this.mBluetoothAdapter.stopLeScan(this.mLeScanCallback);
                this.isLeScanActive = false;
            } else {
                Log.d(TAG, "stopScanForREV(): skip stop, adapter=" + this.mBluetoothAdapter);
            }
        }
        catch (NullPointerException e) {
            Log.e(TAG, "stopScanForREV(): NPE while stopping scan", e);
        }
        catch (SecurityException e) {
            Log.e(TAG, "stopScanForREV(): missing permission while stopping scan", e);
        }
    }

    public void scanForConnectedBroadcastREV() {
        Log.d(TAG, "scanForConnectedBroadcastREV() requested");
        this.isScanningForConnectedBroadcastREV = true;
        this.mScanHandler.post(this.startScanRunnableForConnectedBroadcastREV);
    }

    public void stopScanForConnectedBroadcastREV() {
        this.mScanHandler.removeCallbacks(this.startScanRunnableForConnectedBroadcastREV);
        this.mScanHandler.removeCallbacks(this.stopScanRunnableForConnectedBroadcastREV);
        this.stopScanForREV();
        this.revFoundHandler.post(new Runnable(){

            @Override
            public void run() {
                if (REVRobotFinder.this.devicesWithConnectedBroadcast != null) {
                    REVRobotFinder.this.devicesWithConnectedBroadcast.clear();
                }
            }
        });
    }

    public REVRobot firstConnectedREV() {
        if (this.mRevRobotConnected.size() > 0) {
            return this.mRevRobotConnected.get(0);
        }
        return null;
    }

    public void clearFoundREVList() {
        ArrayList<REVRobot> revToRemove = new ArrayList<REVRobot>();
        for (REVRobot rev : this.mRevFound) {
            if (this.mRevRobotConnected.contains(rev)) continue;
            revToRemove.add(rev);
        }
        for (REVRobot rev : revToRemove) {
            this.mRevFound.remove(rev);
        }
        if (this.mContext == null) {
            Log.w(TAG, "clearFoundREVList(): mContext is null, cannot broadcast list clear");
            return;
        }
        Intent intent = new Intent(REVRobotFinder_REVListCleared);
        this.mContext.sendBroadcast(intent);
    }

    public REVRobot findREV(BluetoothDevice pDevice) {
        for (REVRobot rev : this.mRevRobotConnected) {
            if (rev.getBluetoothDevice() == null || !rev.getBluetoothDevice().equals(pDevice)) continue;
            return rev;
        }
        for (REVRobot rev : this.mRevFound) {
            if (rev.getBluetoothDevice() == null || !rev.getBluetoothDevice().equals(pDevice)) continue;
            return rev;
        }
        return null;
    }

    public List<REVRobot> getRevFoundList() {
        return this.mRevFound;
    }

    public List<REVRobot> getmRevRobotConnectedList() {
        return this.mRevRobotConnected;
    }

    public List<REVRobot> getmRampFound() {
        return this.mRampFound;
    }

    public void setmRampFound(List<REVRobot> mRampFound) {
        this.mRampFound = mRampFound;
    }

    public List<REVRobot> getmRampConnected() {
        return this.mRampConnected;
    }

    public void setmRampConnected(List<REVRobot> mRampConnected) {
        this.mRampConnected = mRampConnected;
    }

    public List<REVRobot> getDevicesWithConnectedBroadcast() {
        return this.devicesWithConnectedBroadcast;
    }

    public boolean isAutoRescanConnectedBroadcast() {
        return this.autoRescanConnectedBroadcast;
    }

    public void setAutoRescanConnectedBroadcast(boolean autoRescanConnectedBroadcast) {
        this.autoRescanConnectedBroadcast = autoRescanConnectedBroadcast;
    }

    private void startScan() {
        Log.d(TAG, "startScan() begin; adapter=" + this.mBluetoothAdapter + ", context=" + this.mContext + ", isScanningForConnectedBroadcastREV=" + this.isScanningForConnectedBroadcastREV + ", optionsMask=" + this.mScanOptionsFlagMask);
        this.logPermissionSnapshot();

        long now = System.currentTimeMillis();
        if (this.isLeScanActive) {
            Log.w(TAG, "startScan(): scan already active, skipping duplicate start");
            return;
        }
        long minStartIntervalMs = this.isScanningForConnectedBroadcastREV ? 1000L : MIN_START_SCAN_INTERVAL_MS;
        if (now - this.lastStartScanTimestampMs < minStartIntervalMs) {
            Log.w(TAG, "startScan(): throttled locally to avoid platform rate limit; deltaMs=" + (now - this.lastStartScanTimestampMs) + ", minIntervalMs=" + minStartIntervalMs + ", connectedBroadcastMode=" + this.isScanningForConnectedBroadcastREV);
            return;
        }

        if (this.mBluetoothAdapter == null) {
            // Defensive recovery for cases where adapter was never set by caller.
            this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Log.w(TAG, "startScan(): recovered adapter from BluetoothAdapter.getDefaultAdapter(): " + this.mBluetoothAdapter);
        }

        if (this.mBluetoothAdapter == null) {
            Log.e("BLERobotControlLib", "Could not start scan, bluetooth adapter is null. Your device may not support Bluetooth LE");
            Log.e(TAG, "startScan(): aborting because adapter is still null after recovery");
            return;
        }

        if (!this.mBluetoothAdapter.isEnabled()) {
            Log.w(TAG, "startScan(): aborting because adapter is disabled");
            return;
        }

        if (!this.hasRuntimeScanPermissions()) {
            Log.e(TAG, "startScan(): missing runtime Bluetooth scan/connect permissions");
            return;
        }

        boolean filterByServices = (this.mScanOptionsFlagMask & 2) != 0;
        try {
            if (filterByServices) {
                UUID[] services = BluetoothRobot.getAdvertisedServiceUUIDs();
                Log.d(TAG, "startScan(): startLeScan(UUID[]) with " + services.length + " services");
                this.mBluetoothAdapter.startLeScan(services, this.mLeScanCallback);
            } else {
                Log.d(TAG, "startScan(): startLeScan(callback)");
                this.mBluetoothAdapter.startLeScan(this.mLeScanCallback);
            }
            this.lastStartScanTimestampMs = now;
            this.isLeScanActive = true;
        }
        catch (SecurityException e) {
            Log.e(TAG, "startScan(): SecurityException from startLeScan", e);
            return;
        }
        catch (Throwable e) {
            Log.e(TAG, "startScan(): unexpected error from startLeScan", e);
            return;
        }

        this.getPairedREV();
    }

    private void getPairedREV() {
        Set<BluetoothDevice> paired;
        if (this.mBluetoothAdapter != null && (paired = this.mBluetoothAdapter.getBondedDevices()) != null && paired.size() != 0) {
            Log.d(TAG, "getPairedREV(): bonded devices count=" + paired.size());
            this.pairedDeviceList = new ArrayList<BluetoothDevice>();
            for (BluetoothDevice bluetoothDevice : paired) {
                String name = this.safeDeviceName(bluetoothDevice);
                String nameLower = name.toLowerCase(Locale.US);
                int type = bluetoothDevice.getType();
                String address = this.safeDeviceAddress(bluetoothDevice);
                boolean isLeOrDual = type == BluetoothDevice.DEVICE_TYPE_LE || type == BluetoothDevice.DEVICE_TYPE_DUAL;
                boolean nameLooksRev = nameLower.contains("rev");
                Log.d(TAG, "getPairedREV(): paired device name=" + name + ", address=" + address + ", type=" + this.safeDeviceType(bluetoothDevice) + ", bond=" + this.safeBondState(bluetoothDevice) + ", isLeOrDual=" + isLeOrDual + ", nameLooksRev=" + nameLooksRev);

                if (name == null || !isLeOrDual || !nameLooksRev) {
                    Log.d(TAG, "getPairedREV(): filtered paired device address=" + address + " (name/type did not match REV criteria)");
                    continue;
                }
                this.pairedDeviceList.add(bluetoothDevice);
                Log.d(TAG, "getPairedREV(): accepted paired REV candidate address=" + address + ", name=" + name);
            }
            Log.d(TAG, "getPairedREV(): REV-like paired devices=" + this.pairedDeviceList.size());
            if (this.pairedDeviceList.size() > 0) {
                if (this.mContext == null) {
                    Log.w(TAG, "getPairedREV(): mContext is null, cannot broadcast paired devices");
                    return;
                }
                Intent intent = new Intent(REVRobotFinder_REVPairedFound);
                intent.putExtra("PairedBluetoothDevices", this.pairedDeviceList);
                Log.d(TAG, "getPairedREV(): broadcasting REVRobotFinder_REVPairedFound with " + this.pairedDeviceList.size() + " devices");
                this.mContext.sendBroadcast(intent);
            }
        } else {
            Log.d(TAG, "getPairedREV(): no bonded devices or adapter unavailable");
        }
    }

    private void handleFoundBluetoothDevice(BluetoothDevice device, byte[] advertisingData, int rssi) {
        Log.d(TAG, "handleFoundBluetoothDevice(): name=" + this.safeDeviceName(device) + ", rssi=" + rssi + ", advLen=" + (advertisingData == null ? 0 : advertisingData.length));
        Log.d(TAG, "handleFoundBluetoothDevice(): identity address=" + this.safeDeviceAddress(device) + ", type=" + this.safeDeviceType(device) + ", bond=" + this.safeBondState(device));
        if (!this.isBluetoothDeviceExist(device)) {
            boolean filterByDeviceName;
            boolean filterByProductId;
            List<AdRecord> records = AdRecord.parseScanRecord(advertisingData);
            if (records == null) {
                Log.w(TAG, "handleFoundBluetoothDevice(): AdRecord parser returned null, skipping " + this.safeDeviceAddress(device));
                return;
            }
            Log.d(TAG, "handleFoundBluetoothDevice(): AD records=" + this.summarizeAdRecords(records));
            boolean isConnectable = false;
            byte adValue = 0;
            for (AdRecord record : records) {
                if (record == null || record.getType() != 1) continue;
                byte[] data = record.getmData();
                if (data == null || data.length == 0) continue;
                adValue = data[0];
                if (adValue != 5 && adValue != 6) continue;
                isConnectable = true;
                break;
            }
            if (!isConnectable && !this.isScanningForConnectedBroadcastREV) {
                Log.d(TAG, "Filtered out non-connectable advertisement from " + this.safeDeviceName(device));
                return;
            }
            REVRobot robot = new REVRobot(device, records, null);
            robot.debugLog();
            robot.setRssi(rssi);
            Log.d(TAG, "candidateRobot(): name=" + this.safeDeviceName(device) + ", productId=" + robot.getProductId() + ", btType=" + this.safeDeviceType(device) + ", address=" + this.safeDeviceAddress(device) + ", connectedBroadcastMode=" + this.isScanningForConnectedBroadcastREV);
            if (adValue == 6 && robot.getProductId() != 18 && robot.getProductId() != 17 && !(isConnectable = false) && !this.isScanningForConnectedBroadcastREV) {
                Log.d(TAG, "Filtered out DFU-like advertisement by product id: " + robot.getProductId());
                return;
            }
            byte revConnectedBroadcastByte = 0;
            byte healthValue = 0;
            if (advertisingData.length > 7) {
                revConnectedBroadcastByte = advertisingData[7];
            }
            if (advertisingData.length > 8) {
                healthValue = advertisingData[8];
            }
            boolean bl = filterByProductId = (this.mScanOptionsFlagMask & 1) != 0;
            if (filterByProductId && robot.getProductId() != 15 && robot.getProductId() != 16 && robot.getProductId() != 64 && robot.getProductId() != 17 && robot.getProductId() != 18) {
                Log.d(TAG, "Filtered by product id. id=" + robot.getProductId() + ", name=" + this.safeDeviceName(device));
                return;
            }
            boolean bl2 = filterByDeviceName = (this.mScanOptionsFlagMask & 4) != 0;
            String robotName = robot.getName();
            if (filterByDeviceName && (robotName == null || !robotName.toUpperCase(Locale.US).contains("REV"))) {
                Log.d(TAG, "Filtered by name rule. name=" + robot.getName());
                return;
            }
            if (robot.getProductId() == 16 && this.isScanningForConnectedBroadcastREV && revConnectedBroadcastByte != REVCommandValues.kRevConnectedBroadcast) {
                Log.d(TAG, "Connected-broadcast mode: filtered REV without connected marker. name=" + this.safeDeviceName(device) + ", marker=" + (revConnectedBroadcastByte & 0xFF));
                return;
            }
            boolean bl3 = robot.isDFUMode = robot.getProductId() == 17 || robot.getProductId() == 18 || robot.getProductId() == 64;
            if (FORCE_ASSUME_REV_FOR_DEBUG) {
                robot.setRevType(REVRobot.REVType.REV);
                robot.isDFUMode = false;
                Log.w(TAG, "FORCE_ASSUME_REV_FOR_DEBUG active: forcing REV classification for " + this.safeDeviceName(device) + " (productId=" + robot.getProductId() + ")");
            } else if (robot.getProductId() == 15 || robot.getProductId() == 17 || robot.getProductId() == 64) {
                robot.setRevType(REVRobot.REVType.REV);
            } else {
                robot.setRevType(REVRobot.REVType.RAMP);
            }
            Log.d(TAG, "Classified device: name=" + this.safeDeviceName(device) + ", productId=" + robot.getProductId() + ", revType=" + robot.getRevType() + ", btType=" + this.safeDeviceType(device) + ", dfu=" + robot.isDFUMode);
            Intent intent = null;
            if (this.isScanningForConnectedBroadcastREV) {
                if (robot.getRevType() == REVRobot.REVType.RAMP) {
                    final REVRobot ramp = robot;
                    this.rampFoundHandler.post(new Runnable(){

                        @Override
                        public void run() {
                            REVRobotFinder.this.mRampFound.add(ramp);
                            Intent intent = new Intent(REVRobotFinder.REVRobotFinder_RAMPFound);
                            intent.putExtra("BluetoothDevice", ramp.getBluetoothDevice());
                            REVRobotFinder.this.mContext.sendBroadcast(intent);
                        }
                    });
                } else {
                    final REVRobot rev = robot;
                    final byte health = healthValue;
                    this.revFoundHandler.post(new Runnable(){

                        @Override
                        public void run() {
                            rev.setHealth((float)health / 100.0f);
                            REVRobotFinder.this.mRevFound.add(rev);
                            REVRobotFinder.this.devicesWithConnectedBroadcast.add(rev);
                            Intent intent = new Intent(REVRobotFinder.REVRobotFinder_ConnectedBroadcastREVFound);
                            intent.putExtra("BluetoothDevice", rev.getBluetoothDevice());
                            REVRobotFinder.this.mContext.sendBroadcast(intent);
                        }
                    });
                }
            } else if (robot.getRevType() == REVRobot.REVType.REV) {
                REVRobot rev = robot;
                this.mRevFound.add(rev);
                Log.d(TAG, "REV found and added. foundCount=" + this.mRevFound.size() + ", name=" + rev.getName() + ", productId=" + rev.getProductId());
                intent = new Intent(REVRobotFinder_REVFound);
                intent.putExtra("BluetoothDevice", rev.getBluetoothDevice());
                this.mContext.sendBroadcast(intent);
            } else {
                final REVRobot ramp = robot;
                this.rampFoundHandler.post(new Runnable(){

                    @Override
                    public void run() {
                        REVRobotFinder.this.mRampFound.add(ramp);
                        Intent intent = new Intent(REVRobotFinder.REVRobotFinder_RAMPFound);
                        intent.putExtra("BluetoothDevice", ramp.getBluetoothDevice());
                        REVRobotFinder.this.mContext.sendBroadcast(intent);
                    }
                });
            }
        } else if (this.isScanningForConnectedBroadcastREV) {
            REVRobot rev = this.findREV(device);
            if (rev != null && this.mRevRobotConnected.contains(rev)) {
                return;
            }
            boolean isFound = false;
            for (REVRobot r : this.devicesWithConnectedBroadcast) {
                if (!r.getBluetoothDevice().equals(device)) continue;
                rev = r;
                isFound = true;
            }
            List<AdRecord> records = AdRecord.parseScanRecord(advertisingData);
            if (records == null) {
                Log.w(TAG, "Connected-broadcast update: AdRecord parser returned null for " + this.safeDeviceAddress(device));
                return;
            }
            boolean isConnectable = false;
            for (AdRecord record : records) {
                byte adValue;
                if (record == null || record.getType() != 1) continue;
                byte[] data = record.getmData();
                if (data == null || data.length == 0) continue;
                adValue = data[0];
                if (adValue != 5) continue;
                isConnectable = true;
                break;
            }
            if (isConnectable) {
                if (rev != null) {
                    final REVRobot robot = rev;
                    this.revFoundHandler.post(new Runnable(){

                        @Override
                        public void run() {
                            REVRobotFinder.this.devicesWithConnectedBroadcast.remove(robot);
                            Intent intent = new Intent(REVRobotFinder.REVRobotFinder_ConnectedBroadcastREVFound);
                            intent.putExtra("BluetoothDevice", robot.getBluetoothDevice());
                            REVRobotFinder.this.mContext.sendBroadcast(intent);
                        }
                    });
                }
                return;
            }
            byte healthValue = 0;
            byte specialIconValue = 0;
            byte avatarIconBroadcastDriverValue = 1;
            if (advertisingData.length > 8) {
                healthValue = advertisingData[8];
            }
            if (advertisingData.length > 9) {
                specialIconValue = advertisingData[9];
            }
            if (advertisingData.length > 10) {
                avatarIconBroadcastDriverValue = advertisingData[10];
            }
            if (rev != null) {
                final boolean isFound_t = isFound;
                final REVRobot robot = rev;
                rev.setHealth((float)healthValue / 100.0f);
                rev.setSpecialBroadcastID(specialIconValue);
                rev.setAvatarIconBroadcastDriverID(avatarIconBroadcastDriverValue);
                this.revFoundHandler.post(new Runnable(){

                    @Override
                    public void run() {
                        Intent intent = null;
                        if (isFound_t) {
                            intent = new Intent(REVRobotFinder.REVRobotFinder_ConnectedBroadcastREVUpdated);
                        } else {
                            REVRobotFinder.this.devicesWithConnectedBroadcast.add(robot);
                            intent = new Intent(REVRobotFinder.REVRobotFinder_ConnectedBroadcastREVFound);
                        }
                        intent.putExtra("BluetoothDevice", robot.getBluetoothDevice());
                        REVRobotFinder.this.mContext.sendBroadcast(intent);
                    }
                });
            } else {
                final REVRobot rb = new REVRobot(device, records, null);
                rb.setHealth((float)healthValue / 100.0f);
                rb.setSpecialBroadcastID(specialIconValue);
                this.revFoundHandler.post(new Runnable(){

                    @Override
                    public void run() {
                        REVRobotFinder.this.devicesWithConnectedBroadcast.add(rb);
                        Intent intent = new Intent(REVRobotFinder.REVRobotFinder_ConnectedBroadcastREVFound);
                        intent.putExtra("BluetoothDevice", rb.getBluetoothDevice());
                        REVRobotFinder.this.mContext.sendBroadcast(intent);
                    }
                });
            }
        } else {
            REVRobot rev = this.findREV(device);
            if (rev != null) {
                Log.d(TAG, "Update RSSI: " + rev.getName() + " : " + rssi);
                rev.rssi = rssi;
            } else {
                Log.d(TAG, "Device already exists in cache but no REV object found for address=" + this.safeDeviceAddress(device));
            }
        }
    }

    private String summarizeAdRecords(List<AdRecord> records) {
        if (records == null || records.isEmpty()) {
            return "<none>";
        }

        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < records.size(); i++) {
            AdRecord record = records.get(i);
            if (record == null) {
                continue;
            }
            if (summary.length() > 0) {
                summary.append(" | ");
            }
            byte[] data = record.getmData();
            int dataLen = data != null ? data.length : 0;
            summary.append("type=").append(record.getType()).append(",len=").append(dataLen);
            if (dataLen > 0) {
                summary.append(",data=").append(this.bytesToHexPreview(data, 12));
            }
        }
        return summary.toString();
    }

    private String safeDeviceAddress(BluetoothDevice device) {
        if (device == null) {
            return "<null-address>";
        }
        try {
            String address = device.getAddress();
            return address != null ? address : "<no-address>";
        }
        catch (SecurityException ex) {
            Log.w(TAG, "safeDeviceAddress(): permission denied for getAddress()", ex);
            return "<permission-denied>";
        }
        catch (Throwable ex) {
            Log.w(TAG, "safeDeviceAddress(): failed reading device address", ex);
            return "<error>";
        }
    }

    private String safeDeviceType(BluetoothDevice device) {
        if (device == null) {
            return "<null-type>";
        }
        try {
            int type = device.getType();
            switch (type) {
                case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                    return "CLASSIC";
                case BluetoothDevice.DEVICE_TYPE_LE:
                    return "LE";
                case BluetoothDevice.DEVICE_TYPE_DUAL:
                    return "DUAL";
                case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
                default:
                    return "UNKNOWN(" + type + ")";
            }
        }
        catch (Throwable ex) {
            Log.w(TAG, "safeDeviceType(): failed reading device type", ex);
            return "<error>";
        }
    }

    private String safeBondState(BluetoothDevice device) {
        if (device == null) {
            return "<null-bond>";
        }
        try {
            int state = device.getBondState();
            switch (state) {
                case BluetoothDevice.BOND_NONE:
                    return "NONE";
                case BluetoothDevice.BOND_BONDING:
                    return "BONDING";
                case BluetoothDevice.BOND_BONDED:
                    return "BONDED";
                default:
                    return "UNKNOWN(" + state + ")";
            }
        }
        catch (Throwable ex) {
            Log.w(TAG, "safeBondState(): failed reading bond state", ex);
            return "<error>";
        }
    }

    private String bytesToHexPreview(byte[] data, int maxBytes) {
        if (data == null || data.length == 0) {
            return "<empty>";
        }
        int len = Math.min(data.length, maxBytes);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(String.format(Locale.US, "%02X", data[i] & 0xFF));
        }
        if (data.length > len) {
            sb.append(" ...(+").append(data.length - len).append(')');
        }
        return sb.toString();
    }

    private boolean hasRuntimeScanPermissions() {
        if (this.mContext == null) {
            Log.e(TAG, "Permission check failed: mContext is null");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean scanGranted = this.mContext.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            boolean connectGranted = this.mContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            return scanGranted && connectGranted;
        }

        boolean coarseGranted = this.mContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean fineGranted = this.mContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return coarseGranted || fineGranted;

    }

    private void logPermissionSnapshot() {
        if (this.mContext == null) {
            Log.w(TAG, "logPermissionSnapshot(): mContext is null");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean scanGranted = this.mContext.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            boolean connectGranted = this.mContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, "Permissions S+: BLUETOOTH_SCAN=" + scanGranted + ", BLUETOOTH_CONNECT=" + connectGranted);
        } else {
            boolean coarseGranted = this.mContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            boolean fineGranted = this.mContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, "Permissions pre-S: ACCESS_COARSE_LOCATION=" + coarseGranted + ", ACCESS_FINE_LOCATION=" + fineGranted);
        }
    }

    private String safeDeviceName(BluetoothDevice device) {
        if (device == null) {
            return "<null-device>";
        }
        try {
            String name = device.getName();
            return name != null ? name : "<unnamed>";
        }
        catch (SecurityException ex) {
            Log.w(TAG, "safeDeviceName(): permission denied for getName()", ex);
            return "<permission-denied>";
        }
        catch (Throwable ex) {
            Log.w(TAG, "safeDeviceName(): failed reading device name", ex);
            return "<error>";
        }
    }

    private boolean isBluetoothDeviceExist(BluetoothDevice pDevice) {
        for (REVRobot rev : this.mRevFound) {
            if (rev.getBluetoothDevice() == null || !rev.getBluetoothDevice().equals(pDevice)) continue;
            return true;
        }
        return false;
    }

    public int getmScanOptionsFlagMask() {
        return this.mScanOptionsFlagMask;
    }

    public void setmScanOptionsFlagMask(int mScanOptionsFlagMask) {
        this.mScanOptionsFlagMask = mScanOptionsFlagMask;
    }
}
