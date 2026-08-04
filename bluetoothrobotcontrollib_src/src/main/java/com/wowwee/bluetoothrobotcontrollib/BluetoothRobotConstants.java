/*
 * Decompiled with CFR 0.152.
 */
package com.wowwee.bluetoothrobotcontrollib;

import com.wowwee.bluetoothrobotcontrollib.BluetoothRobotConstantsBase;

public class BluetoothRobotConstants
extends BluetoothRobotConstantsBase {
    public static final String kMipBatteryLevelServiceString = "batteryLevel";
    public static final String kMipBatteryLevelServiceUUID = "0000180f-0000-1000-8000-00805f9b34fb";
    public static final String kMipBatteryLevelReportCharacteristicString = "batteryLevelReport";
    public static final String kMipBatteryLevelReportCharacteristicUUID = "00002a19-0000-1000-8000-00805f9b34fb";
    public static final String kMipRSSIReportServiceString = "rssiReport";
    public static final String kMipRSSIReportServiceUUID = "0000ffa0-0000-1000-8000-00805f9b34fb";
    public static final String kMipRSSIReportReadChracteristicString = "rssiLevel";
    public static final String kMipRSSIReportReadChracteristicUUID = "0000ffa1-0000-1000-8000-00805f9b34fb";
    public static final String kMipRSSIReportSetIntervalChracteristicString = "setRssiNotification";
    public static final String kMipRSSIReportSetIntervalChracteristicUUID = "0000ffa2-0000-1000-8000-00805f9b34fb";
    public static final String kMipModuleParametersServiceString = "moduleParams";
    public static final String kMipModuleParametersServiceUUID = "0000ff90-0000-1000-8000-00805f9b34fb";
    public static final String kMipModuleParameterDeviceNameCharacteristicString = "deviceName";
    public static final String kMipModuleParameterDeviceNameCharacteristicUUID = "0000ff91-0000-1000-8000-00805f9b34fb";
    public static final String kMipModuleParameterBTCommunicationIntervalCharacteristicString = "btCommInterval";
    public static final String kMipModuleParameterBTCommunicationIntervalCharacteristicUUID = "0000ff92-0000-1000-8000-00805f9b34fb";
    public static final String kMipModuleParameterUARTBaudRateCharacteristicString = "uartBaudRate";
    public static final String kMipModuleParameterUARTBaudRateCharacteristicUUID = "0000ff93-0000-1000-8000-00805f9b34fb";
    public static final String kMipModuleParameterResetModuleCharacteristicString = "resetModule";
    public static final String kMipModuleParameterResetModuleCharacteristicUUID = "0000ff94-0000-1000-8000-00805f9b34fb";
    public static final String kMipModuleParameterBroadcastPeriodCharacteristicString = "setOrReadBroadcastPeriod";
    public static final String kMipModuleParameterBroadcastPeriodCharacteristicUUID = "0000ff95-0000-1000-8000-00805f9b34fb";
    public static final String kMipModuleParameterProductIDCharacteristicString = "setOrReadProductID";
    public static final String kMipModuleParameterProductIDCharacteristicUUID = "0000ff96-0000-1000-8000-00805f9b34fb";
    public static final String kMipModuleParameterTransmitPowerCharacteristicString = "setOrReadTransmitPower";
    public static final String kMipModuleParameterTransmitPowerCharacteristicUUID = "0000ff97-0000-1000-8000-00805f9b34fb";
    public static final String kMipModuleParameterCustomBroadcastDataCharacteristicString = "setOrReadCustomBroadcastData";
    public static final String kMipModuleParameterCustomBroadcastDataCharacteristicUUID = "0000ff98-0000-1000-8000-00805f9b34fb";
    public static final String kMipModuleParameterRemoteControlExtensionCharacteristicString = "setRemoteControlExtension";
    public static final String kMipModuleParameterRemoteControlExtensionCharacteristicUUID = "0000ff99-0000-1000-8000-00805f9b34fb";
    public static final String kMipModuleParameterStandbyModeCharacteristicString = "readOrWriteStandbyMode";
    public static final String kMipModuleParameterStandbyModeCharacteristicUUID = "0000ff9A-0000-1000-8000-00805f9b34fb";
    public static final String kMipModuleParameterSetConnectedBroadcastDataCharacteristicString = "setConnectedBroadcastData";
    public static final String kMipModuleParameterSetConnectedBroadcastDataCharacteristicUUID = "0000ff9B-0000-1000-8000-00805f9b34fb";
    public static final String kMipModuleParameterSetConnectedBroadcastOnOffCharacteristicString = "setConnectedBroadcastOnOff";
    public static final String kMipModuleParameterSetConnectedBroadcastOnOffCharacteristicUUID = "0000ff9C-0000-1000-8000-00805f9b34fb";
    public static final String kDeviceNuvotonBootloaderServiceString = "nuvotonBootloader";
    public static final String kDeviceNuvotonBootloaderServiceUUID = "0000ff00-0000-1000-8000-00805f9b34fb";
    public static final String kDeviceNuvotonBootloader_GetChipStatusCharacteristicString = "getChipStatus";
    public static final String kDeviceNuvotonBootloader_GetChipStatusCharacteristicUUID = "0000ff01-0000-1000-8000-00805f9b34fb";
    public static final String kDeviceNuvotonBootloader_RestartChipCharacteristicString = "restartChip";
    public static final String kDeviceNuvotonBootloader_RestartChipCharacteristicUUID = "0000ff02-0000-1000-8000-00805f9b34fb";
    public static final String kDeviceNuvotonBootloader_TransferFirmwareHeaderCharacteristicString = "headerData";
    public static final String kDeviceNuvotonBootloader_TransferFirmwareHeaderCharacteristicUUID = "0000ff03-0000-1000-8000-00805f9b34fb";
    public static final String kDeviceNuvotonBootloader_StopTransferCharacteristicString = "stoptransfer";
    public static final String kDeviceNuvotonBootloader_StopTransferCharacteristicUUID = "0000ff04-0000-1000-8000-00805f9b34fb";
    public static final String kDeviceNuvotonBootloader_TransferFirmwareStatusCharacteristicString = "transferFirmwareStatus";
    public static final String kDeviceNuvotonBootloader_TransferFirmwareStatusCharacteristicUUID = "0000ff05-0000-1000-8000-00805f9b34fb";
    public static final String kDeviceNuvotonBootloader_ReadFirmwareDataStatusCharacteristicString = "readFirmwareDataStatus";
    public static final String kDeviceNuvotonBootloader_ReadFirmwareDataStatusCharacteristicUUID = "0000ff06-0000-1000-8000-00805f9b34fb";
    public static final String kDeviceNuvotonBootloader_WriteFirmwareToNuvotonCharacteristicString = "writeFirmwareToNuvoton";
    public static final String kDeviceNuvotonBootloader_WriteFirmwareToNuvotonCharacteristicUUID = "0000ff07-0000-1000-8000-00805f9b34fb";
    public static final String kDeviceNuvotonBootloader_FirmwareWriteCompleteCharacteristicString = "firmwareWriteComplete";
    public static final String kDeviceNuvotonBootloader_FirmwareWriteCompleteCharacteristicUUID = "0000ff08-0000-1000-8000-00805f9b34fb";
    public static final String kDeviceNuvotonBootloader_TransferFirmwareDataCharacteristicString = "transferData";
    public static final String kDeviceNuvotonBootloader_TransferFirmwareDataCharacteristicUUID = "0000ff09-0000-1000-8000-00805f9b34fb";
    public static final String kDeviceNuvotonBootloader_WriteFirmwareToNuvotonProgressCharacteristicString = "firmwareToNuvotonProgress";
    public static final String kDeviceNuvotonBootloader_WriteFirmwareToNuvotonProgressCharacteristicUUID = "0000ff0a-0000-1000-8000-00805f9b34fb";
    public static final String kMipDeviceInformationServiceString = "deviceInfo";
    public static final String kMipDeviceInformationServiceUUID = "0000180a-0000-1000-8000-00805f9b34fb";
    public static final String kMipDeviceInformationSystemIDCharacteristicString = "systemId";
    public static final String kMipDeviceInformationSystemIDCharacteristicUUID = "00002a23-0000-1000-8000-00805f9b34fb";
    public static final String kMipDeviceInformationModuleSoftwareVerCharacteristicString = "softwareVersion";
    public static final String kMipDeviceInformationModuleSoftwareVerCharacteristicUUID = "00002a26-0000-1000-8000-00805f9b34fb";
    public static final String kSettingServiceString = "deviceSetting";
    public static final String kSettingServiceUUID = "0000ff10-0000-1000-8000-00805f9b34fb";
    public static final String kSettingProductActivationCharacteristicString = "activation";
    public static final String kSettingProductActivationCharacteristicUUID = "0000ff1b-0000-1000-8000-00805f9b34fb";
    public static final String kDeviceDFUServiceString = "dfu";
    public static final String kDeviceDFUServiceUUID = "0000ff30-0000-1000-8000-00805f9b34fb";
    public static final String kDeviceDFUCharacteristicString = "dfuchar";
    public static final String kDeviceDFUCharacteristicUUID = "0000ff31-0000-1000-8000-00805f9b34fb";
    public static final String kLedServiceString = "ledService";
    public static final String kLedServiceUUID = "0000fe00-0000-1000-8000-00805f9b34fb";
    public static final String kRGBLedCharacteristicString = "rgbled";
    public static final String kRGBLedCharacteristicUUID = "0000fe01-0000-1000-8000-00805f9b34fb";
    public static final String kTracterbeamledCharacteristicString = "tracterbeamled";
    public static final String kTracterbeamledCharacteristicUUID = "0000fe03-0000-1000-8000-00805f9b34fb";

    public enum kDFURebootMode {
        kDeviceApplicationMode(1),
        kDeviceDFUMode(2);

        byte value;

        kDFURebootMode(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }

        public static kDFURebootMode getParamWithValue(byte value) {
            kDFURebootMode[] kDFURebootModeArray = kDFURebootMode.values();
            int n = kDFURebootModeArray.length;
            int n2 = 0;
            while (n2 < n) {
                kDFURebootMode param = kDFURebootModeArray[n2];
                if (param.value == value) {
                    return param;
                }
                ++n2;
            }
            return null;
        }
    }

    public enum kModuleParameterUARTBaudRateValues {
        kModuleParameterUARTBaudRate4800(0),
        kModuleParameterUARTBaudRate9600(1),
        kModuleParameterUARTBaudRate19200(2),
        kModuleParameterUARTBaudRate38400(3),
        kModuleParameterUARTBaudRate57600(4),
        kModuleParameterUARTBaudRate115200(5);

        byte value;

        kModuleParameterUARTBaudRateValues(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }

        public static kModuleParameterUARTBaudRateValues getParamWithValue(byte value) {
            kModuleParameterUARTBaudRateValues[] kModuleParameterUARTBaudRateValuesArray = kModuleParameterUARTBaudRateValues.values();
            int n = kModuleParameterUARTBaudRateValuesArray.length;
            int n2 = 0;
            while (n2 < n) {
                kModuleParameterUARTBaudRateValues param = kModuleParameterUARTBaudRateValuesArray[n2];
                if (param.value == value) {
                    return param;
                }
                ++n2;
            }
            return null;
        }
    }

    public enum nuvotonBootloaderMode {
        kNuvotonAppMode(0),
        kNuvotonBootloaderMode(1);

        byte value;

        nuvotonBootloaderMode(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }

        public static nuvotonBootloaderMode getParamWithValue(byte value) {
            nuvotonBootloaderMode[] nuvotonBootloaderModeArray = nuvotonBootloaderMode.values();
            int n = nuvotonBootloaderModeArray.length;
            int n2 = 0;
            while (n2 < n) {
                nuvotonBootloaderMode param = nuvotonBootloaderModeArray[n2];
                if (param.value == value) {
                    return param;
                }
                ++n2;
            }
            return null;
        }
    }

    public enum nuvotonFirmwareCompleteStatus {
        kNuvotonFirmwareCompleteStatus_ready(-1),
        kNuvotonFirmwareCompleteStatus_Success(0),
        kNuvotonFirmwareCompleteStatus_BadFirmwareData(1),
        kNuvotonFirmwareCompleteStatus_UpdateFailure(2);

        byte value;

        nuvotonFirmwareCompleteStatus(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }

        public static nuvotonFirmwareCompleteStatus getParamWithValue(byte value) {
            nuvotonFirmwareCompleteStatus[] nuvotonFirmwareCompleteStatusArray = nuvotonFirmwareCompleteStatus.values();
            int n = nuvotonFirmwareCompleteStatusArray.length;
            int n2 = 0;
            while (n2 < n) {
                nuvotonFirmwareCompleteStatus param = nuvotonFirmwareCompleteStatusArray[n2];
                if (param.value == value) {
                    return param;
                }
                ++n2;
            }
            return null;
        }
    }

    public enum nuvotonFirmwareStatus {
        kNuvotonFirmwareStatus_ready(-1),
        kNuvotonFirmwareStatus_DataOK(0),
        kNuvotonFirmwareStatus_BadChecksum(1),
        kNuvotonFirmwareStatus_BadData(2),
        kNuvotonFirmwareStatus_DataEmpty(3),
        kNuvotonFirmwareStatus_NextPacket(4),
        kNuvotonFirmwareStatus_HeaderOK(5);

        byte value;

        nuvotonFirmwareStatus(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }

        public static nuvotonFirmwareStatus getParamWithValue(byte value) {
            nuvotonFirmwareStatus[] nuvotonFirmwareStatusArray = nuvotonFirmwareStatus.values();
            int n = nuvotonFirmwareStatusArray.length;
            int n2 = 0;
            while (n2 < n) {
                nuvotonFirmwareStatus param = nuvotonFirmwareStatusArray[n2];
                if (param.value == value) {
                    return param;
                }
                ++n2;
            }
            return null;
        }
    }
}

