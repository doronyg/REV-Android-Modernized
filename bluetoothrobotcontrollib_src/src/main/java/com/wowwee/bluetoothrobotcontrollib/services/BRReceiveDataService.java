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
import com.wowwee.bluetoothrobotcontrollib.RobotCommand;
import com.wowwee.bluetoothrobotcontrollib.services.BRBaseService;
import java.beans.PropertyChangeListener;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

public class BRReceiveDataService
extends BRBaseService {
    public static final String commandKeyPathKVO = "lastRobotCommand";
    public static final String rawDataKeyPathKVO = "lastCommandData";
    public static final String firmwareKeyPathKVO = "lastFirmwareCommand";
    public boolean processRobotCommands;
    public boolean firmwareUpdateMode;
    private RobotCommand lastRobotCommand;
    private byte[] lastCommandData;
    private byte[] lastFirmwareCommand;

    public BRReceiveDataService(BluetoothLeService pBluetoothLeService, PropertyChangeListener pListener, String pBluetoothDeviceAddress) {
        super("recieveData", UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"), pBluetoothLeService, pBluetoothDeviceAddress);
        this.addPropertyChangeListener(pListener);
        this.firmwareUpdateMode = false;
        this.processRobotCommands = true;
        this.lastCommandData = null;
        this.lastRobotCommand = null;
    }

    @Override
    public void notifyCharacteristicHandler(BluetoothGattCharacteristic pCharacteristic) {
        byte[] data;
        if (pCharacteristic.getUuid().toString().equals("0000ffe4-0000-1000-8000-00805f9b34fb") && (data = pCharacteristic.getValue()) != null && data.length > 0) {
            ArrayList<Byte> byteList = new ArrayList<Byte>();
            boolean isUnsigned = false;
            int i = 0;
            while (i < data.length) {
                if (data[i] < 0) {
                    isUnsigned = true;
                }
                byteList.add(data[i]);
                ++i;
            }
            String ascii = null;
            if (!isUnsigned) {
                ascii = new String(data, StandardCharsets.US_ASCII);
            }
            Log.d("Bootloader", "ascii = " + ascii + ", hex = " + RobotCommand.byteArrayToHexString(data));
            byte[] oldData = this.lastCommandData;
            this.lastCommandData = data;
            this.changes.fireIndexedPropertyChange(rawDataKeyPathKVO, 0, oldData, this.lastCommandData);
            if (!this.processRobotCommands) {
                Log.e("MipRobot", "ReceiveData !processRobotCommands return");
                return;
            }
            if (this.firmwareUpdateMode) {
                byte[] dat = this.lastFirmwareCommand;
                this.lastFirmwareCommand = data;
                this.changes.fireIndexedPropertyChange(firmwareKeyPathKVO, 0, dat, this.lastFirmwareCommand);
                return;
            }
            try {
                RobotCommand robotCommand = null;
                // Some Android stacks deliver non-printable ASCII that is not actually hex text.
                // Prefer binary parsing unless payload is a valid ASCII-hex command frame.
                if (!isUnsigned && isAsciiHexString(ascii)) {
                    robotCommand = RobotCommand.create(ascii);
                } else {
                    robotCommand = RobotCommand.create(data);
                }
                if (robotCommand != null) {
                    RobotCommand oldRobotCommand = this.lastRobotCommand;
                    this.lastRobotCommand = robotCommand;
                    this.changes.fireIndexedPropertyChange(commandKeyPathKVO, 1, oldRobotCommand, this.lastRobotCommand);
                } else {
                    Log.e("MipRobot", "ReceiveData robotCommand is null");
                }
            }
            catch (Exception e) {
                Log.e("MipRobot", "ReceiveData command parse failed", e);
            }
        }
    }

    private boolean isAsciiHexString(String ascii) {
        if (ascii == null) {
            return false;
        }
        String normalized = ascii.trim();
        if (normalized.length() < 2 || normalized.length() % 2 != 0) {
            return false;
        }
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            boolean isHex = c >= '0' && c <= '9'
                    || c >= 'a' && c <= 'f'
                    || c >= 'A' && c <= 'F';
            if (!isHex) {
                return false;
            }
        }
        return true;
    }

    public void turnOff() {
        this.setNotifications(false);
    }

    public void turnOn() {
        this.setNotifications(true);
    }

    private void setNotifications(boolean value) {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ffe4-0000-1000-8000-00805f9b34fb"));
            this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, value);
        }
    }
}

