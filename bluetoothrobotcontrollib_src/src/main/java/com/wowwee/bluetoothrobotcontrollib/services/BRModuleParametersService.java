/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.bluetooth.BluetoothGattCharacteristic
 *  android.util.Log
 *  org.apache.http.util.ByteArrayBuffer
 */
package com.wowwee.bluetoothrobotcontrollib.services;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;
import com.wowwee.bluetoothrobotcontrollib.BluetoothLeService;
import com.wowwee.bluetoothrobotcontrollib.BluetoothRobotConstants;
import com.wowwee.bluetoothrobotcontrollib.BluetoothRobotConstantsBase;
import com.wowwee.bluetoothrobotcontrollib.services.BRBaseService;
import java.beans.PropertyChangeListener;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BRModuleParametersService
extends BRBaseService {
    public static final String deviceNameKeyPathKVO = "deviceName";
    public static final String btCommIntervalKeyPathKVO = "btCommInterval";
    public static final String uartBaudRateKeyPathKVO = "moduleUartBaudRate";
    public static final String broadcastPeriodKeyPathKVO = "btBroadcastPeriod";
    public static final String productIdKeyPathKVO = "productId";
    public static final String transmitPowerKeyPathKVO = "moduleTransmitPower";
    public static final String customBroadcastDataKeyPathKVO = "customBroadcastData";
    public static final String pulseSleepModeKeyPathKVO = "pulseSleepMode";
    private final BluetoothRobotConstants.kModuleParameterUARTBaudRateValues uartBuadDefaultValue = BluetoothRobotConstants.kModuleParameterUARTBaudRateValues.kModuleParameterUARTBaudRate9600;
    private final BluetoothRobotConstantsBase.kModuleParameterBTCommunicationIntervalValues btCommIntervalDefaultValue = BluetoothRobotConstantsBase.kModuleParameterBTCommunicationIntervalValues.kModuleParameterBTCommunicationInterval20ms;
    private final BluetoothRobotConstantsBase.kModuleParameter_BroadcastPeriodValues broadcastPeriodDefaultValue = BluetoothRobotConstantsBase.kModuleParameter_BroadcastPeriodValues.kModuleParameter_BroadcastPeriod200MS;
    private final BluetoothRobotConstantsBase.kMipModuleParameter_TransmitPowerValues transmitPowerDefaultValue = BluetoothRobotConstantsBase.kMipModuleParameter_TransmitPowerValues.kMipModuleParameter_TransmitPower_0dBm;
    private String deviceName;
    private BluetoothRobotConstantsBase.kModuleParameterBTCommunicationIntervalValues btCommInterval;
    private BluetoothRobotConstants.kModuleParameterUARTBaudRateValues moduleUartBaudRate;
    private BluetoothRobotConstantsBase.kModuleParameter_BroadcastPeriodValues btBroadcastPeriod;
    private short productId;
    private BluetoothRobotConstantsBase.kMipModuleParameter_TransmitPowerValues moduleTransmitPower;
    private HashMap<Byte, Byte> customBroadcastData;
    private boolean pulseSleepMode;
    private String deviceNameInProgres = null;
    private boolean isSettingsDeviceName = false;

    public BRModuleParametersService(BluetoothLeService pBluetoothLeService, PropertyChangeListener pListener, String pBluetoothDeviceAddress) {
        super("moduleParams", UUID.fromString("0000ff90-0000-1000-8000-00805f9b34fb"), pBluetoothLeService, pBluetoothDeviceAddress);
        this.addPropertyChangeListener(pListener);
        this.moduleUartBaudRate = this.uartBuadDefaultValue;
        this.btCommInterval = this.btCommIntervalDefaultValue;
        this.btBroadcastPeriod = this.broadcastPeriodDefaultValue;
        this.pulseSleepMode = false;
        this.customBroadcastData = null;
        this.moduleTransmitPower = this.transmitPowerDefaultValue;
        this.productId = 0;
        if (pBluetoothLeService.getGatt(this.mBluetoothDeviceAddress) != null) {
            this.deviceName = pBluetoothLeService.getGatt(this.mBluetoothDeviceAddress).getDevice().getName();
        }
    }

    @Override
    public void notifyCharacteristicHandler(BluetoothGattCharacteristic pCharacteristic) {
        byte[] data;
        if (pCharacteristic.getUuid().toString().equals("0000ff91-0000-1000-8000-00805f9b34fb")) {
            if (this.isSettingsDeviceName && this.deviceNameInProgres != null) {
                String oldDeviceName = this.deviceName;
                this.deviceName = this.deviceNameInProgres;
                this.deviceNameInProgres = null;
                Log.d("BLE", "BRModuleParametersService receive new name: " + this.deviceName);
                this.changes.fireIndexedPropertyChange(deviceNameKeyPathKVO, 1, oldDeviceName, this.deviceName);
            } else {
                byte[] data2 = pCharacteristic.getValue();
                if (data2 != null && data2.length > 0) {
                    String asciiDeviceName = null;
                    asciiDeviceName = new String(data2, StandardCharsets.US_ASCII);
                    String oldDeviceName = this.deviceName;
                    this.deviceName = asciiDeviceName;
                    this.changes.fireIndexedPropertyChange(deviceNameKeyPathKVO, 1, oldDeviceName, this.deviceName);
                    Log.d("BLE", "BRModuleParametersService receive name: " + this.deviceName);
                }
            }
            this.isSettingsDeviceName = false;
        } else if (pCharacteristic.getUuid().toString().equals("0000ff92-0000-1000-8000-00805f9b34fb")) {
            byte[] data3 = pCharacteristic.getValue();
            if (data3 != null && data3.length > 0) {
                byte payload = data3[0];
                BluetoothRobotConstantsBase.kModuleParameterBTCommunicationIntervalValues oldParam = this.btCommInterval;
                this.btCommInterval = BluetoothRobotConstantsBase.kModuleParameterBTCommunicationIntervalValues.getParamWithValue(payload);
                this.changes.fireIndexedPropertyChange(btCommIntervalKeyPathKVO, 2, oldParam, this.btCommInterval);
                Log.d("BLE", "BRModuleParametersService receive btCommInterval: " + this.btCommInterval.getValue());
            }
        } else if (pCharacteristic.getUuid().toString().equals("0000ff93-0000-1000-8000-00805f9b34fb")) {
            byte[] data4 = pCharacteristic.getValue();
            if (data4 != null && data4.length > 0) {
                byte payload = data4[0];
                BluetoothRobotConstants.kModuleParameterUARTBaudRateValues oldParam = this.moduleUartBaudRate;
                this.moduleUartBaudRate = BluetoothRobotConstants.kModuleParameterUARTBaudRateValues.getParamWithValue(payload);
                this.changes.fireIndexedPropertyChange(uartBaudRateKeyPathKVO, 3, oldParam, this.moduleUartBaudRate);
                Log.d("BLE", "BRModuleParametersService receive moduleUartBaudRate: " + this.moduleUartBaudRate.getValue());
            }
        } else if (pCharacteristic.getUuid().toString().equals("0000ff95-0000-1000-8000-00805f9b34fb")) {
            byte[] data5 = pCharacteristic.getValue();
            if (data5 != null && data5.length > 0) {
                byte payload = data5[0];
                BluetoothRobotConstantsBase.kModuleParameter_BroadcastPeriodValues oldParam = this.btBroadcastPeriod;
                this.btBroadcastPeriod = BluetoothRobotConstantsBase.kModuleParameter_BroadcastPeriodValues.getParamWithValue(payload);
                this.changes.fireIndexedPropertyChange(broadcastPeriodKeyPathKVO, 4, oldParam, this.btBroadcastPeriod);
                Log.d("BLE", "BRModuleParametersService receive btBroadcastPeriod: " + this.btBroadcastPeriod.getValue());
            }
        } else if (pCharacteristic.getUuid().toString().equals("0000ff96-0000-1000-8000-00805f9b34fb")) {
            byte[] data6 = pCharacteristic.getValue();
            if (data6 != null && data6.length > 1) {
                Short oldProductId = this.productId;
                this.productId = (short)((data6[1] << 8) + (data6[0] & 0xFF));
                Short newProductId = this.productId;
                this.changes.fireIndexedPropertyChange(productIdKeyPathKVO, 5, oldProductId, newProductId);
                Log.d("BLE", "BRModuleParametersService receive btBroadcastPeriod: " + this.btBroadcastPeriod.getValue());
            }
        } else if (pCharacteristic.getUuid().toString().equals("0000ff97-0000-1000-8000-00805f9b34fb")) {
            byte[] data7 = pCharacteristic.getValue();
            if (data7 != null && data7.length > 0) {
                byte payload = data7[0];
                BluetoothRobotConstantsBase.kMipModuleParameter_TransmitPowerValues oldParam = this.moduleTransmitPower;
                this.moduleTransmitPower = BluetoothRobotConstantsBase.kMipModuleParameter_TransmitPowerValues.getParamWithValue(payload);
                this.changes.fireIndexedPropertyChange(transmitPowerKeyPathKVO, 5, oldParam, this.moduleTransmitPower);
                Log.d("BLE", "BRModuleParametersService receive moduleTransmitPower: " + this.moduleTransmitPower.getValue());
            }
        } else if (pCharacteristic.getUuid().toString().equals("0000ff98-0000-1000-8000-00805f9b34fb")) {
            byte[] data8 = pCharacteristic.getValue();
            if (data8 != null && data8.length > 0) {
                int len = data8.length;
                HashMap<Byte, Byte> newCustomBroadcastData = new HashMap<Byte, Byte>();
                int i = 0;
                while (i < len - 1) {
                    newCustomBroadcastData.put(data8[i], data8[i + 1]);
                    i += 2;
                }
                Log.d("BLE", "BRModuleParametersService receive customBroadcastData: " + this.customBroadcastData.toString());
            }
        } else if (pCharacteristic.getUuid().toString().equals("0000ff9A-0000-1000-8000-00805f9b34fb") && (data = pCharacteristic.getValue()) != null && data.length > 0) {
            byte payload = data[0];
            boolean oldPulseSleepMode = this.pulseSleepMode;
            this.pulseSleepMode = payload != 0;
            this.changes.fireIndexedPropertyChange(pulseSleepModeKeyPathKVO, 7, oldPulseSleepMode, this.pulseSleepMode);
            Log.d("BLE", "BRModuleParametersService receive pulseSleepMode: " + this.pulseSleepMode);
        }
    }

    public void readBTDeviceName() {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff91-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support deviceName characteristic");
                return;
            }
            this.isSettingsDeviceName = false;
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.readCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }

    public void setBTDeviceName(String newDeviceName) {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff91-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support deviceName characteristic");
                return;
            }
            if (newDeviceName == null) {
                Log.e("BRModuleParameterService", "Device name cannot be null");
                return;
            }
            int maxLength = 15;
            if (newDeviceName.length() > 15) {
                Log.w("BRModuleParameterService", "Device Name [" + newDeviceName + "] cannot be longer than " + 15 + " characters, trimming to fit");
            }
            String trimmedDeviceName = newDeviceName.substring(0, Math.min(15, newDeviceName.length()));
            byte[] deviceNameData = null;
            deviceNameData = trimmedDeviceName.getBytes(StandardCharsets.US_ASCII);
            if (deviceNameData != null) {
                this.isSettingsDeviceName = true;
                this.deviceNameInProgres = trimmedDeviceName;
                byte[] payload = new byte[deviceNameData.length + 1];
                System.arraycopy(deviceNameData, 0, payload, 0, deviceNameData.length);
                payload[payload.length - 1] = 0;
                characteristic.setValue(payload);
                int charaProp = characteristic.getProperties();
                if ((charaProp | 0x10) > 0) {
                    this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
                }
                this.mBluetoothLeService.writeCharacteristic(characteristic, this.mBluetoothDeviceAddress);
            }
        }
    }

    public void readBTCommunicationInterval() {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff92-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support btCommInterval characteristic");
                return;
            }
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.readCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }

    public void setBTCommunicationInterval(BluetoothRobotConstantsBase.kModuleParameterBTCommunicationIntervalValues value) {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff92-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support btCommInterval characteristic");
                return;
            }
            characteristic.setValue(new byte[]{value.getValue()});
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.writeCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }

    public void readUartBaudRate() {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff93-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support uartBaudRate characteristic");
                return;
            }
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.readCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }

    public void setUartBuadRate(BluetoothRobotConstants.kModuleParameterUARTBaudRateValues value) {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff93-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support uartBaudRate characteristic");
                return;
            }
            characteristic.setValue(new byte[]{value.getValue()});
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.writeCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }

    public void restartModule() {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff94-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support resetModule characteristic");
                return;
            }
            characteristic.setValue(new byte[]{BluetoothRobotConstantsBase.kModuleParameterValues.kModuleParameter_RestartModule.getValue()});
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.writeCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }

    public void restoreUserDataToFactorySettings() {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff94-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support resetModule characteristic");
                return;
            }
            characteristic.setValue(new byte[]{BluetoothRobotConstantsBase.kModuleParameterValues.kModuleParameter_ResetModuleResetUserData.getValue()});
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.writeCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }

    public void restoreFullyFactorySettings() {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff94-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support resetModule characteristic");
                return;
            }
            characteristic.setValue(new byte[]{BluetoothRobotConstantsBase.kModuleParameterValues.kModuleParameter_ResetModuleRestoreFactorySettings.getValue()});
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.writeCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }

    public void setBroadcastPeriod(BluetoothRobotConstantsBase.kModuleParameter_BroadcastPeriodValues period) {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff95-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support setOrReadBroadcastPeriod characteristic");
                return;
            }
            characteristic.setValue(new byte[]{period.getValue()});
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.writeCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }

    public void readBroadcastPeriod() {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff95-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support setOrReadBroadcastPeriod characteristic");
                return;
            }
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.readCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }

    public void setProductIdentifier(short productIdentifier) {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff96-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support setOrReadProductID characteristic");
                return;
            }
            byte[] productIdentifierData = new byte[]{(byte)(productIdentifier & 0xFF), (byte)(productIdentifier >> 8 & 0xFF)};
            characteristic.setValue(productIdentifierData);
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.writeCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }

    public void readProductIdentifier() {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff96-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support setOrReadProductID characteristic");
                return;
            }
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.readCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }

    public void setTransmitPower(BluetoothRobotConstantsBase.kMipModuleParameter_TransmitPowerValues transmitPower) {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff97-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support setOrReadTransmitPower characteristic");
                return;
            }
            characteristic.setValue(new byte[]{transmitPower.getValue()});
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.writeCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }

    public void readTransmitPower() {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff97-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support setOrReadTransmitPower characteristic");
                return;
            }
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.readCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }

    public void setBroadcastData(HashMap<Byte, Byte> broadcastData) {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff98-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support setOrReadCustomBroadcastData characteristic");
                return;
            }
            byte[] customData = new byte[16];
            for (Map.Entry<Byte, Byte> pairs : broadcastData.entrySet()) {
                byte byteData;
                byte index = pairs.getKey();
                customData[index] = byteData = pairs.getValue().byteValue();
            }
            characteristic.setValue(customData);
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.writeCharacteristic(characteristic, this.mBluetoothDeviceAddress);
            HashMap<Byte, Byte> oldBroadcastData = this.customBroadcastData;
            this.customBroadcastData = broadcastData;
            this.changes.fireIndexedPropertyChange(customBroadcastDataKeyPathKVO, 6, oldBroadcastData, this.customBroadcastData);
        }
    }

    public void setBroadcastDataToDefault() {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff98-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support setOrReadCustomBroadcastData characteristic");
                return;
            }
            byte[] payloadData = new byte[16];
            characteristic.setValue(payloadData);
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.writeCharacteristic(characteristic, this.mBluetoothDeviceAddress);
            this.customBroadcastData = null;
        }
    }

    public void readBroadcastData() {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff98-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support setOrReadCustomBroadcastData characteristic");
                return;
            }
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.readCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }

    public void forceModuleSleep() {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff99-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support setRemoteControlExtension characteristic");
                return;
            }
            if (!this.pulseSleepMode) {
                Log.w("BRModuleParameterService", "The function forceModuleSleep is only available when in pulseSleepMode = true");
                return;
            }
            characteristic.setValue(new byte[]{BluetoothRobotConstantsBase.kMipModuleParameter_RemoteControlExtensionValues.kMipModuleParameter_RemoteControlExtension_ForceSleepMode.getValue()});
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.writeCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }

    public void saveCurrentIOOutputInputState() {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff99-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support setRemoteControlExtension characteristic");
                return;
            }
            characteristic.setValue(new byte[]{BluetoothRobotConstantsBase.kMipModuleParameter_RemoteControlExtensionValues.kMipModuleParameter_RemoteControlExtension_SaveIOState.getValue()});
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.writeCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }

    public void setStandbyPulsedSleepMode(boolean pulsedSleep) {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff9A-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support readOrWriteStandbyMode characteristic");
                return;
            }
            byte payloadValue = 0;
            if (pulsedSleep) {
                payloadValue = 1;
            }
            characteristic.setValue(new byte[]{payloadValue});
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.writeCharacteristic(characteristic, this.mBluetoothDeviceAddress);
            boolean oldPulseSleepMode = this.pulseSleepMode;
            this.pulseSleepMode = pulsedSleep;
            this.changes.fireIndexedPropertyChange(pulseSleepModeKeyPathKVO, 7, oldPulseSleepMode, this.pulseSleepMode);
        }
    }

    public void readStandbyPulsedSleepMode() {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff9A-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support readOrWriteStandbyMode characteristic");
                return;
            }
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.readCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }

    public void writeCurrentCustomBroadcastDataToFlash() {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff99-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support writeCurrentCustomBroadcastDataToFlash characteristic");
                return;
            }
            characteristic.setValue(new byte[]{BluetoothRobotConstantsBase.kMipModuleParameter_RemoteControlExtensionValues.kMipModuleParameter_RemoteControlExtension_WriteCustomBroadcastDataToFlash.getValue()});
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.writeCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }

    public void disconnectCurrentBluetoothClient() {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff9A-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support disconnectCurrentBluetoothClient characteristic");
                return;
            }
            characteristic.setValue(new byte[]{BluetoothRobotConstantsBase.kMipModuleParameter_RemoteControlExtensionValues.kMipModuleParameter_RemoteControlExtension_DisconnectBluetoothClient.getValue()});
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.writeCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }

    public void setConnectedBroadcastData(byte[] data) {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff9B-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support setConnectedBroadcastData characteristic");
                return;
            }
            characteristic.setValue(data);
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.writeCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }

    public void setConnectedBroadcastOn(boolean isOn) {
        if (this.mBluetoothService != null) {
            BluetoothGattCharacteristic characteristic = this.mBluetoothService.getCharacteristic(UUID.fromString("0000ff9C-0000-1000-8000-00805f9b34fb"));
            if (characteristic == null) {
                Log.e("BRModuleParameterService", "This device does not support setConnectedBroadcastOn characteristic");
                return;
            }
            byte payloadValue = 0;
            if (isOn) {
                payloadValue = 1;
            }
            characteristic.setValue(new byte[]{payloadValue});
            int charaProp = characteristic.getProperties();
            if ((charaProp | 0x10) > 0) {
                this.mBluetoothLeService.setCharacteristicNotification(characteristic, this.mBluetoothDeviceAddress, true);
            }
            this.mBluetoothLeService.writeCharacteristic(characteristic, this.mBluetoothDeviceAddress);
        }
    }
}

