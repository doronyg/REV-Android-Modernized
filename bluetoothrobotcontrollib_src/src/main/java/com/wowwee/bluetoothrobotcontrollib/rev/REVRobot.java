/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.bluetooth.BluetoothDevice
 *  android.os.Handler
 *  android.util.Log
 */
package com.wowwee.bluetoothrobotcontrollib.rev;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.util.Log;
import com.wowwee.bluetoothrobotcontrollib.BluetoothLeService;
import com.wowwee.bluetoothrobotcontrollib.BluetoothRobotPrivate;
import com.wowwee.bluetoothrobotcontrollib.RobotCommand;
import com.wowwee.bluetoothrobotcontrollib.rev.REVCommandValues;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotConstant;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotFinder;
import com.wowwee.bluetoothrobotcontrollib.rev.REVTrackingStatus;
import com.wowwee.bluetoothrobotcontrollib.rev.util.GunShotData;
import com.wowwee.bluetoothrobotcontrollib.services.BRBaseService;
import com.wowwee.bluetoothrobotcontrollib.services.BRDFUService;
import com.wowwee.bluetoothrobotcontrollib.services.BRDeviceInformationService;
import com.wowwee.bluetoothrobotcontrollib.services.BRModuleParametersService;
import com.wowwee.bluetoothrobotcontrollib.services.BRReceiveDataService;
import com.wowwee.bluetoothrobotcontrollib.services.BRSendDataService;
import com.wowwee.bluetoothrobotcontrollib.services.BRSettingService;
import com.wowwee.bluetoothrobotcontrollib.util.AdRecord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class REVRobot
extends BluetoothRobotPrivate {
    protected REVRobotInterface callbackInterface = null;
    public boolean disableReceivedCommandProcessing = false;
    public int batteryLevel;
    public int batteryType;
    public int avatarIconBroadcastDriverID = 1;
    public boolean isFreezed = false;
    public int volume;
    public int voiceChipVersion;
    public int irChipVersion;
    public String softwareVersion;
    public int rssi;
    public REVRobotConstant.revRobotTrackingMode revTrackingMode;
    private int specialBroadcastID = 0;
    public List<GunShotData> processGunShotDataList = new ArrayList<GunShotData>();
    public boolean isDFUMode;
    public REVType revType;
    public boolean beaconFound;
    public float health = 1.0f;
    private boolean isDead;
    public float healthRatio = 1.0f;
    public boolean isShieldActivated = false;
    public boolean isInvincibleActivated = false;
    public float shieldValue;
    public boolean isAbsorberActivated;
    public float absorberValue;

    public REVRobot(BluetoothDevice pBluetoothDevice, List<AdRecord> pScanRecords, BluetoothLeService pBluetoothLeService) {
        super(pBluetoothDevice, pScanRecords, pBluetoothLeService);
    }

    public void setCallbackInterface(REVRobotInterface callbackInterface) {
        this.callbackInterface = callbackInterface;
    }

    public REVType getRevType() {
        return this.revType;
    }

    public void setRevType(REVType revType) {
        this.revType = revType;
    }

    public int getRssi() {
        return this.rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public void setIsDead(boolean isDead) {
        this.isDead = isDead;
    }

    public boolean isDead() {
        return this.isDead;
    }

    public String getSoftwareVersion() {
        return this.softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    public int getVoiceChipVersion() {
        return this.voiceChipVersion;
    }

    public void setVoiceChipVersion(int voiceChipVersion) {
        this.voiceChipVersion = voiceChipVersion;
    }

    public int getIrChipVersion() {
        return this.irChipVersion;
    }

    public void setIrChipVersion(int irChipVersion) {
        this.irChipVersion = irChipVersion;
    }

    public boolean isFreezed() {
        return this.isFreezed;
    }

    public void setFreezed(boolean isFreezed) {
        this.isFreezed = isFreezed;
    }

    public boolean isAbsorberActivated() {
        return this.isAbsorberActivated;
    }

    public void setAbsorberActivated(boolean isAbsorberActivated) {
        this.isAbsorberActivated = isAbsorberActivated;
    }

    public float getAbsorberValue() {
        return this.absorberValue;
    }

    public void setAbsorberValue(float absorberValue) {
        this.absorberValue = absorberValue;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void revGetShotWithGunShotData(GunShotData gunShotData) {
        REVRobot rEVRobot = this;
        synchronized (rEVRobot) {
            this.processGunShotDataList.add(0, gunShotData);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public void revClearGunShotDataList(boolean cleanUp) {
        REVRobot rEVRobot = this;
        synchronized (rEVRobot) {
            if (cleanUp) {
                this.processGunShotDataList.clear();
            } else {
                while (this.processGunShotDataList.size() > 10) {
                    this.processGunShotDataList.remove(this.processGunShotDataList.size() - 1);
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean revContainsUnprocessedGunShot() {
        REVRobot rEVRobot = this;
        synchronized (rEVRobot) {
            block4: {
                if (this.processGunShotDataList.size() <= 0 || this.processGunShotDataList.get(0).isProcessed) break block4;
                return true;
            }
        }
        return false;
    }

    public void setHealth(float health) {
        this.health = health;
        this.refreshBroadcastData();
    }

    public void activateShield(float value, final float duration) {
        this.isShieldActivated = true;
        this.shieldValue = value;
        new Thread(){

            @Override
            public void run() {
                try {
                    Thread.sleep((long)duration);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                REVRobot.this.deactivateShield();
            }
        }.start();
    }

    public void deactivateShield() {
        this.isShieldActivated = false;
        this.setSpecialBroadcastID(0);
    }

    public void setSpecialBroadcastID(int specialBroadcastID) {
        if (this.specialBroadcastID != specialBroadcastID) {
            this.specialBroadcastID = specialBroadcastID;
            this.refreshBroadcastData();
            if (this.callbackInterface != null) {
                this.callbackInterface.revSpecialBroadcastIDChanged(this);
            }
        }
    }

    public int getSpecialBroadcastID() {
        return this.specialBroadcastID;
    }

    public void setAvatarIconBroadcastDriverID(int avatarIconBroadcastDriverID) {
        if (this.avatarIconBroadcastDriverID != avatarIconBroadcastDriverID) {
            this.avatarIconBroadcastDriverID = avatarIconBroadcastDriverID;
            this.refreshBroadcastData();
            if (this.callbackInterface != null) {
                this.callbackInterface.revAvatarIconBroadcastDriverIDChanged(this);
            }
        }
    }

    public int getAvatarIconBroadcastDriverID() {
        return this.avatarIconBroadcastDriverID;
    }

    public void refreshBroadcastData() {
        if (this.kBluetoothRobotState == 2) {
            int healthVale = (int)(this.health * 100.0f);
            byte healthByte = (byte)healthVale;
            byte specialIDByte = (byte)this.specialBroadcastID;
            byte avatarIconBroadcastDriverIDByte = (byte)this.avatarIconBroadcastDriverID;
            byte[] data = new byte[]{REVCommandValues.kRevConnectedBroadcast, healthByte, specialIDByte, avatarIconBroadcastDriverIDByte};
            if (data != null) {
                this.setConnectedBroadcastData(data);
            }
        }
    }

    public void revHealthReset() {
        this.health = 1.0f;
        this.isDead = false;
        this.refreshBroadcastData();
    }

    @Override
    public void peripheralDidConnect() {
        super.peripheralDidConnect();
        REVRobotFinder.getInstance().revDidConnect(this);
        try {
            Thread.sleep(500L);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void peripheralDidDisconnect() {
        if (this.kBluetoothRobotState == 2) {
            super.peripheralDidDisconnect();
        } else {
            super.peripheralDidDisconnect();
            REVRobotFinder.getInstance().revDidDisconnect(this);
            if (this.callbackInterface != null) {
                this.callbackInterface.revDeviceDisconnected(this);
            }
        }
    }

    @Override
    public void peripheralDidBecomeReady() {
        super.peripheralDidBecomeReady();
        if (this.callbackInterface != null) {
            this.callbackInterface.revDeviceReady(this);
        }
        Thread thread = new Thread(new Runnable(){

            @Override
            public void run() {
                try {
                    Thread.sleep(500L);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                REVRobot.this.revGetVolume();
                REVRobot.this.revGetSoftwareVersion();
                REVRobot.this.revGetHardwareInfo();
                REVRobot.this.revGetBatteryLevel();
                REVRobot.this.getBluetoothModuleSoftwareVersion();
                REVRobot.this.getBluetoothModuleSystemID();
                REVRobot.this.getProductActivationStatus();
            }
        });
        thread.start();
    }

    public void revDrive(float[] vector) {
        this.revDrive(vector, 1.0f, 1.0f);
    }

    public void revDrive(float[] vector, float driveSpeedRatio, float turnSpeedRatio) {
        if (this.isFreezed) {
            return;
        }
        int xValue = Math.max(Math.min(Math.round(vector[0] * 31.0f), 31), -31);
        int yValue = Math.max(Math.min(Math.round(vector[1] * 31.0f), 31), -31);
        boolean driveForward = yValue > 0;
        boolean turnRight = xValue > 0;
        xValue = Math.round((float)Math.abs(xValue) * turnSpeedRatio);
        yValue = Math.round((float)Math.abs(yValue) * turnSpeedRatio);
        byte driveValue = 0;
        if (yValue != 0) {
            driveValue = (byte)(driveForward ? REVCommandValues.kRevDriveContinuousValue.kRevDriveCont_FW_Speed1.getValue() + yValue : REVCommandValues.kRevDriveContinuousValue.kRevDriveCont_BW_Speed1.getValue() + yValue);
        }
        byte turnValue = 0;
        if (xValue != 0) {
            turnValue = (byte)(turnRight ? REVCommandValues.kRevDriveContinuousValue.kRevDriveCont_Right_Speed1.getValue() + xValue : REVCommandValues.kRevDriveContinuousValue.kRevDriveCont_Left_Speed1.getValue() + xValue);
        }
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevDrive_Continuous, driveValue, turnValue));
    }

    public void revSetCurrentTraction(byte traction) {
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevCurrentTraction, traction));
    }

    public void revGetCurrentTraction() {
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevCurrentTraction, (byte)-1));
    }

    public void revSetTrackingMode(REVRobotConstant.revRobotTrackingMode mode) {
        this.revTrackingMode = mode;
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevSetTrackingMode, mode.getValue()));
    }

    public void revGetTrackingMode() {
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevSetTrackingMode, (byte)-1));
    }

    public void revSetBumpNotifyOnOff(boolean isOn) {
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevSetBumpNotifyOnOff, (byte)(isOn ? 1 : 0)));
    }

    public void revGetBumpNotify() {
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevBumpNotify));
    }

    public void revSetTrackingSensorStatus(boolean isOn) {
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevSetTrackingSensorOnOff, (byte)(isOn ? 1 : 0)));
    }

    public void revGetTrackingSensorStatus() {
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevTrackingSensorStatus));
    }

    public void revGetTrackingStatusUpdate() {
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevTrackingStatusUpdate));
    }

    public void revSetCurrentTrackingSetting(REVRobotConstant.revRobotTrackingDistance trackingDistance, REVRobotConstant.revRobotTrackingSpeed trackingSpeed) {
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevTrackingSettings, trackingDistance.getValue(), trackingSpeed.getValue()));
    }

    public void revGetCurrentTrackingSetting() {
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevTrackingSettings, (byte)-1));
    }

    public void revSendIRCommand(byte irCommand, byte sound, REVRobotConstant.revTXDirection direction) {
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevSendIRCommand, irCommand, sound, direction.getValue()));
    }

    public void rampSendIRCommand(byte irCommand, REVRobotConstant.rampTxDirection direction) {
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevSendIRCommand, irCommand, direction.getValue()));
    }

    public void revGetBatteryLevel() {
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevBatteryLevel));
    }

    public void revGetRGBLed() {
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevGetLEDColor));
    }

    public void revSetRGBLed(REVRobotConstant.revRobotColor color) {
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevSetLEDColor, color.getValue()));
    }

    public void revFlashRGBLed(REVRobotConstant.revRobotColor color, int onTime, int offTime, int flashTime) {
        byte timeOn = (byte)(onTime / 20);
        byte timeOff = (byte)(offTime / 20);
        byte repeatCount = (byte)flashTime;
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevFlashLEDColor, color.getValue(), timeOn, timeOff, repeatCount));
    }

    public void revPulsateRGBLed(REVRobotConstant.revRobotColor color, byte onTime, byte offTime) {
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevPulsateLEDColor, color.getValue(), onTime, offTime));
    }

    public void revSetUserData(byte data1, byte data2) {
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevSetUserStatus));
    }

    public void revGetUserData(byte data1) {
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevGetUserStatus));
    }

    public void revGetSoftwareVersion() {
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevGetSoftwareVersion));
    }

    public void revSetVolume(byte volume) {
        this.volume = volume;
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevSetVolume, volume));
    }

    public void revGetVolume() {
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevGetVolume));
    }

    public void revGetHardwareInfo() {
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevGetHardwareVersion));
    }

    public void revPlaySound(byte soundIndex) {
        byte repeatTime = 0;
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevPlaySound, soundIndex, repeatTime));
    }

    public void revStop() {
        this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevStop));
    }

    public void revTurnLeftByTime(float time, float turnSpeedRatio) {
        if (!this.isFreezed) {
            byte turnSpeed = (byte)(turnSpeedRatio * 10.0f);
            byte timeValue = (byte)(time * 1000.0f / 10.0f);
            this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevTurnLeftByTime, timeValue, turnSpeed));
        }
    }

    public void revTurnRightByTime(float time, float turnSpeedRatio) {
        if (!this.isFreezed) {
            byte turnSpeed = (byte)(turnSpeedRatio * 10.0f);
            byte timeValue = (byte)(time * 1000.0f / 10.0f);
            this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevTurnRightByTime, timeValue, turnSpeed));
        }
    }

    public void revDriveForwardWithTime(float time) {
        if (!this.isFreezed) {
            byte turnSpeed = 10;
            byte timeValue = (byte)(time * 1000.0f / 10.0f);
            this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevDriveForwardByTime, turnSpeed, timeValue, (byte)0));
        }
    }

    public void revDriveBackwardWithTime(float time) {
        if (!this.isFreezed) {
            byte turnSpeed = 10;
            byte timeValue = (byte)(time * 1000.0f / 10.0f);
            this.sendRobotCommand(RobotCommand.create(REVCommandValues.kRevDriveForwardByTime, turnSpeed, timeValue, (byte)1));
        }
    }

    public void activateAbsorberWithValue(float value, float duration) {
        this.isAbsorberActivated = true;
        this.absorberValue = value;
        Handler handler = new Handler();
        handler.postDelayed(new Runnable(){

            @Override
            public void run() {
                REVRobot.this.deactivateAbsorber();
            }
        }, (long)duration);
    }

    public void deactivateAbsorber() {
        this.isAbsorberActivated = false;
        this.specialBroadcastID = 0;
    }

    public void handleReceivedRevCommand(byte commandValue, ArrayList<Byte> dataArray) {
        if (this.callbackInterface != null && this.disableReceivedCommandProcessing) {
            return;
        }
        if (commandValue == REVCommandValues.kRevCurrentTraction) {
            if (dataArray.size() == 1 && this.callbackInterface != null) {
                this.callbackInterface.revDidReceiveCurrentTraction(this, dataArray.get(0));
            }
        } else if (commandValue == REVCommandValues.kRevSetTrackingMode) {
            if (dataArray.size() == 1 && this.callbackInterface != null) {
                this.callbackInterface.revDidReceiveTrackingMode(this, dataArray.get(0));
            }
        } else if (commandValue == REVCommandValues.kRevBumpNotify) {
            if (this.callbackInterface != null) {
                this.callbackInterface.revDidReceiveBumpNotify(this);
            }
        } else if (commandValue == REVCommandValues.kRevTrackingSensorStatus) {
            if (dataArray.size() == 3) {
                Log.d("AI", "kRevTrackingSensorStatus beaconFound = true, rev = " + this);
                this.beaconFound = true;
                REVTrackingStatus status = new REVTrackingStatus();
                status.setSignalDirection(REVRobotConstant.revRobotTrackingSignalDirection.values()[dataArray.get(0)]);
                status.setDegree(dataArray.get(1).byteValue());
                status.setStrength(dataArray.get(2).byteValue());
                if (this.callbackInterface != null) {
                    this.callbackInterface.revDidReceiveTrackingStatus(status);
                }
            }
        } else if (commandValue == REVCommandValues.kRevTrackingStatusUpdate) {
            if (dataArray.size() == 1 && this.callbackInterface != null) {
                if (dataArray.get(0) == 0) {
                    this.beaconFound = false;
                    Log.d("AI", "kRevTrackingStatusUpdate beaconFound = false, " + this);
                } else {
                    this.beaconFound = true;
                    Log.d("AI", "kRevTrackingStatusUpdate beaconFound = true, " + this);
                }
                this.callbackInterface.revDidReceiveTrackingUpdateStatus(this, dataArray.get(0));
            }
        } else if (commandValue == REVCommandValues.kRevTrackingSettings) {
            if (dataArray.size() == 2) {
                byte distance = dataArray.get(0);
                byte speed = dataArray.get(1);
                if (this.callbackInterface != null) {
                    this.callbackInterface.revDidReceiveTrackingDistanceAndSpeed(this, distance, speed);
                }
            }
        } else if (commandValue == REVCommandValues.kRevBatteryLevel) {
            if (dataArray.size() == 2) {
                byte currentBatteryLevel = dataArray.get(0);
                float level = (float)(currentBatteryLevel - 77) / 47.0f;
                this.batteryLevel = (int)(level * 100.0f);
                this.batteryType = dataArray.get(1).byteValue();
                if (this.callbackInterface != null) {
                    this.callbackInterface.revDidReceiveBatteryInfo(this, this.batteryLevel, this.batteryType);
                }
            }
        } else if (commandValue == REVCommandValues.kRevGetLEDColor) {
            if (dataArray.size() == 3 && this.callbackInterface != null) {
                this.callbackInterface.revDidReceiveCurrentLEDColor(this, dataArray.get(0));
            }
        } else if (commandValue == REVCommandValues.kRevGetUserStatus) {
            if (dataArray.size() == 2 && this.callbackInterface != null) {
                this.callbackInterface.revDidReceiveUserStatus(this, dataArray.get(0), dataArray.get(1));
            }
        } else if (commandValue == REVCommandValues.kRevGetSoftwareVersion) {
            if (dataArray.size() == 5) {
                byte year = dataArray.get(0);
                byte month = dataArray.get(1);
                byte day = dataArray.get(2);
                byte version = dataArray.get(3);
                byte bootloaderVersion = dataArray.get(4);
                String monthString = Byte.toString(month);
                String dayString = Byte.toString(day);
                String versionString = Byte.toString(version);
                String date = String.valueOf(year) + (monthString.length() == 1 ? "0" + monthString : monthString) + (dayString.length() == 1 ? "0" + dayString : dayString) + (versionString.length() == 1 ? "0" + versionString : versionString);
                String bootloaderString = Byte.toString(bootloaderVersion);
                this.softwareVersion = date + "." + (bootloaderString.length() == 1 ? "0" + bootloaderString : bootloaderString);
                if (this.callbackInterface != null) {
                    this.callbackInterface.revDidReceiveSoftwareVersion(this, date, Byte.toString(bootloaderVersion));
                }
            }
        } else if (commandValue == REVCommandValues.kRevGetVolume) {
            if (dataArray.size() == 1 && this.callbackInterface != null) {
                this.volume = dataArray.get(0).byteValue();
                this.callbackInterface.revDidReceiveVolumeLevel(this, dataArray.get(0).byteValue());
            }
        } else if (commandValue == REVCommandValues.kRevGetHardwareVersion) {
            if (dataArray.size() == 2) {
                this.voiceChipVersion = dataArray.get(0).byteValue();
                this.irChipVersion = dataArray.get(1).byteValue();
                if (this.callbackInterface != null) {
                    this.callbackInterface.revDidReceiveHardwareVersion(this, this.voiceChipVersion, this.irChipVersion);
                }
            }
        } else if (commandValue == REVCommandValues.kRevSendIRCommand) {
            if (dataArray.size() == 2 && this.callbackInterface != null) {
                this.callbackInterface.revDidReceiveIRCommand(this, dataArray.get(0), dataArray.get(1));
            }
        } else if (commandValue == REVCommandValues.kRevRampUpdateNotify) {
            if (this.callbackInterface != null) {
                this.callbackInterface.revRobotDidJumpedOverRamp(this);
            }
        } else if (this.callbackInterface != null) {
            this.callbackInterface.revDidReceiveRawData(this, dataArray);
        }
    }

    @Override
    public void didReceiveBatteryUpdate(int batteryPercentage) {
        Log.d("REVRobot", "Received battery level: " + batteryPercentage);
    }

    @Override
    public void didReceiveBluetoothRSSIUpdate(int rssi) {
        Log.d("REVRobot", "Received RSSI: " + rssi);
    }

    @Override
    public void didReceiveProductActivationStatus(byte status) {
        if (status == BluetoothRobotPrivate.kActivation_FactoryDefault) {
            this.callbackInterface.revDidReceiveToyActivationStatus(this, false, false);
        } else if (status == BluetoothRobotPrivate.kActivation_Activate) {
            this.callbackInterface.revDidReceiveToyActivationStatus(this, true, false);
        } else if (status == BluetoothRobotPrivate.kActivation_ActivationSentToFlurry) {
            this.callbackInterface.revDidReceiveToyActivationStatus(this, true, true);
        }
    }

    @Override
    public void didReceiveRawCommandData(byte[] data) {
        Log.d("REVRobot", "Received didReceiveRawCommandData");
        if (data != null) {
            byte commandValue = data[0];
            ArrayList<Byte> dataArray = new ArrayList<Byte>();
            int i = 1;
            while (i < data.length) {
                dataArray.add(data[i]);
                ++i;
            }
            this.handleReceivedRevCommand(commandValue, dataArray);
        }
    }

    @Override
    public void didReceiveRobotCommand(RobotCommand robotCommand) {
        Log.d("REVRobot", "Received didReceiveRobotCommand");
        if (this.callbackInterface != null && this.callbackInterface.revBluetoothDidProcessedReceiveRobotCommand(this, robotCommand) && this.disableReceivedCommandProcessing) {
            return;
        }
        if (robotCommand == null) {
            Log.d("MipRobot", "handleReceivedRevCommand - data is nil");
            return;
        }
        byte commandValue = robotCommand.getCmdByte();
        ArrayList<Byte> dataArray = robotCommand.getDataArray();
        this.handleReceivedRevCommand(commandValue, dataArray);
    }

    @Override
    public HashMap<String, BRBaseService> buildPeripheralServiceDict(BluetoothLeService pBluetoothLeService) {
        HashMap<String, BRBaseService> serviceDict = new HashMap<String, BRBaseService>();
        serviceDict.put("0000ffe5-0000-1000-8000-00805f9b34fb", new BRSendDataService(pBluetoothLeService, this.mBluetoothDevice.getAddress()));
        serviceDict.put("0000ffe0-0000-1000-8000-00805f9b34fb", new BRReceiveDataService(pBluetoothLeService, this, this.mBluetoothDevice.getAddress()));
        serviceDict.put("0000ff90-0000-1000-8000-00805f9b34fb", new BRModuleParametersService(pBluetoothLeService, this, this.mBluetoothDevice.getAddress()));
        serviceDict.put("0000180a-0000-1000-8000-00805f9b34fb", new BRDeviceInformationService(pBluetoothLeService, this, this.mBluetoothDevice.getAddress()));
        serviceDict.put("0000ff10-0000-1000-8000-00805f9b34fb", new BRSettingService(pBluetoothLeService, this, this.mBluetoothDevice.getAddress()));
        serviceDict.put("0000ff30-0000-1000-8000-00805f9b34fb", new BRDFUService(pBluetoothLeService, this, this.mBluetoothDevice.getAddress()));
        return serviceDict;
    }

    @Override
    public void sendRawCommandData(byte[] data, BRBaseService.BRServiceAction callback) {
        byte[] d1 = data;
        BRBaseService.BRServiceAction ca = callback;
        BRSendDataService service = (BRSendDataService)this.findService("0000ffe5-0000-1000-8000-00805f9b34fb");
        if (service != null) {
            service.sendData(d1, ca);
        } else {
            Log.d("BluetoothRobot", "BluetoothRobot: This device does not support Send Data Service");
        }
    }

    public interface REVRobotInterface {
        void revDeviceReady(REVRobot var1);

        void revDeviceDisconnected(REVRobot var1);

        void revDidReceiveBatteryInfo(REVRobot var1, int var2, int var3);

        void revDidReceiveHardwareVersion(REVRobot var1, int var2, int var3);

        void revDidReceiveToyActivationStatus(REVRobot var1, boolean var2, boolean var3);

        void revDidReceiveVolumeLevel(REVRobot var1, int var2);

        void revDidReceiveIRCommand(REVRobot var1, byte var2, byte var3);

        void revDidReceiveTrackingMode(REVRobot var1, byte var2);

        void revDidReceiveTrackingStatus(REVTrackingStatus var1);

        void revDidReceiveTrackingUpdateStatus(REVRobot var1, byte var2);

        void revDidReceiveTrackingDistanceAndSpeed(REVRobot var1, byte var2, byte var3);

        void revDidReceiveCurrentLEDColor(REVRobot var1, byte var2);

        void revDidReceiveSoftwareVersion(REVRobot var1, String var2, String var3);

        void revDidReceiveCurrentTraction(REVRobot var1, byte var2);

        void revDidReceiveUserStatus(REVRobot var1, byte var2, byte var3);

        void revDidReceiveBumpNotify(REVRobot var1);

        void revDidReceiveRawData(REVRobot var1, ArrayList<Byte> var2);

        void revSpecialBroadcastIDChanged(REVRobot var1);

        void revAvatarIconBroadcastDriverIDChanged(REVRobot var1);

        boolean revBluetoothDidProcessedReceiveRobotCommand(REVRobot var1, RobotCommand var2);

        void revRobotDidJumpedOverRamp(REVRobot var1);
    }

    public enum REVType {
        REV,
        RAMP

    }
}

