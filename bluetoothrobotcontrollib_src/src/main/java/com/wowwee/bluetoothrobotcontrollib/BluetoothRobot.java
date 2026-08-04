/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.bluetooth.BluetoothDevice
 *  android.bluetooth.BluetoothGatt
 *  android.bluetooth.BluetoothGattService
 *  android.content.ComponentName
 *  android.content.Context
 *  android.content.Intent
 *  android.content.ServiceConnection
 *  android.os.IBinder
 *  android.util.Log
 */
package com.wowwee.bluetoothrobotcontrollib;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import com.wowwee.bluetoothrobotcontrollib.BluetoothLeService;
import com.wowwee.bluetoothrobotcontrollib.BluetoothLeServiceListener;
import com.wowwee.bluetoothrobotcontrollib.RobotCommand;
import com.wowwee.bluetoothrobotcontrollib.services.BRBaseService;
import com.wowwee.bluetoothrobotcontrollib.services.BRReceiveDataService;
import com.wowwee.bluetoothrobotcontrollib.services.BRSendDataService;
import com.wowwee.bluetoothrobotcontrollib.util.AdRecord;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class BluetoothRobot
implements PropertyChangeListener {
    public static final int kBluetoothRobotStateDisconnected = 0;
    public static final int kBluetoothRobotStateConnecting = 1;
    public static final int kBluetoothRobotStateConnected = 2;
    public static final int kBluetoothRobotStateDisconnecting = 3;
    protected BluetoothDevice mBluetoothDevice = null;
    protected String mName;
    protected int kBluetoothRobotState = 0;
    protected int mConnectAttempts = 0;
    protected List<BluetoothGattService> mServicesReady;
    protected Timer mConnectTimer;
    protected int connectAttempts;
    protected int mProductId;
    protected int mInitialProductId;
    protected int mInitialBatteryLevel;
    protected int mInitialIOModes;
    protected int mInitialIOStates;
    protected static BluetoothLeService mBluetoothLeService;
    public HashMap<Byte, Byte> customBroadcastData = null;
    protected Context mContext;
    protected static boolean isBluetoothLeServiceBinded;
    protected static ServiceConnection currentServiceConnection;
    private final BluetoothLeServiceListener mBluetoothLeServiceListener = new BluetoothLeServiceListener(){

        @Override
        public void onBluetoothLeServiceConnectionStateChanged(BluetoothGatt gatt, String pAction) {
            if ("com.wowwee.bluetooth.le.ACTION_GATT_CONNECTED".equals(pAction)) {
                BluetoothRobot.this.peripheralDidConnect();
            } else if ("com.wowwee.bluetooth.le.ACTION_GATT_DISCONNECTED".equals(pAction)) {
                BluetoothRobot.this.peripheralDidDisconnect();
            } else if (!"com.wowwee.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED".equals(pAction)) {
                "com.wowwee.bluetooth.le.ACTION_DATA_AVAILABLE".equals(pAction);
            }
        }

        @Override
        public void onServicesDiscovered() {
            mBluetoothLeService.setServiceDict(BluetoothRobot.this.buildPeripheralServiceDict(mBluetoothLeService), BluetoothRobot.this.mBluetoothDevice.getAddress());
            Thread thread = new Thread(new Runnable(){

                @Override
                public void run() {
                    try {
                        Thread.sleep(500L);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    BRReceiveDataService receiveDataService = (BRReceiveDataService)mBluetoothLeService.findService("0000ffe0-0000-1000-8000-00805f9b34fb", BluetoothRobot.this.mBluetoothDevice.getAddress());
                    receiveDataService.turnOn();
                    BluetoothRobot.this.peripheralDidBecomeReady();
                }
            });
            thread.start();
        }
    };
    private final ServiceConnection mServiceConnection = new ServiceConnection(){

        public void onServiceConnected(ComponentName componentName, IBinder service) {
            currentServiceConnection = this;
            mBluetoothLeService = ((BluetoothLeService.LocalBinder)service).getService();
            mBluetoothLeService.setContext(BluetoothRobot.this.mContext);
            mBluetoothLeService.initialize();
            BluetoothRobot.this.connect();
        }

        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    static {
        isBluetoothLeServiceBinded = false;
        currentServiceConnection = null;
    }

    public BluetoothRobot(BluetoothDevice pBluetoothDevice, List<AdRecord> pScanRecords, BluetoothLeService pBluetoothLeService) {
        this.mBluetoothDevice = pBluetoothDevice;
        this.mName = this.mBluetoothDevice != null ? this.mBluetoothDevice.getName() : "<unknown>";
        if (mBluetoothLeService == null && pBluetoothLeService != null) {
            mBluetoothLeService = pBluetoothLeService;
        }
        AdRecord manufactorerData = null;
        if (pScanRecords != null) {
            for (AdRecord scanRecord : pScanRecords) {
                if (scanRecord.getType() != -1) continue;
                manufactorerData = scanRecord;
                break;
            }
        }
        if (manufactorerData != null) {
            byte[] mData = AdRecord.getRawData(manufactorerData);
            Log.d("BluetoothRobot", "mData[0] " + mData[0] + " mData[1] " + mData[1]);
            this.mInitialProductId = mData[0] != 0 ? 65535 : mData[0] << 8 | mData[1];
            this.mInitialProductId = mData[0] << 8 | mData[1];
            if (mData.length == 9) {
                this.mInitialBatteryLevel = mData[6];
                this.mInitialIOModes = mData[7];
                this.mInitialIOStates = mData[8];
            } else {
                if (this.customBroadcastData == null) {
                    this.customBroadcastData = new HashMap();
                } else {
                    this.customBroadcastData.clear();
                }
                int i = 2;
                while (i < mData.length) {
                    byte key = (byte)(i - 2);
                    byte value = mData[i];
                    this.customBroadcastData.put(key, value);
                    ++i;
                }
            }
        } else {
            Log.w("BluetoothRobot", "No manufacturer advertisement payload; continuing with limited metadata (common for paired-fallback connect path).");
        }
    }

    public HashMap<String, BRBaseService> buildPeripheralServiceDict(BluetoothLeService pBluetoothLeService) {
        HashMap<String, BRBaseService> serviceDict = new HashMap<String, BRBaseService>();
        serviceDict.put("0000ffe5-0000-1000-8000-00805f9b34fb", new BRSendDataService(pBluetoothLeService, this.mBluetoothDevice.getAddress()));
        serviceDict.put("0000ffe0-0000-1000-8000-00805f9b34fb", new BRReceiveDataService(pBluetoothLeService, this, this.mBluetoothDevice.getAddress()));
        return serviceDict;
    }

    public void disconnect() {
        this.kBluetoothRobotState = 3;
        if (mBluetoothLeService != null && this.mBluetoothDevice != null) {
            mBluetoothLeService.disconnect(this.mBluetoothDevice.getAddress());
        }
    }

    public void connect(Context context) {
        if (mBluetoothLeService == null) {
            this.startBluetoothLeService(context);
        } else {
            this.connect();
        }
    }

    public void connect() {
        if (mBluetoothLeService != null) {
            mBluetoothLeService.setServiceDict(this.buildPeripheralServiceDict(mBluetoothLeService), this.mBluetoothDevice.getAddress());
            mBluetoothLeService.setBluetoothLeServiceListener(this.mBluetoothLeServiceListener, this.mBluetoothDevice.getAddress());
            this.kBluetoothRobotState = 1;
            mBluetoothLeService.connect(this.mBluetoothDevice.getAddress());
        } else {
            Log.e("BluetoothRobot", "Failed to connect to BluetoothRobot: mBluetoothLeService = null");
        }
    }

    protected void startBluetoothLeService(Context context) {
        if (!isBluetoothLeServiceBinded) {
            this.mContext = context;
            Intent gattServiceIntent = new Intent(context, BluetoothLeService.class);
            if (context.bindService(gattServiceIntent, this.mServiceConnection, 1)) {
                Log.d("BLE", "Bind service success");
                isBluetoothLeServiceBinded = true;
            } else {
                Log.d("BLE", "Bind service failed");
            }
        }
    }

    public void peripheralDidConnect() {
        this.kBluetoothRobotState = 2;
    }

    public void peripheralDidDisconnect() {
        if (this.kBluetoothRobotState == 2) {
            Log.d("BLE", "Try to reconnect...");
            try {
                Thread.sleep(100L);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.connect();
            new Timer().schedule(new TimerTask(){

                @Override
                public void run() {
                    if (BluetoothRobot.this.kBluetoothRobotState == 1) {
                        Log.d("BLE", "Reconnect timeout, disconnect now");
                        BluetoothRobot.this.disconnect();
                        BluetoothRobot.this.peripheralDidDisconnect();
                    }
                }
            }, 2000L);
        } else {
            this.kBluetoothRobotState = 0;
            if (mBluetoothLeService != null) {
                mBluetoothLeService.disconnect(this.mBluetoothDevice.getAddress());
                mBluetoothLeService.close(this.mBluetoothDevice.getAddress());
                mBluetoothLeService.removeBluetoothLeServiceListener(this.mBluetoothDevice.getAddress());
            }
        }
    }

    public static void unbindBluetoothLeService(Context context) {
        if (isBluetoothLeServiceBinded && context != null && currentServiceConnection != null) {
            context.unbindService(currentServiceConnection);
            isBluetoothLeServiceBinded = false;
            mBluetoothLeService = null;
            currentServiceConnection = null;
        }
    }

    public void peripheralDidBecomeReady() {
        this.kBluetoothRobotState = 2;
    }

    public void sendRobotCommand(RobotCommand robotCommand) {
        this.sendRobotCommand(robotCommand, null);
    }

    private void sendRobotCommand(RobotCommand robotCommand, BRBaseService.BRServiceAction callback) {
        if (callback != null) {
            robotCommand.completedCallback = callback;
        }
        this._processRobotCommand(robotCommand);
    }

    private void _processRobotCommand(RobotCommand robotCommand) {
        this.sendRawCommandData(robotCommand.data(), robotCommand.completedCallback);
    }

    protected void sendRawCommandData(byte[] data, BRBaseService.BRServiceAction callback) {
        BRSendDataService service = (BRSendDataService)this.findService("0000ffe5-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.sendData(data, callback);
        } else {
            Log.d("BluetoothRobot", "BluetoothRobot: This device does not support Send Data Service");
        }
    }

    protected BRBaseService findService(String pUUIDString) {
        Log.i("BluetoothRobot", "pUUIDString " + pUUIDString + " getGattService " + mBluetoothLeService.getGattService(UUID.fromString(pUUIDString), this.mBluetoothDevice.getAddress()));
        if (mBluetoothLeService.getGattService(UUID.fromString(pUUIDString), this.mBluetoothDevice.getAddress()) == null) {
            return null;
        }
        return mBluetoothLeService.findService(pUUIDString, this.mBluetoothDevice.getAddress());
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        String propertyName = event.getPropertyName();
        Log.d("propertyChange", "Received propertyChange");
        if (propertyName.equals("lastRobotCommand")) {
            RobotCommand command = (RobotCommand)event.getNewValue();
            this.didReceiveRobotCommand(command);
        }
    }

    public void didReceiveRobotCommand(RobotCommand robotCommand) {
        Log.w("BluetoothRobotPrivate", "should override didReceiveRobotCommand");
    }

    public static UUID[] getAdvertisedServiceUUIDs() {
        return new UUID[]{UUID.fromString("0000ffb0-0000-1000-8000-00805f9b34fb"), UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")};
    }

    public BluetoothDevice getBluetoothDevice() {
        return this.mBluetoothDevice;
    }

    public String getName() {
        return this.mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public int getProductId() {
        return this.mInitialProductId;
    }
}

