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
import java.util.UUID;

public class BRSendDataService
extends BRBaseService {
    private static final int sendMaxChunkSize = 20;
    private boolean dataNextData = true;

    public BRSendDataService(BluetoothLeService pBluetoothLeService, String pBluetoothDeviceAddress) {
        super("sendData", UUID.fromString("0000ffe5-0000-1000-8000-00805f9b34fb"), pBluetoothLeService, pBluetoothDeviceAddress);
    }

    @Override
    public void notifyCharacteristicHandler(BluetoothGattCharacteristic pCharacteristic) {
        this.dataNextData = true;
    }

    public void sendData(byte[] data) {
        this.sendData(data, null);
    }

    public void sendData(final byte[] data, final BRBaseService.BRServiceAction writeCallback) {
        if (this.mBluetoothService != null) {
            final BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ffe9-0000-1000-8000-00805f9b34fb"));
            final BRSendDataService sendDataService = this;
            this.dataNextData = true;
            Thread t = new Thread(){

                /*
                 * WARNING - Removed try catching itself - possible behaviour change.
                 */
                @Override
                public void run() {
                    BRSendDataService bRSendDataService = sendDataService;
                    synchronized (bRSendDataService) {
                        int length = data.length;
                        int chunkSize = 20;
                        int offset = 0;
                        do {
                            int thisChunkSize = length - offset > chunkSize ? chunkSize : length - offset;
                            byte[] chunk = new byte[thisChunkSize];
                            System.arraycopy(data, offset, chunk, 0, thisChunkSize);
                            BRSendDataService.this.dataNextData = (offset += thisChunkSize) >= length;
                            if (characteristic == null) break;
                            characteristic.setValue(chunk);
                            BRSendDataService.this.mBluetoothLeService.writeCharacteristic(characteristic, BRSendDataService.this.mBluetoothDeviceAddress);
                            do {
                                try {
                                    Thread.sleep(5L);
                                }
                                catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } while (!BRSendDataService.this.dataNextData);
                        } while (offset < length);
                        if (writeCallback != null) {
                            writeCallback.serviceActionCallback(null);
                        }
                    }
                }
            };
            t.start();
        }
    }
}

