/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.bluetooth.BluetoothDevice
 *  android.util.Log
 */
package com.wowwee.bluetoothrobotcontrollib;

import android.bluetooth.BluetoothDevice;
import android.util.Log;
import com.wowwee.bluetoothrobotcontrollib.BluetoothLeService;
import com.wowwee.bluetoothrobotcontrollib.BluetoothRobot;
import com.wowwee.bluetoothrobotcontrollib.BluetoothRobotConstants;
import com.wowwee.bluetoothrobotcontrollib.BluetoothRobotConstantsBase;
import com.wowwee.bluetoothrobotcontrollib.RobotCommand;
import com.wowwee.bluetoothrobotcontrollib.services.BRBaseService;
import com.wowwee.bluetoothrobotcontrollib.services.BRBatteryLevelService;
import com.wowwee.bluetoothrobotcontrollib.services.BRDFUService;
import com.wowwee.bluetoothrobotcontrollib.services.BRDeviceInformationService;
import com.wowwee.bluetoothrobotcontrollib.services.BRModuleParametersService;
import com.wowwee.bluetoothrobotcontrollib.services.BRRSSIReportService;
import com.wowwee.bluetoothrobotcontrollib.services.BRReceiveDataService;
import com.wowwee.bluetoothrobotcontrollib.services.BRSendDataService;
import com.wowwee.bluetoothrobotcontrollib.services.BRSettingService;
import com.wowwee.bluetoothrobotcontrollib.util.AdRecord;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;

public class BluetoothRobotPrivate
extends BluetoothRobot
implements PropertyChangeListener {
    public static byte kActivation_FactoryDefault = 0;
    public static byte kActivation_Activate = 1;
    public static byte kActivation_ActivationSentToFlurry = (byte)2;
    public static byte kActivation_HackerUartUsed = (byte)4;
    public static byte kActivation_HackerUartUsedSentToFlurry = (byte)8;
    public Boolean isDFUSupported;
    protected byte toyActivationStatus;
    public String bleSystemId;
    public String bleModuleSoftwareVersion;

    public BluetoothRobotPrivate(BluetoothDevice pBluetoothDevice, List<AdRecord> pScanRecords, BluetoothLeService pBluetoothLeService) {
        super(pBluetoothDevice, pScanRecords, pBluetoothLeService);
    }

    @Override
    public HashMap<String, BRBaseService> buildPeripheralServiceDict(BluetoothLeService pBluetoothLeService) {
        HashMap<String, BRBaseService> serviceDict = super.buildPeripheralServiceDict(pBluetoothLeService);
        serviceDict.put("0000ff30-0000-1000-8000-00805f9b34fb", new BRDFUService(pBluetoothLeService, this, this.mBluetoothDevice.getAddress()));
        serviceDict.put("0000ff31-0000-1000-8000-00805f9b34fb", new BRDFUService(pBluetoothLeService, this, this.mBluetoothDevice.getAddress()));
        return serviceDict;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        String propertyName = event.getPropertyName();
        if (propertyName.equals("batteryReading")) {
            int newBatteryLevel = (Integer)event.getNewValue();
            this.didReceiveBatteryUpdate(newBatteryLevel);
        } else if (propertyName.equals("rssiLevel")) {
            int newRssi = (Integer)event.getNewValue();
            this.didReceiveBluetoothRSSIUpdate(newRssi);
        } else if (propertyName.equals("lastRobotCommand")) {
            RobotCommand command = (RobotCommand)event.getNewValue();
            this.didReceiveRobotCommand(command);
        } else if (propertyName.equals("lastCommandData")) {
            byte[] data = (byte[])event.getNewValue();
            this.didReceiveRawCommandData(data);
        } else if (propertyName.equals("deviceName")) {
            String name = (String)event.getNewValue();
            this.didReceiveModuleDeviceName(name);
            this.mName = name;
        } else if (propertyName.equals("btCommInterval")) {
            BluetoothRobotConstantsBase.kModuleParameterBTCommunicationIntervalValues newValues = (BluetoothRobotConstantsBase.kModuleParameterBTCommunicationIntervalValues) event.getNewValue();
            this.didReceiveModuleBtTransmissionInterval(newValues);
        } else if (propertyName.equals("moduleUartBaudRate")) {
            BluetoothRobotConstants.kModuleParameterUARTBaudRateValues newValues = (BluetoothRobotConstants.kModuleParameterUARTBaudRateValues) event.getNewValue();
            this.didReceiveModuleUARTBuadRate(newValues);
        } else if (propertyName.equals("btBroadcastPeriod")) {
            BluetoothRobotConstantsBase.kModuleParameter_BroadcastPeriodValues newValues = (BluetoothRobotConstantsBase.kModuleParameter_BroadcastPeriodValues) event.getNewValue();
            this.didReceiveModuleBtBroadcastPeriod(newValues);
        } else if (propertyName.equals("productId")) {
            short newProductId = (Short)event.getNewValue();
            this.didReceiveModuleProductId(newProductId);
        } else if (propertyName.equals("moduleTransmitPower")) {
            BluetoothRobotConstantsBase.kMipModuleParameter_TransmitPowerValues newValues = (BluetoothRobotConstantsBase.kMipModuleParameter_TransmitPowerValues) event.getNewValue();
            this.didReceiveModuleBtTransmitPower(newValues);
        } else if (propertyName.equals("customBroadcastData")) {
            HashMap customBroadcastData = (HashMap)event.getNewValue();
            this.didReceiveModuleBtCustomBroadcastData(customBroadcastData);
            this.customBroadcastData = customBroadcastData;
        } else if (propertyName.equals("systemId")) {
            String newSystemId;
            this.bleSystemId = newSystemId = (String)event.getNewValue();
            this.didReceiveDeviceSystemID(newSystemId);
        } else if (propertyName.equals("moduleSoftwareVersion")) {
            String newModuleVersion;
            this.bleModuleSoftwareVersion = newModuleVersion = (String)event.getNewValue();
            this.didReceiveDeviceSoftwareVersion(newModuleVersion);
        } else if (propertyName.equals("activationStatus")) {
            String value = (String)event.getNewValue();
            this.toyActivationStatus = Integer.valueOf(value).byteValue();
            this.didReceiveProductActivationStatus(this.toyActivationStatus);
        }
    }

    public void debugLog() {
    }

    public void getBluetoothBatteryLevel() {
        BRBatteryLevelService service = (BRBatteryLevelService)this.findService("0000180f-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.readBatteryLevel();
        } else {
            Log.d("BluetoothRobotPrivate", "BluetoothRobot: This device does not support Battery Level Service");
        }
    }

    public void getBluetoothRssiReading() {
        BRRSSIReportService service = (BRRSSIReportService)this.findService("0000ffa0-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.readRSSI();
        } else {
            Log.d("BluetoothRobotPrivate", "BluetoothRobot: This device does not support RSSI Report Service");
        }
    }

    @Override
    public void sendRobotCommand(RobotCommand robotCommand) {
        this.sendRobotCommand(robotCommand, null);
    }

    public void sendRobotCommand(RobotCommand robotCommand, BRBaseService.BRServiceAction callback) {
        if (callback != null) {
            robotCommand.completedCallback = callback;
        }
        this._processRobotCommand(robotCommand);
    }

    private void _processRobotCommand(RobotCommand robotCommand) {
        this.sendRawCommandData(robotCommand.data(), robotCommand.completedCallback);
    }

    @Override
    public void sendRawCommandData(byte[] data, BRBaseService.BRServiceAction callback) {
        BRSendDataService service = (BRSendDataService)this.findService("0000ffe5-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.sendData(data, callback);
        } else {
            Log.d("BluetoothRobotPrivate", "BluetoothRobot: This device does not support Send Data Service");
        }
    }

    public void getBluetoothDeviceName() {
        BRModuleParametersService service = (BRModuleParametersService)this.findService("0000ff90-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.readBTDeviceName();
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Module Parameters Service");
        }
    }

    public void setBluetoothDeviceName(String newName) {
        BRModuleParametersService service = (BRModuleParametersService)this.findService("0000ff90-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.setBTDeviceName(newName);
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Module Parameters Service");
        }
    }

    public void getBluetoothCommunicationInterval() {
        BRModuleParametersService service = (BRModuleParametersService)this.findService("0000ff90-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.readBTCommunicationInterval();
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Module Parameters Service");
        }
    }

    public void getBluetoothUARTBuardRate() {
        BRModuleParametersService service = (BRModuleParametersService)this.findService("0000ff90-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.readUartBaudRate();
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Module Parameters Service");
        }
    }

    public void setBluetoothUARTBuardRate(BluetoothRobotConstants.kModuleParameterUARTBaudRateValues value) {
        BRModuleParametersService service = (BRModuleParametersService)this.findService("0000ff90-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.setUartBuadRate(value);
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Module Parameters Service");
        }
    }

    public void restartBluetoothDevice() {
        BRModuleParametersService service = (BRModuleParametersService)this.findService("0000ff90-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.restartModule();
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Module Parameters Service");
        }
    }

    public void factoryResetBluetoothDevice(boolean userDataOnly) {
        BRModuleParametersService service = (BRModuleParametersService)this.findService("0000ff90-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            if (userDataOnly) {
                service.restoreUserDataToFactorySettings();
            } else {
                service.restoreFullyFactorySettings();
            }
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Module Parameters Service");
        }
    }

    public void setBroadcastPeriod(BluetoothRobotConstantsBase.kModuleParameter_BroadcastPeriodValues period) {
        BRModuleParametersService service = (BRModuleParametersService)this.findService("0000ff90-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.setBroadcastPeriod(period);
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Module Parameters Service");
        }
    }

    public void getBroadcastPeriod() {
        BRModuleParametersService service = (BRModuleParametersService)this.findService("0000ff90-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.readBroadcastPeriod();
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Module Parameters Service");
        }
    }

    public void setProductIdentifier(short productIdentifier) {
        BRModuleParametersService service = (BRModuleParametersService)this.findService("0000ff90-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.setProductIdentifier(productIdentifier);
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Module Parameters Service");
        }
    }

    public void getProductIdentifier() {
        BRModuleParametersService service = (BRModuleParametersService)this.findService("0000ff90-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.readProductIdentifier();
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Module Parameters Service");
        }
    }

    public void setTransmitPower(BluetoothRobotConstantsBase.kMipModuleParameter_TransmitPowerValues transmitPower) {
        BRModuleParametersService service = (BRModuleParametersService)this.findService("0000ff90-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.setTransmitPower(transmitPower);
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Module Parameters Service");
        }
    }

    public void getTransmitPower() {
        BRModuleParametersService service = (BRModuleParametersService)this.findService("0000ff90-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.readTransmitPower();
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Module Parameters Service");
        }
    }

    public void setBroadcastData(HashMap<Byte, Byte> broadcastData) {
        BRModuleParametersService service = (BRModuleParametersService)this.findService("0000ff90-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.setBroadcastData(broadcastData);
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Module Parameters Service");
        }
    }

    public void setBroadcastDataToDefault() {
        BRModuleParametersService service = (BRModuleParametersService)this.findService("0000ff90-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.setBroadcastDataToDefault();
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Module Parameters Service");
        }
    }

    public void getBroadcastData() {
        BRModuleParametersService service = (BRModuleParametersService)this.findService("0000ff90-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.readBroadcastData();
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Module Parameters Service");
        }
    }

    public void forceModuleSleep() {
        BRModuleParametersService service = (BRModuleParametersService)this.findService("0000ff90-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.forceModuleSleep();
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Module Parameters Service");
        }
    }

    public void saveCurrentIOOutputInputState() {
        BRModuleParametersService service = (BRModuleParametersService)this.findService("0000ff90-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.saveCurrentIOOutputInputState();
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Module Parameters Service");
        }
    }

    public void setStandbyPulsedSleepMode(boolean pulsedSleep) {
        BRModuleParametersService service = (BRModuleParametersService)this.findService("0000ff90-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.setStandbyPulsedSleepMode(pulsedSleep);
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Module Parameters Service");
        }
    }

    public void getStandbyPulsedSleepMode() {
        BRModuleParametersService service = (BRModuleParametersService)this.findService("0000ff90-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.readStandbyPulsedSleepMode();
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Module Parameters Service");
        }
    }

    public void saveBroadcastDataFlash() {
        BRModuleParametersService service = (BRModuleParametersService)this.findService("0000ff90-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.writeCurrentCustomBroadcastDataToFlash();
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Module Parameters Service");
        }
    }

    public void setReceiveDataNotificationsEnabled(boolean turnOn) {
        BRReceiveDataService receiveDataService = (BRReceiveDataService)this.findService("0000ffe0-0000-1000-8000-00805f9b34fb");
        if (receiveDataService != null) {
            if (turnOn) {
                Log.d("BluetoothRobotPrivate", "Turn receive data notify ON");
                receiveDataService.turnOn();
            } else {
                receiveDataService.turnOff();
            }
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Receive Data Service");
        }
    }

    public void didReceiveFirmwareSentdata(int sentdata) {
        Log.d("BluetoothRobotPrivate", "BluetoothRobotPrivate didReceiveFirmwareSentdata call interface call back");
    }

    public void didReceiveFirmwareToChip(int sentdata) {
        Log.d("BluetoothRobotPrivate", "BluetoothRobotPrivate didReceiveFirmwareToChip call interface call back");
    }

    public void getBluetoothModuleSystemID() {
        BRDeviceInformationService service = (BRDeviceInformationService)this.findService("0000180a-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.readSystemId();
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Device Information Service");
        }
    }

    public void getBluetoothModuleSoftwareVersion() {
        BRDeviceInformationService service = (BRDeviceInformationService)this.findService("0000180a-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.readModuleSoftwareVersion();
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Device Information Service");
        }
    }

    public void setFirmwareUpdateMode(boolean value) {
        BRReceiveDataService service = (BRReceiveDataService)this.findService("0000ffe0-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            if (this.kBluetoothRobotState == 2) {
                service.firmwareUpdateMode = value;
            }
        } else {
            Log.d("BluetoothRobotPrivate", "eive Data Service");
        }
    }

    public boolean firmwareUpdateMode() {
        BRReceiveDataService service = (BRReceiveDataService)this.findService("0000ffe0-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            if (this.kBluetoothRobotState == 2) {
                return service.firmwareUpdateMode;
            }
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Receive Data Service");
        }
        return false;
    }

    public void didReceiveBatteryUpdate(int batteryPercentage) {
        Log.w("BluetoothRobotPrivate", "should override didReceiveBatteryUpdate");
    }

    public void didReceiveBluetoothRSSIUpdate(int rssi) {
        Log.w("BluetoothRobotPrivate", "should override didReceiveBluetoothRSSIUpdate");
    }

    public void didReceiveRawCommandData(byte[] data) {
        Log.w("BluetoothRobotPrivate", "should override didReceiveRawCommandData");
    }

    @Override
    public void didReceiveRobotCommand(RobotCommand robotCommand) {
        Log.w("BluetoothRobotPrivate", "should override didReceiveRobotCommand");
    }

    public void didReceiveModuleDeviceName(String deviceName) {
        Log.w("BluetoothRobotPrivate", "should override didReceiveModuleDeviceName");
    }

    public void didReceiveModuleBtTransmissionInterval(BluetoothRobotConstantsBase.kModuleParameterBTCommunicationIntervalValues interval) {
        Log.w("BluetoothRobotPrivate", "should override didReceiveModuleBtTransmissionInterval");
    }

    public void didReceiveModuleUARTBuadRate(BluetoothRobotConstants.kModuleParameterUARTBaudRateValues rate) {
    }

    public void didReceiveModuleBtBroadcastPeriod(BluetoothRobotConstantsBase.kModuleParameter_BroadcastPeriodValues period) {
        Log.w("BluetoothRobotPrivate", "should override didReceiveModuleBtBroadcastPeriod");
    }

    public void didReceiveModuleProductId(short productId) {
        Log.w("BluetoothRobotPrivate", "should override didReceiveModuleProductId");
    }

    public void didReceiveModuleBtTransmitPower(BluetoothRobotConstantsBase.kMipModuleParameter_TransmitPowerValues transmitPower) {
        Log.w("BluetoothRobotPrivate", "should override didReceiveModuleBtTransmitPower");
    }

    public void didReceiveModuleBtCustomBroadcastData(HashMap<Byte, Byte> customBroadcastData) {
        Log.w("BluetoothRobotPrivate", "should override didReceiveModuleBtCustomBroadcastData");
    }

    public void didReceiveDeviceSystemID(String systemId) {
        Log.w("BluetoothRobotPrivate", "should override didReceiveDeviceSystemID");
    }

    public void didReceiveDeviceSoftwareVersion(String version) {
        Log.w("BluetoothRobotPrivate", "should override didReceiveDeviceSoftwareVersion");
    }

    public void didReceiveNuvotonChipStatus(BluetoothRobotConstants.nuvotonBootloaderMode NuvotonChipstatus) {
        Log.w("BluetoothRobotPrivate", "should override didReceiveNuvotonChipstatus " + NuvotonChipstatus + " - ");
    }

    public void didReceiveFirmwareCompleteStatus(BluetoothRobotConstants.nuvotonFirmwareCompleteStatus NuvotonFirmwareCompleteStatus) {
        Log.w("BluetoothRobotPrivate", "should override nuvotonFirmwareCompleteStatus " + NuvotonFirmwareCompleteStatus + " - ");
    }

    public void didReceiveFirmwareDataStatus(BluetoothRobotConstants.nuvotonFirmwareStatus FirmwareDataStatus) {
        Log.w("BluetoothRobotPrivate", "should override didReceiveFirmwareDataStatus " + FirmwareDataStatus + " - ");
    }

    public void didReceiveProductActivationStatus(byte status) {
    }

    private void writeProductActivation(int activationType) {
        Log.d("BluetoothRobotPrivate", "writeActivationStatus");
        BRSettingService service = (BRSettingService)this.findService("0000ff10-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.writeProductActivationStatus(activationType);
        } else {
            Log.d("BluetoothRobotPrivate", "BluetoothRobot: This device does not support writeActivationStatus");
        }
    }

    public void resetProductActivationStatus() {
        this.toyActivationStatus = kActivation_FactoryDefault;
        this.writeProductActivation(this.toyActivationStatus);
    }

    public void setProductActivated() {
        this.toyActivationStatus = kActivation_Activate;
        this.writeProductActivation(this.toyActivationStatus);
    }

    public void setProductActivationUploaded() {
        this.toyActivationStatus = kActivation_ActivationSentToFlurry;
        this.writeProductActivation(this.toyActivationStatus);
    }

    public void getProductActivationStatus() {
        Log.d("BluetoothRobotPrivate", "readActivationStatus");
        BRSettingService service = (BRSettingService)this.findService("0000ff10-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.readProductActivationStatus();
        } else {
            Log.d("BluetoothRobotPrivate", "BluetoothRobot: This device does not support readActivationStatus");
        }
    }

    public void setConnectedBroadcastData(byte[] data) {
        BRModuleParametersService service = (BRModuleParametersService)this.findService("0000ff90-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.setConnectedBroadcastData(data);
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Module Parameters Service");
        }
    }

    public void setConnectedBroadcastOn(boolean isOn) {
        BRModuleParametersService service = (BRModuleParametersService)this.findService("0000ff90-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.setConnectedBroadcastOn(isOn);
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support Module Parameters Service");
        }
    }

    public void nordicRebootToMode(byte mode) {
        BRDFUService service = (BRDFUService)this.findService("0000ff30-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.rebootToMode(mode);
        } else {
            Log.d("BluetoothRobotPrivate", "This device does not support DFU Service");
        }
    }

    public void checkDFU() {
        Log.d("BluetoothRobotPrivate", "checkDFU()");
        BRDFUService service = (BRDFUService)this.findService("0000ff30-0000-1000-8000-00805f9b34fb");
        Log.d("BluetoothRobotPrivate", "checkDFU()" + service);
        if (service != null) {
            this.isDFUSupported = true;
            Log.d("BluetoothRobotPrivate", "isDFUSupported = true");
        } else {
            this.isDFUSupported = false;
            Log.d("BluetoothRobotPrivate", "isDFUSupported = false");
        }
    }
}

