/*
 * Decompiled with CFR 0.152.
 */
package com.wowwee.bluetoothrobotcontrollib.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdRecord {
    public static final int TYPE_FLAGS = 1;
    public static final int TYPE_UUID16_INC = 2;
    public static final int TYPE_UUID16 = 3;
    public static final int TYPE_UUID32_INC = 4;
    public static final int TYPE_UUID32 = 5;
    public static final int TYPE_UUID128_INC = 6;
    public static final int TYPE_UUID128 = 7;
    public static final int TYPE_NAME_SHORT = 8;
    public static final int TYPE_NAME = 9;
    public static final int TYPE_TRANSMITPOWER = 10;
    public static final int TYPE_CONNINTERVAL = 18;
    public static final int TYPE_SERVICEDATA = 22;
    public static final int TYPE_MANUFACTURERDATA = -1;
    private final int mLength;
    private final int mType;
    private final byte[] mData;
    protected static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static List<AdRecord> parseScanRecord(byte[] scanRecord) {
        ArrayList<AdRecord> records = new ArrayList<AdRecord>();
        int index = 0;
        while (index < scanRecord.length) {
            byte type;
            byte length;
            if ((length = scanRecord[index++]) == 0 || (type = scanRecord[index]) == 0) break;
            byte[] data = Arrays.copyOfRange(scanRecord, index + 1, index + length);
            records.add(new AdRecord(length, type, data));
            index += length;
        }
        return records;
    }

    public static String getName(AdRecord nameRecord) {
        return new String(nameRecord.mData);
    }

    public static int getServiceDataUuid(AdRecord serviceData) {
        if (serviceData.mType != 22) {
            return -1;
        }
        byte[] raw = serviceData.mData;
        int uuid = (raw[1] & 0xFF) << 8;
        return uuid += raw[0] & 0xFF;
    }

    public static byte[] getServiceData(AdRecord serviceData) {
        if (serviceData.mType != 22) {
            return null;
        }
        byte[] raw = serviceData.mData;
        return Arrays.copyOfRange(raw, 2, raw.length);
    }

    public static byte[] getRawData(AdRecord record) {
        return record.mData;
    }

    public AdRecord(int length, int type, byte[] data) {
        this.mLength = length;
        this.mType = type;
        this.mData = data;
    }

    public int getLength() {
        return this.mLength;
    }

    public int getType() {
        return this.mType;
    }

    public byte[] getmData() {
        return this.mData;
    }

    public String toString() {
        switch (this.mType) {
            case 1: {
                byte _flag = this.mData[0];
                return "Flags: " + _flag;
            }
            case 8: 
            case 9: {
                return "Name: " + AdRecord.getName(this);
            }
            case 2: 
            case 3: {
                return "UUIDs: " + AdRecord.bytesToHex(this.mData);
            }
            case 10: {
                return "Transmit Power: " + this.mData[0];
            }
            case 18: {
                int asInt = this.mData[0] & 0xFF | (this.mData[1] & 0xFF) << 8 | (this.mData[2] & 0xFF) << 16 | (this.mData[3] & 0xFF) << 24;
                float connInterval = Float.intBitsToFloat(asInt);
                return "Connect Interval: " + connInterval;
            }
            case 22: {
                return "Service Data";
            }
            case -1: {
                int initialProductId = this.mData[0] << 8 | this.mData[1];
                byte initialBatteryLevel = this.mData[6];
                byte initialIOModes = this.mData[7];
                byte initialIOStates = this.mData[8];
                return "Manufacturer Data :" + initialProductId + " Battery Level: " + initialBatteryLevel + " initialIOModes: " + initialIOModes + " initialIOStates: " + initialIOStates;
            }
        }
        return "Unknown Structure " + this.mType + ": " + AdRecord.getName(this);
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int j = 0;
        while (j < bytes.length) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0xF];
            ++j;
        }
        return new String(hexChars);
    }
}

