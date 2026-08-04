/*
 * Decompiled with CFR 0.152.
 */
package com.wowwee.bluetoothrobotcontrollib;

public class BluetoothRobotConstantsBase {
    public static final String KMipPwmOutputServiceUUID = "0000ffb0-0000-1000-8000-00805f9b34fb";
    public static final String KMipProgrammableIOServiceUUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    public static final String kMipSendDataServiceString = "sendData";
    public static final String kMipSendDataServiceUUID = "0000ffe5-0000-1000-8000-00805f9b34fb";
    public static final String kMipSendBytesDataCharateristicString = "sendData";
    public static final String kMipSendBytesDataCharateristicUUID = "0000ffe9-0000-1000-8000-00805f9b34fb";
    public static final String kMipReceiveDataServiceString = "recieveData";
    public static final String kMipReceiveDataServiceUUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static final String kMipReceiveDataCharateristicString = "receiveData";
    public static final String kMipReceiveDataCharateristicUUID = "0000ffe4-0000-1000-8000-00805f9b34fb";
    public static final String kMipReceiveDataNotificationUUID = "00002902-0000-1000-8000-00805f9b34fb";

    public enum kMipModuleParameter_RemoteControlExtensionValues {
        kMipModuleParameter_RemoteControlExtension_SaveIOState(1),
        kMipModuleParameter_RemoteControlExtension_ForceSleepMode(2),
        kMipModuleParameter_RemoteControlExtension_DisconnectBluetoothClient(3),
        kMipModuleParameter_RemoteControlExtension_WriteCustomBroadcastDataToFlash(4);

        byte value;

        kMipModuleParameter_RemoteControlExtensionValues(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }

        public static kMipModuleParameter_RemoteControlExtensionValues getParamWithValue(byte value) {
            kMipModuleParameter_RemoteControlExtensionValues[] kMipModuleParameter_RemoteControlExtensionValuesArray = kMipModuleParameter_RemoteControlExtensionValues.values();
            int n = kMipModuleParameter_RemoteControlExtensionValuesArray.length;
            int n2 = 0;
            while (n2 < n) {
                kMipModuleParameter_RemoteControlExtensionValues param = kMipModuleParameter_RemoteControlExtensionValuesArray[n2];
                if (param.value == value) {
                    return param;
                }
                ++n2;
            }
            return null;
        }
    }

    public enum kMipModuleParameter_TransmitPowerValues {
        kMipModuleParameter_TransmitPower_Plus4dBm(0),
        kMipModuleParameter_TransmitPower_0dBm(1),
        kMipModuleParameter_TransmitPower_Minus6dBm(2),
        kMipModuleParameter_TransmitPower_Minus23dBm(3);

        byte value;

        kMipModuleParameter_TransmitPowerValues(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }

        public static kMipModuleParameter_TransmitPowerValues getParamWithValue(byte value) {
            kMipModuleParameter_TransmitPowerValues[] kMipModuleParameter_TransmitPowerValuesArray = kMipModuleParameter_TransmitPowerValues.values();
            int n = kMipModuleParameter_TransmitPowerValuesArray.length;
            int n2 = 0;
            while (n2 < n) {
                kMipModuleParameter_TransmitPowerValues param = kMipModuleParameter_TransmitPowerValuesArray[n2];
                if (param.value == value) {
                    return param;
                }
                ++n2;
            }
            return null;
        }
    }

    public enum kModuleParameterBTCommunicationIntervalValues {
        kModuleParameterBTCommunicationInterval20ms(0),
        kModuleParameterBTCommunicationInterval50ms(1),
        kModuleParameterBTCommunicationInterval100ms(2),
        kModuleParameterBTCommunicationInterval200ms(3),
        kModuleParameterBTCommunicationInterval300ms(4),
        kModuleParameterBTCommunicationInterval400ms(5),
        kModuleParameterBTCommunicationInterval500ms(6),
        kModuleParameterBTCommunicationInterval1000ms(7),
        kModuleParameterBTCommunicationInterval2000ms(8);

        byte value;

        kModuleParameterBTCommunicationIntervalValues(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }

        public static kModuleParameterBTCommunicationIntervalValues getParamWithValue(byte value) {
            kModuleParameterBTCommunicationIntervalValues[] kModuleParameterBTCommunicationIntervalValuesArray = kModuleParameterBTCommunicationIntervalValues.values();
            int n = kModuleParameterBTCommunicationIntervalValuesArray.length;
            int n2 = 0;
            while (n2 < n) {
                kModuleParameterBTCommunicationIntervalValues param = kModuleParameterBTCommunicationIntervalValuesArray[n2];
                if (param.value == value) {
                    return param;
                }
                ++n2;
            }
            return null;
        }
    }

    public enum kModuleParameterValues {
        kModuleParameter_ResetModuleRestoreFactorySettings(54),
        kModuleParameter_ResetModuleResetUserData(53),
        kModuleParameter_RestartModule(85);

        byte value;

        kModuleParameterValues(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    public enum kModuleParameter_BroadcastPeriodValues {
        kModuleParameter_BroadcastPeriod200MS(0),
        kModuleParameter_BroadcastPeriod500MS(1),
        kModuleParameter_BroadcastPeriod1000MS(2),
        kModuleParameter_BroadcastPeriod1500MS(3),
        kModuleParameter_BroadcastPeriod2000MS(4),
        kModuleParameter_BroadcastPeriod2500MS(5),
        kModuleParameter_BroadcastPeriod3000MS(6),
        kModuleParameter_BroadcastPeriod4000MS(7),
        kModuleParameter_BroadcastPeriod5000MS(8);

        byte value;

        kModuleParameter_BroadcastPeriodValues(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }

        public static kModuleParameter_BroadcastPeriodValues getParamWithValue(byte value) {
            kModuleParameter_BroadcastPeriodValues[] kModuleParameter_BroadcastPeriodValuesArray = kModuleParameter_BroadcastPeriodValues.values();
            int n = kModuleParameter_BroadcastPeriodValuesArray.length;
            int n2 = 0;
            while (n2 < n) {
                kModuleParameter_BroadcastPeriodValues param = kModuleParameter_BroadcastPeriodValuesArray[n2];
                if (param.value == value) {
                    return param;
                }
                ++n2;
            }
            return null;
        }
    }
}

