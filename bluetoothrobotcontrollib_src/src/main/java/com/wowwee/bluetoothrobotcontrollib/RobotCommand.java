/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.util.Log
 */
package com.wowwee.bluetoothrobotcontrollib;

import android.util.Log;
import com.wowwee.bluetoothrobotcontrollib.services.BRBaseService;
import java.util.ArrayList;

public class RobotCommand {
    private byte cmdByte;
    private ArrayList<Byte> dataArray = new ArrayList();
    public BRBaseService.BRServiceAction completedCallback = null;
    public int length;
    protected static final char[] hexArray = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static RobotCommand create(String asciiHex) {
        if (asciiHex == null) {
            return null;
        }
        return new RobotCommand(asciiHex);
    }

    public static RobotCommand create(byte command) {
        return new RobotCommand(command);
    }

    public static RobotCommand create(byte command, ArrayList<Byte> dataArray) {
        return new RobotCommand(command, dataArray);
    }

    public static RobotCommand create(byte command, byte byte1) {
        ArrayList<Byte> dataArray = new ArrayList<Byte>();
        dataArray.add(byte1);
        return new RobotCommand(command, dataArray);
    }

    public static RobotCommand create(byte command, byte byte1, byte byte2) {
        ArrayList<Byte> dataArray = new ArrayList<Byte>();
        dataArray.add(byte1);
        dataArray.add(byte2);
        return new RobotCommand(command, dataArray);
    }

    public static RobotCommand create(byte command, byte byte1, byte byte2, byte byte3) {
        ArrayList<Byte> dataArray = new ArrayList<Byte>();
        dataArray.add(byte1);
        dataArray.add(byte2);
        dataArray.add(byte3);
        return new RobotCommand(command, dataArray);
    }

    public static RobotCommand create(byte command, byte byte1, byte byte2, byte byte3, byte byte4) {
        ArrayList<Byte> dataArray = new ArrayList<Byte>();
        dataArray.add(byte1);
        dataArray.add(byte2);
        dataArray.add(byte3);
        dataArray.add(byte4);
        return new RobotCommand(command, dataArray);
    }

    public static RobotCommand create(byte command, byte byte1, byte byte2, byte byte3, byte byte4, byte byte5) {
        ArrayList<Byte> dataArray = new ArrayList<Byte>();
        dataArray.add(byte1);
        dataArray.add(byte2);
        dataArray.add(byte3);
        dataArray.add(byte4);
        dataArray.add(byte5);
        return new RobotCommand(command, dataArray);
    }

    public static RobotCommand create(byte command, byte byte1, byte byte2, byte byte3, byte byte4, byte byte5, byte byte6) {
        ArrayList<Byte> dataArray = new ArrayList<Byte>();
        dataArray.add(byte1);
        dataArray.add(byte2);
        dataArray.add(byte3);
        dataArray.add(byte4);
        dataArray.add(byte5);
        dataArray.add(byte6);
        return new RobotCommand(command, dataArray);
    }

    public static RobotCommand create(byte command, byte byte1, byte byte2, byte byte3, byte byte4, byte byte5, byte byte6, byte byte7) {
        ArrayList<Byte> dataArray = new ArrayList<Byte>();
        dataArray.add(byte1);
        dataArray.add(byte2);
        dataArray.add(byte3);
        dataArray.add(byte4);
        dataArray.add(byte5);
        dataArray.add(byte6);
        dataArray.add(byte7);
        return new RobotCommand(command, dataArray);
    }

    public static RobotCommand create(byte command, byte byte1, byte byte2, byte byte3, byte byte4, byte byte5, byte byte6, byte byte7, byte byte8) {
        ArrayList<Byte> dataArray = new ArrayList<Byte>();
        dataArray.add(byte1);
        dataArray.add(byte2);
        dataArray.add(byte3);
        dataArray.add(byte4);
        dataArray.add(byte5);
        dataArray.add(byte6);
        dataArray.add(byte7);
        dataArray.add(byte8);
        return new RobotCommand(command, dataArray);
    }

    public static RobotCommand create(byte[] byte1) {
        ArrayList<Byte> dataArray = new ArrayList<Byte>();
        byte[] byArray = byte1;
        int n = byte1.length;
        int n2 = 0;
        while (n2 < n) {
            byte b = byArray[n2];
            dataArray.add(b);
            ++n2;
        }
        return new RobotCommand(dataArray);
    }

    public static RobotCommand create(byte command, byte[] byte1) {
        ArrayList<Byte> dataArray = new ArrayList<Byte>();
        byte[] byArray = byte1;
        int n = byte1.length;
        int n2 = 0;
        while (n2 < n) {
            byte b = byArray[n2];
            dataArray.add(b);
            ++n2;
        }
        return new RobotCommand(command, dataArray);
    }

    public static RobotCommand create(byte command, byte byte1, byte[] byte2) {
        ArrayList<Byte> dataArray = new ArrayList<Byte>();
        dataArray.add(byte1);
        byte[] byArray = byte2;
        int n = byte2.length;
        int n2 = 0;
        while (n2 < n) {
            byte b = byArray[n2];
            dataArray.add(b);
            ++n2;
        }
        return new RobotCommand(command, dataArray);
    }

    public static RobotCommand create(byte command, byte byte1, byte byte2, byte[] byte3) {
        ArrayList<Byte> dataArray = new ArrayList<Byte>();
        dataArray.add(byte1);
        dataArray.add(byte2);
        byte[] byArray = byte3;
        int n = byte3.length;
        int n2 = 0;
        while (n2 < n) {
            byte b = byArray[n2];
            dataArray.add(b);
            ++n2;
        }
        return new RobotCommand(command, dataArray);
    }

    public static RobotCommand create(byte command, byte byte1, byte byte2, byte byte3, byte[] byte4) {
        ArrayList<Byte> dataArray = new ArrayList<Byte>();
        dataArray.add(byte1);
        dataArray.add(byte2);
        dataArray.add(byte3);
        byte[] byArray = byte4;
        int n = byte4.length;
        int n2 = 0;
        while (n2 < n) {
            byte b = byArray[n2];
            dataArray.add(b);
            ++n2;
        }
        return new RobotCommand(command, dataArray);
    }

    public static RobotCommand create(byte command, byte byte1, byte byte2, byte byte3, byte byte4, byte[] byte5) {
        ArrayList<Byte> dataArray = new ArrayList<Byte>();
        dataArray.add(byte1);
        dataArray.add(byte2);
        dataArray.add(byte3);
        dataArray.add(byte4);
        byte[] byArray = byte5;
        int n = byte5.length;
        int n2 = 0;
        while (n2 < n) {
            byte b = byArray[n2];
            dataArray.add(b);
            ++n2;
        }
        return new RobotCommand(command, dataArray);
    }

    public RobotCommand(String asciiHex) {
        boolean uneven;
        if (asciiHex == null || asciiHex.length() <= 1) {
            Log.w("RobotCommand", "Received invalid command " + asciiHex + " invalid length! Must be at least 2 characters");
        }
        int asciiLength = asciiHex.length();
        ArrayList<Byte> theDataArrayList = new ArrayList<Byte>();
        boolean bl = uneven = asciiLength % 2 == 1;
        if (uneven) {
            --asciiLength;
            Log.w("RobotCommand", "Received invalid command " + asciiHex + " invalid length! Chopping off last character");
        }
        int i = 0;
        while (i < asciiLength) {
            String cmd = asciiHex.substring(i, i + 2);
            Integer cmdInteger = Integer.parseInt(cmd, 16);
            theDataArrayList.add(cmdInteger.byteValue());
            i += 2;
        }
        this.cmdByte = theDataArrayList.get(0);
        if (this.cmdByte == 0) {
            Log.w("RobotCommand", "Received invalid command, it cannot be zero");
        }
        this.length = theDataArrayList.size();
        theDataArrayList.remove(0);
        this.dataArray = theDataArrayList;
    }

    public RobotCommand(byte command) {
        this(command, null);
    }

    public RobotCommand(byte command, ArrayList<Byte> dataArray) {
        this.cmdByte = command;
        this.dataArray = dataArray;
        this.length = dataArray != null ? dataArray.size() + 1 : 1;
    }

    public RobotCommand(ArrayList<Byte> byteArray) {
        if (byteArray.size() > 0) {
            this.cmdByte = byteArray.get(0);
            this.dataArray = new ArrayList();
            int i = 1;
            while (i < byteArray.size()) {
                this.dataArray.add(byteArray.get(i));
                ++i;
            }
            this.length = this.dataArray.size() + 1;
        } else {
            Log.e("RobotCommand", "Byte array cannot be empty");
        }
    }

    public byte[] data() {
        byte[] commandData = this.dataArray != null ? new byte[this.dataArray.size() + 1] : new byte[]{this.cmdByte};
        if (this.dataArray != null) {
            commandData[0] = this.cmdByte;
            int i = 0;
            while (i < this.dataArray.size()) {
                commandData[i + 1] = this.dataArray.get(i);
                ++i;
            }
        }
        return commandData;
    }

    public byte getCmdByte() {
        return this.cmdByte;
    }

    public ArrayList<Byte> getDataArray() {
        return this.dataArray;
    }

    public String description() {
        String desc = "RobotCommand: " + RobotCommand.byteArrayToHexString(new byte[]{this.cmdByte});
        if (this.dataArray != null) {
            for (Byte b : this.dataArray) {
                desc = desc + " " + RobotCommand.byteArrayToHexString(new byte[]{b});
            }
        }
        if (this.completedCallback != null) {
            desc = desc + " with callback";
        }
        return desc;
    }

    public static String byteArrayToHexString(byte[] bytes) {
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

