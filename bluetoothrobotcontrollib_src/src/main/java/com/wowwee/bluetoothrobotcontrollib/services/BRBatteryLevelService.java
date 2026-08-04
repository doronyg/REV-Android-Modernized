/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.bluetooth.BluetoothGattCharacteristic
 */
package com.wowwee.bluetoothrobotcontrollib.services;

import android.bluetooth.BluetoothGattCharacteristic;
import com.wowwee.bluetoothrobotcontrollib.BluetoothLeService;
import com.wowwee.bluetoothrobotcontrollib.services.BRBaseService;
import java.beans.PropertyChangeListener;
import java.nio.ByteBuffer;
import java.util.UUID;

public class BRBatteryLevelService
extends BRBaseService {
    public static final String batteryReadingKeyPathKVO = "batteryReading";
    private int mBatteryReading = 0;

    public BRBatteryLevelService(BluetoothLeService pBluetoothLeService, PropertyChangeListener pListener, String pBluetoothDeviceAddress) {
        super("batteryLevel", UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb"), pBluetoothLeService, pBluetoothDeviceAddress);
        this.addPropertyChangeListener(pListener);
    }

    @Override
    public void notifyCharacteristicHandler(BluetoothGattCharacteristic pCharacteristic) {
        byte[] data;
        if (pCharacteristic.getUuid().toString().equals("00002a19-0000-1000-8000-00805f9b34fb") && (data = pCharacteristic.getValue()) != null && data.length > 0) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(data);
            int oldBatteryReading = this.mBatteryReading;
            this.mBatteryReading = byteBuffer.get();
            this.changes.fireIndexedPropertyChange(batteryReadingKeyPathKVO, this.mBatteryReading, oldBatteryReading, this.mBatteryReading);
        }
    }

    public void readBatteryLevel() {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb"));
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            if ((charaProp | 2) > 0) {
                this.mBluetoothLeService.readCharacteristic(characteristic, this.mBluetoothDeviceAddress);
            }
        }
    }

    public void turnOff() {
        this.setNotifications(false);
    }

    public void turnOn() {
        this.setNotifications(true);
    }

    private void setNotifications(boolean value) {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb"));
            this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, value);
        }
    }
}

