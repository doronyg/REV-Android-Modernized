/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.bluetooth.BluetoothAdapter
 *  android.content.Context
 */
package com.wowwee.bluetoothrobotcontrollib;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import com.wowwee.bluetoothrobotcontrollib.BluetoothRobot;

public class BluetoothRobotFinder {
    public boolean autoConnectToKnownRobotIdentifiers = true;
    protected BluetoothAdapter mBluetoothAdapter;
    protected Context mContext;

    public void addAutoConnectRobot(BluetoothRobot pRobot) {
    }

    public void removeAutoConnectRobot(BluetoothRobot pRobot) {
    }

    public void setBluetoothAdapter(BluetoothAdapter pBluetoothAdapter) {
        this.mBluetoothAdapter = pBluetoothAdapter;
    }

    public void setApplicationContext(Context pContext) {
        this.mContext = pContext;
    }
}

