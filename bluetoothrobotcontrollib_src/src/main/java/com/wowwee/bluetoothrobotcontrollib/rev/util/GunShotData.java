/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.annotations.SerializedName
 */
package com.wowwee.bluetoothrobotcontrollib.rev.util;

import com.google.gson.annotations.SerializedName;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotConstant;

public class GunShotData {
    @SerializedName(value="gunID")
    private int gunShotID;
    @SerializedName(value="name")
    private String gunShotName;
    @SerializedName(value="damageLevel")
    private float damageLevel;
    private REVRobotConstant.revRobotRXSensor sensor;
    private long timestamp;
    public boolean isProcessed = false;

    public int getgunShotID() {
        return this.gunShotID;
    }

    public void setGunShotID(int gunID) {
        this.gunShotID = gunID;
    }

    public String getGunShotName() {
        return this.gunShotName;
    }

    public void setGunShotName(String name) {
        this.gunShotName = name;
    }

    public float getDamageLevel() {
        return this.damageLevel;
    }

    public void setDamageLevel(float damageLevel) {
        this.damageLevel = damageLevel;
    }

    public REVRobotConstant.revRobotRXSensor getSensor() {
        return this.sensor;
    }

    public void setSensor(REVRobotConstant.revRobotRXSensor sensor) {
        this.sensor = sensor;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isProcessed() {
        return this.isProcessed;
    }

    public void setProcessed(boolean isProcessed) {
        this.isProcessed = isProcessed;
    }

    public GunShotData(GunShotData copy) {
        this.setGunShotID(copy.getgunShotID());
        this.setGunShotName(copy.getGunShotName());
        this.setDamageLevel(copy.getDamageLevel());
        this.setSensor(copy.getSensor());
        this.setTimestamp(copy.getTimestamp());
    }
}

