/*
 * Decompiled with CFR 0.152.
 */
package com.wowwee.bluetoothrobotcontrollib.rev;

import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotConstant;

public class REVTrackingStatus {
    private int degree;
    private int strength;
    private REVRobotConstant.revRobotTrackingSignalDirection signalDirection;

    public int getDegree() {
        return this.degree;
    }

    public void setDegree(int degree) {
        this.degree = degree;
    }

    public int getStrength() {
        return this.strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public REVRobotConstant.revRobotTrackingSignalDirection getSignalDirection() {
        return this.signalDirection;
    }

    public void setSignalDirection(REVRobotConstant.revRobotTrackingSignalDirection signalDirection) {
        this.signalDirection = signalDirection;
    }
}

