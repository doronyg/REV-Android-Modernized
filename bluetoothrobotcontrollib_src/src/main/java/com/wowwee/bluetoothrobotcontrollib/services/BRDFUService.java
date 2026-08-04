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

public class BRDFUService
extends BRBaseService {
    public BRDFUService(BluetoothLeService pBluetoothLeService, PropertyChangeListener pListener, String pBluetoothDeviceAddress) {
        super("dfu", UUID.fromString("0000ff30-0000-1000-8000-00805f9b34fb"), pBluetoothLeService, pBluetoothDeviceAddress);
        this.addPropertyChangeListener(pListener);
    }

    public void rebootToMode(byte mode) {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff31-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support btCommInterval characteristic");
                return;
            }
            characteristic.setValue(new byte[]{mode});
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.writeCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }
}

