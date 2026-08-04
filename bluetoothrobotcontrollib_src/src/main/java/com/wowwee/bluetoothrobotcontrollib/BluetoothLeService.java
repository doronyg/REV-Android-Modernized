/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.app.Service
 *  android.bluetooth.BluetoothAdapter
 *  android.bluetooth.BluetoothDevice
 *  android.bluetooth.BluetoothGatt
 *  android.bluetooth.BluetoothGattCallback
 *  android.bluetooth.BluetoothGattCharacteristic
 *  android.bluetooth.BluetoothGattDescriptor
 *  android.bluetooth.BluetoothGattService
 *  android.bluetooth.BluetoothManager
 *  android.content.Context
 *  android.content.Intent
 *  android.os.Binder
 *  android.os.Handler
 *  android.os.IBinder
 *  android.util.Log
 */
package com.wowwee.bluetoothrobotcontrollib;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import com.wowwee.bluetoothrobotcontrollib.BluetoothLeServiceListener;
import com.wowwee.bluetoothrobotcontrollib.services.BRBaseService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BluetoothLeService
extends Service {
    private static final String TAG = BluetoothLeService.class.getSimpleName();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private final ArrayList<String> mBluetoothDeviceAddress = new ArrayList();
    private final HashMap<String, BluetoothGatt> mBluetoothGatt = new HashMap();
    private int mConnectionState = 0;
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final String ACTION_GATT_CONNECTED = "com.wowwee.bluetooth.le.ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_DISCONNECTED = "com.wowwee.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public static final String ACTION_GATT_SERVICES_DISCOVERED = "com.wowwee.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public static final String ACTION_DATA_AVAILABLE = "com.wowwee.bluetooth.le.ACTION_DATA_AVAILABLE";
    public static final String EXTRA_DATA = "com.wowwee.bluetooth.le.EXTRA_DATA";
    public HashMap<String, HashMap<String, BRBaseService>> mServiceDict = new HashMap();
    private final HashMap<String, BluetoothLeServiceListener> mListener = new HashMap();
    private final ArrayList<BluetoothGattCharacteristic> readCharacteristicsQueue = new ArrayList();
    private final ArrayList<String> readCharacteristicsAddressQueue = new ArrayList();
    private boolean isReadingCharacteristics = false;
    private long lastReadCharacteristicsTime = 0L;
    protected Context mContext;
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback(){

        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == 2) {
                if (BluetoothLeService.this.mBluetoothGatt.containsValue(gatt)) {
                    String intentAction = BluetoothLeService.ACTION_GATT_CONNECTED;
                    BluetoothLeService.this.mConnectionState = 2;
                    BluetoothLeService.this.broadcastUpdate(intentAction);
                    Log.i(TAG, "Connected to GATT server.");
                    Log.i(TAG, "Attempting to start service discovery:" + gatt.discoverServices());
                    String address = BluetoothLeService.this.getAddressFromBluetoothGatt(gatt);
                    if (address != null && BluetoothLeService.this.mListener.containsKey(address)) {
                        BluetoothLeService.this.mListener.get(address).onBluetoothLeServiceConnectionStateChanged(gatt, BluetoothLeService.ACTION_GATT_CONNECTED);
                    }
                } else {
                    Log.e(TAG, "Receive BluetoothProfile.STATE_CONNECTED but BluetoothGatt is null");
                    gatt.disconnect();
                }
            } else if (newState == 0) {
                String intentAction = BluetoothLeService.ACTION_GATT_DISCONNECTED;
                BluetoothLeService.this.mConnectionState = 0;
                Log.i(TAG, "Disconnected from GATT server.");
                BluetoothLeService.this.broadcastUpdate(intentAction);
                String address = BluetoothLeService.this.getAddressFromBluetoothGatt(gatt);
                if (address != null && BluetoothLeService.this.mListener.containsKey(address)) {
                    BluetoothLeService.this.mListener.get(address).onBluetoothLeServiceConnectionStateChanged(gatt, BluetoothLeService.ACTION_GATT_DISCONNECTED);
                }
            }
        }

        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == 0) {
                BluetoothLeService.this.broadcastUpdate(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
                String address = BluetoothLeService.this.getAddressFromBluetoothGatt(gatt);
                if (address != null && BluetoothLeService.this.mListener.containsKey(address)) {
                    BluetoothLeService.this.mListener.get(address).onServicesDiscovered();
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            BluetoothLeService.this.isReadingCharacteristics = false;
            if (status == 0) {
                Log.d("BLE", "onCharacteristicRead GATT_SUCCESS: " + characteristic.getService().getUuid().toString());
                BluetoothLeService.this.broadcastUpdate(BluetoothLeService.ACTION_DATA_AVAILABLE, characteristic);
                String address = BluetoothLeService.this.getAddressFromBluetoothGatt(gatt);
                if (address != null) {
                    BRBaseService service = BluetoothLeService.this.findService(characteristic.getService().getUuid().toString(), address);
                    if (service != null) {
                        service.notifyCharacteristicHandler(characteristic);
                    } else {
                        Log.d("BLE", "ERROR: cannot find BR Service for " + characteristic.getService().getUuid().toString());
                    }
                }
            } else {
                Log.d("BLE", "onCharacteristicRead failed with id: " + characteristic.getService().getUuid().toString() + " ,code " + status);
            }
            BluetoothLeService.this.readNextCharacteristics();
        }

        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            BRBaseService service;
            Log.d("BLE", "onCharacteristicChanged: " + characteristic.getService().getUuid().toString());
            BluetoothLeService.this.broadcastUpdate(BluetoothLeService.ACTION_DATA_AVAILABLE, characteristic);
            String address = BluetoothLeService.this.getAddressFromBluetoothGatt(gatt);
            if (address != null && (service = BluetoothLeService.this.findService(characteristic.getService().getUuid().toString(), address)) != null) {
                service.notifyCharacteristicHandler(characteristic);
            }
        }

        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            BRBaseService service;
            String address;
            Log.d("BLE", "onCharacteristicWrite: " + characteristic.getService().getUuid().toString());
            if (status == 0 && (address = BluetoothLeService.this.getAddressFromBluetoothGatt(gatt)) != null && (service = BluetoothLeService.this.findService(characteristic.getService().getUuid().toString(), address)) != null) {
                service.notifyCharacteristicHandler(characteristic);
            }
        }

        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            BRBaseService service;
            String address;
            Log.d("BLE", "onDescriptorWrite: " + descriptor.getCharacteristic().getService().getUuid().toString());
            if (status == 0 && (address = BluetoothLeService.this.getAddressFromBluetoothGatt(gatt)) != null && (service = BluetoothLeService.this.findService(descriptor.getCharacteristic().getService().getUuid().toString(), address)) != null) {
                service.descriptorHandler(descriptor);
            }
        }
    };
    private final IBinder mBinder = new LocalBinder();

    private void broadcastUpdate(String action) {
        Intent intent = new Intent(action);
        this.sendBroadcast(intent);
    }

    private void broadcastUpdate(String action, BluetoothGattCharacteristic characteristic) {
        Intent intent = new Intent(action);
        byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            StringBuilder stringBuilder = new StringBuilder(data.length);
            byte[] byArray = data;
            int n = data.length;
            int n2 = 0;
            while (n2 < n) {
                byte byteChar = byArray[n2];
                stringBuilder.append(String.format("%02X ", byteChar));
                ++n2;
            }
            intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder);
        }
        this.sendBroadcast(intent);
    }

    public void setBluetoothLeServiceListener(BluetoothLeServiceListener pListener, String address) {
        this.mListener.put(address, pListener);
    }

    public void removeBluetoothLeServiceListener(String address) {
        this.mListener.remove(address);
    }

    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "BluetoothLeService unbind");
        this.closeAll();
        return super.onUnbind(intent);
    }

    public boolean initialize() {
        if (this.mBluetoothManager == null) {
            this.mBluetoothManager = (BluetoothManager)this.getSystemService("bluetooth");
            if (this.mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        this.mBluetoothAdapter = this.mBluetoothManager.getAdapter();
        if (this.mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    public void setServiceDict(HashMap<String, BRBaseService> pServiceDict, String address) {
        this.mServiceDict.put(address, pServiceDict);
    }

    public BRBaseService findService(String pUuid, String address) {
        if (!this.mServiceDict.containsKey(address)) {
            return null;
        }
        return this.mServiceDict.get(address).get(pUuid);
    }

    public boolean connect(final String address) {
        if (this.mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        final BluetoothDevice device = this.mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        final BluetoothLeService self = this;
        boolean previousConnectionExist = this.mBluetoothDeviceAddress != null && this.containsBluetoothDeviceAddress(address) && this.mBluetoothGatt != null;
        Handler mainHandler = new Handler(this.getMainLooper());
        if (previousConnectionExist) {
            Runnable closeConnectionRunnable = new Runnable(){

                @Override
                public void run() {
                    BluetoothGatt bluetoothGatt;
                    Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
                    if (BluetoothLeService.this.mBluetoothGatt != null && (bluetoothGatt = BluetoothLeService.this.mBluetoothGatt.get(address)) != null) {
                        bluetoothGatt.close();
                        BluetoothLeService.this.mBluetoothGatt.remove(address);
                    }
                }
            };
            mainHandler.post(closeConnectionRunnable);
            try {
                Thread.sleep(100L);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Runnable connectRunnable = new Runnable(){

            @Override
            public void run() {
                BluetoothGatt bluetoothGatt = device.connectGatt(self, false, BluetoothLeService.this.mGattCallback);
                if (bluetoothGatt != null) {
                    BluetoothLeService.this.mBluetoothGatt.put(address, bluetoothGatt);
                }
                Log.d(TAG, "Trying to create a new connection.");
                if (!BluetoothLeService.this.containsBluetoothDeviceAddress(address)) {
                    BluetoothLeService.this.mBluetoothDeviceAddress.add(address);
                }
                BluetoothLeService.this.mConnectionState = 1;
            }
        };
        mainHandler.post(connectRunnable);
        return true;
    }

    public void disconnect(final String address) {
        if (this.mBluetoothAdapter == null || !this.mBluetoothGatt.containsKey(address)) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        Handler mainHandler = new Handler(this.getMainLooper());
        Runnable myRunnable = new Runnable(){

            @Override
            public void run() {
                if (BluetoothLeService.this.mBluetoothGatt != null && BluetoothLeService.this.mBluetoothGatt.get(address) != null) {
                    BluetoothLeService.this.mBluetoothGatt.get(address).disconnect();
                }
            }
        };
        mainHandler.post(myRunnable);
    }

    public void close(final String address) {
        if (this.mBluetoothGatt == null) {
            return;
        }
        if (!this.mBluetoothGatt.containsKey(address)) {
            return;
        }
        Log.d(TAG, "BluetoothLeService close connection");
        Handler mainHandler = new Handler(this.getMainLooper());
        Runnable myRunnable = new Runnable(){

            @Override
            public void run() {
                if (BluetoothLeService.this.mBluetoothGatt != null && BluetoothLeService.this.mBluetoothGatt.containsKey(address)) {
                    BluetoothLeService.this.mBluetoothGatt.get(address).close();
                    BluetoothLeService.this.mBluetoothGatt.remove(address);
                }
            }
        };
        mainHandler.post(myRunnable);
    }

    public void closeAll() {
        for (String s : this.mBluetoothDeviceAddress) {
            this.close(s);
        }
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic, String address) {
        if (this.mBluetoothAdapter == null || this.mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        BluetoothGatt bluetoothGatt = this.mBluetoothGatt.get(address);
        if (bluetoothGatt != null) {
            boolean writeCharacteristicResult = bluetoothGatt.writeCharacteristic(characteristic);
            Log.d("BLE", "writeCharacteristicResult " + writeCharacteristicResult);
        }
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic, String address) {
        this.readCharacteristicsQueue.add(characteristic);
        this.readCharacteristicsAddressQueue.add(address);
        this.readNextCharacteristics();
    }

    private void readNextCharacteristics() {
        if (this.isReadingCharacteristics && System.currentTimeMillis() - this.lastReadCharacteristicsTime > 2000L) {
            this.isReadingCharacteristics = false;
        }
        if (!this.isReadingCharacteristics && this.readCharacteristicsQueue.size() > 0) {
            this.isReadingCharacteristics = true;
            this.lastReadCharacteristicsTime = System.currentTimeMillis();
            BluetoothGattCharacteristic nextReadCharacteristics = this.readCharacteristicsQueue.get(0);
            String address = this.readCharacteristicsAddressQueue.get(0);
            this.readCharacteristicsQueue.remove(0);
            this.readCharacteristicsAddressQueue.remove(0);
            this.performReadCharacteristic(nextReadCharacteristics, address);
        }
    }

    private void performReadCharacteristic(BluetoothGattCharacteristic characteristic, String address) {
        if (this.mBluetoothAdapter == null || this.mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        BluetoothGatt bluetoothGatt = this.mBluetoothGatt.get(address);
        if (bluetoothGatt != null) {
            boolean readCharacteristicResult = bluetoothGatt.readCharacteristic(characteristic);
            Log.d("BLE", "readCharacteristicResult " + readCharacteristicResult);
            if (!readCharacteristicResult) {
                Log.d("BLE", "read characteristics called failed");
            } else {
                Log.d("BLE", "read characteristics " + characteristic.getUuid().toString() + " called success");
            }
        }
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, String address, boolean enabled) {
        if (this.mBluetoothAdapter == null || this.mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        BluetoothGatt bluetoothGatt = this.mBluetoothGatt.get(address);
        if (bluetoothGatt != null && characteristic != null) {
            boolean setCharacteristicNotificationResult = bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
            Log.d("BLE", "setCharacteristicNotificationResult " + setCharacteristicNotificationResult);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            if (descriptor != null) {
                descriptor.setValue(enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : new byte[2]);
                bluetoothGatt.writeDescriptor(descriptor);
            }
        } else if (characteristic == null) {
            Log.w(TAG, "BluetoothLeServer set notifications: characteristic is null");
        }
    }

    public List<BluetoothGattService> getSupportedGattServices(String address) {
        if (this.mBluetoothGatt == null) {
            return null;
        }
        if (!this.mBluetoothGatt.containsKey(address)) {
            return null;
        }
        return this.mBluetoothGatt.get(address).getServices();
    }

    public BluetoothGattService getGattService(UUID uuid, String address) {
        if (this.mBluetoothGatt == null) {
            return null;
        }
        if (!this.mBluetoothGatt.containsKey(address)) {
            return null;
        }
        return this.mBluetoothGatt.get(address).getService(uuid);
    }

    public int getConnectionState() {
        return this.mConnectionState;
    }

    public boolean isConnected() {
        return this.mConnectionState == 2;
    }

    public BluetoothGatt getGatt(String address) {
        return this.mBluetoothGatt.get(address);
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    private boolean containsBluetoothDeviceAddress(String address) {
        for (String s : this.mBluetoothDeviceAddress) {
            if (!s.equalsIgnoreCase(address)) continue;
            return true;
        }
        return false;
    }

    private String getAddressFromBluetoothGatt(BluetoothGatt gatt) {
        for (Map.Entry<String, BluetoothGatt> e : this.mBluetoothGatt.entrySet()) {
            String key = e.getKey();
            BluetoothGatt value = e.getValue();
            if (!gatt.equals(value)) continue;
            return key;
        }
        return null;
    }

    public class LocalBinder
    extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }
}

