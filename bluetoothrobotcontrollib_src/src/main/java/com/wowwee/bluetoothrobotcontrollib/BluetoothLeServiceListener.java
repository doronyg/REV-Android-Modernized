/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.bluetooth.BluetoothGatt
 */
package com.wowwee.bluetoothrobotcontrollib;

import android.bluetooth.BluetoothGatt;

public interface BluetoothLeServiceListener {
    void onBluetoothLeServiceConnectionStateChanged(BluetoothGatt var1, String var2);

    void onServicesDiscovered();
}

