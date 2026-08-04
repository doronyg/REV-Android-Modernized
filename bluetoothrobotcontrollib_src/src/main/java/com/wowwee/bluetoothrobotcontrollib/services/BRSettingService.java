/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.bluetooth.BluetoothGattCharacteristic
 *  android.util.Log
 */
package com.wowwee.bluetoothrobotcontrollib.services;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;
import com.wowwee.bluetoothrobotcontrollib.BluetoothLeService;
import com.wowwee.bluetoothrobotcontrollib.services.BRBaseService;
import java.beans.PropertyChangeListener;
import java.util.UUID;

public class BRSettingService
extends BRBaseService {
    private int activationStatus = -1;
    public static final String activationStatusKeyPathKVO = "activationStatus";

    public BRSettingService(BluetoothLeService pBluetoothLeService, PropertyChangeListener pListener, String pBluetoothDeviceAddress) {
        super("deviceSetting", UUID.fromString("0000ff10-0000-1000-8000-00805f9b34fb"), pBluetoothLeService, pBluetoothDeviceAddress);
        this.addPropertyChangeListener(pListener);
    }

    @Override
    public void notifyCharacteristicHandler(BluetoothGattCharacteristic pCharacteristic) {
        byte[] data;
        if (pCharacteristic.getUuid().toString().equals("0000ff1b-0000-1000-8000-00805f9b34fb") && (data = pCharacteristic.getValue()).length >= 1) {
            int value = data[0];
            int oldActivationStatus = this.activationStatus;
            this.activationStatus = value;
            this.changes.fireIndexedPropertyChange(activationStatusKeyPathKVO, 1, String.valueOf(oldActivationStatus), String.valueOf(this.activationStatus));
            Log.d("BLE", "BRProductionActivationService receive kProductActivationCharacteristicUUID: " + oldActivationStatus + " " + this.activationStatus);
        }
    }

    public void readProductActivationStatus() {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff1b-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRProductionActivationService", "This device does not support readActivationStatus characteristic");
                return;
            }
            this.mBluetoothLeService.readCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }

    public void writeProductActivationStatus(int activationType) {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff1b-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRProductionActivationService", "This device does not support writeActivationStatus characteristic");
                return;
            }
            characteristic.setValue(new byte[]{(byte)activationType});
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.writeCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }
}

