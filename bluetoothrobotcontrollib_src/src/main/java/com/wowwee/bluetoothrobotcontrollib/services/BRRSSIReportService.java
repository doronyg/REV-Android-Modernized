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

public class BRRSSIReportService
extends BRBaseService {
    public static final String rssiLevelKVO = "rssiLevel";
    public int mRssiLevel = 0;
    public int mNotificationPeriod = 0;
    public boolean mIsNotifying = false;

    public BRRSSIReportService(BluetoothLeService pBluetoothLeService, PropertyChangeListener pListener, String pBluetoothDeviceAddress) {
        super("rssiReport", UUID.fromString("0000ffa0-0000-1000-8000-00805f9b34fb"), pBluetoothLeService, pBluetoothDeviceAddress);
        this.addPropertyChangeListener(pListener);
        this.mIsNotifying = false;
    }

    @Override
    public void notifyCharacteristicHandler(BluetoothGattCharacteristic pCharacteristic) {
        byte[] data;
        if (!pCharacteristic.getUuid().toString().equals("0000ffa2-0000-1000-8000-00805f9b34fb") && pCharacteristic.getUuid().toString().equals("0000ffa1-0000-1000-8000-00805f9b34fb") && (data = pCharacteristic.getValue()) != null && data.length > 0) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(data);
            int oldRssiLevel = this.mRssiLevel;
            this.mRssiLevel = byteBuffer.get();
            this.changes.fireIndexedPropertyChange(rssiLevelKVO, this.mRssiLevel, oldRssiLevel, this.mRssiLevel);
        }
    }

    public void setConfigPeriod(byte value) {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ffa2-0000-1000-8000-00805f9b34fb"));
            characteristic.setValue(new byte[]{value});
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            if ((charaProp | 8) > 0) {
                this.mBluetoothLeService.writeCharacteristic(characteristic, this.mBluetoothDeviceAddress);
            }
            this.mNotificationPeriod = value;
        }
    }

    public void readConfigPeriod() {
    }

    public void readRSSI() {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ffa1-0000-1000-8000-00805f9b34fb"));
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
        this.setConfigPeriod((byte)0);
        this.setNotifications(false);
    }

    public void turnOnWithPeriod(byte period) {
        this.setConfigPeriod(period);
        this.setNotifications(true);
    }

    private void setNotifications(boolean value) {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ffa1-0000-1000-8000-00805f9b34fb"));
            this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, value);
        }
    }
}

