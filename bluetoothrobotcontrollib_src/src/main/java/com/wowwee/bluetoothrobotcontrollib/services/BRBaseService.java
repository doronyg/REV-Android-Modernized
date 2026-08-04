/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.bluetooth.BluetoothGattCharacteristic
 *  android.bluetooth.BluetoothGattDescriptor
 *  android.bluetooth.BluetoothGattService
 */
package com.wowwee.bluetoothrobotcontrollib.services;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import com.wowwee.bluetoothrobotcontrollib.BluetoothLeService;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.UUID;

public class BRBaseService {
    protected BluetoothLeService mBluetoothLeService;
    protected BluetoothGattService mBluetoothService;
    protected String mName;
    protected UUID mUuid;
    protected String mBluetoothDeviceAddress;
    protected PropertyChangeSupport changes = new PropertyChangeSupport(this);
    protected boolean isOptional;

    public BRBaseService(String pName, UUID uuid, BluetoothLeService pBluetoothLeService, String pBluetoothDeviceAddress) {
        this.mBluetoothLeService = pBluetoothLeService;
        this.mName = pName;
        this.mUuid = uuid;
        this.mBluetoothDeviceAddress = pBluetoothDeviceAddress;
        this.configureBluetoothService();
    }

    public void configureBluetoothService() {
        this.mBluetoothService = this.mBluetoothLeService.getGattService(this.mUuid, this.mBluetoothDeviceAddress);
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        this.changes.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        this.changes.removePropertyChangeListener(l);
    }

    public void notifyCharacteristicHandler(BluetoothGattCharacteristic pCharacteristic) {
    }

    public void descriptorHandler(BluetoothGattDescriptor pdescriptor) {
    }

    public interface BRServiceAction {
        void serviceActionCallback(Error var1);
    }
}

