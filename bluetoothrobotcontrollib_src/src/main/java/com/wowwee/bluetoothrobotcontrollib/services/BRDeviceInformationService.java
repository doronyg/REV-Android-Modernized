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
import com.wowwee.bluetoothrobotcontrollib.util.AdRecord;
import java.beans.PropertyChangeListener;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

public class BRDeviceInformationService
extends BRBaseService {
    public static final String systemIdKeyPathKVO = "systemId";
    public static final String softwareVersionKeyPathKVO = "moduleSoftwareVersion";
    public String systemid;
    public String moduleSoftwareVersion;

    public static void reverse(byte[] array) {
        if (array == null) {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        while (j > i) {
            byte tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            --j;
            ++i;
        }
    }

    public BRDeviceInformationService(BluetoothLeService pBluetoothLeService, PropertyChangeListener pListener, String pBluetoothDeviceAddress) {
        super("deviceInfo", UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb"), pBluetoothLeService, pBluetoothDeviceAddress);
        this.addPropertyChangeListener(pListener);
    }

    @Override
    public void notifyCharacteristicHandler(BluetoothGattCharacteristic pCharacteristic) {
        byte[] data;
        if (pCharacteristic.getUuid().toString().equals("00002a23-0000-1000-8000-00805f9b34fb")) {
            byte[] data2 = pCharacteristic.getValue();
            if (data2 != null && data2.length > 0) {
                String systemIdentifier;
                BRDeviceInformationService.reverse(data2);
                String oldSystemId = this.systemid;
                this.systemid = systemIdentifier = AdRecord.bytesToHex(data2);
                this.changes.fireIndexedPropertyChange(systemIdKeyPathKVO, 1, oldSystemId, this.systemid);
                Log.d("BLE", "BRDeviceInformationService receive system id: " + systemIdentifier);
            }
        } else if (pCharacteristic.getUuid().toString().equals("00002a26-0000-1000-8000-00805f9b34fb") && (data = pCharacteristic.getValue()) != null && data.length > 0) {
            String ascii = null;
            ascii = new String(data, StandardCharsets.US_ASCII);
            String oldModuleVersion = this.moduleSoftwareVersion;
            this.moduleSoftwareVersion = ascii;
            this.changes.fireIndexedPropertyChange(softwareVersionKeyPathKVO, 2, oldModuleVersion, this.moduleSoftwareVersion);
            Log.d("BLE", "BRDeviceInformationService receive module software version: " + this.moduleSoftwareVersion);
        }
    }

    public void readSystemId() {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("00002a23-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support SystemID characteristic");
                return;
            }
            int charaProp = characteristic.getProperties();
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                this.mBluetoothLeService.readCharacteristic(characteristic, this.mBluetoothDeviceAddress);
            } else {
                Log.w("BRDeviceInformationServer", "readSystemId(): characteristic is not readable; skipping read");
            }
        }
    }

    public void readModuleSoftwareVersion() {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support module software version characteristic");
                return;
            }
            int charaProp = characteristic.getProperties();
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
                Log.d("BRDeviceInformationServer", "readModuleSoftwareVersion(): notify enabled");
            }
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                this.mBluetoothLeService.readCharacteristic(characteristic, this.mBluetoothDeviceAddress);
            } else {
                Log.w("BRDeviceInformationServer", "readModuleSoftwareVersion(): characteristic is not readable; skipping read");
            }
        }
    }
}

