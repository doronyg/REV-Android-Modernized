/*
 * Decompiled with CFR 0.152.
 */
package com.wowwee.bluetoothrobotcontrollib.rev;

public class REVRobotConstant {
    public static final int REVRampPlayModeDefault = 0;
    public static final int REVRampPlayModeInvincibility = 1;
    public static final int REVRampPlayModeEMP = 2;
    public static final int REVRampPlayModeHealthBoost = 4;
    public static final int REVRampPlayModeSnowFlake = 8;

    public enum rampTxDirection {
        RampTXAll(1),
        RampTXAllWithoutFront(2),
        RampTXShooterIR(3);

        public byte value;

        rampTxDirection(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    public enum rev2TXDirection {
        REV2TXFRONTGUN(1),
        REV2TXGROUNDBEACON(2),
        REV2TXSKYFRONT(4),
        REV2TXSKYMIDDLE(8),
        REV2TXSKYBACK(16),
        REV2TXSKYSIDE(32);

        byte value;

        rev2TXDirection(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    public enum revRampShootID {
        REVRampShoot_Invincible(16),
        REVRampShoot_HealthPack(17),
        REVRampShoot_SnowFlake(18),
        REVRampShoot_EMP(10);

        public byte value;

        revRampShootID(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    public enum revRobotColor {
        REVRobotColorRed(1),
        REVRobotColorGreen(2),
        REVRobotColorYellow(3),
        REVRobotColorBlue(4),
        REVRobotColorMagenta(5),
        REVRobotColorCyan(6),
        REVRobotColorWhite(7),
        REVRobotColorBlack(8);

        byte value;

        revRobotColor(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    public enum revRobotRXSensor {
        REVRobotRXSensorFront(1),
        REVRobotRXSensorRear(2),
        REVRobotRXSensorLeft(3),
        REVRobotRXSensorRight(4);

        byte value;

        revRobotRXSensor(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }

        public static revRobotRXSensor getParamWithValue(byte value) {
            revRobotRXSensor[] revRobotRXSensorArray = revRobotRXSensor.values();
            int n = revRobotRXSensorArray.length;
            int n2 = 0;
            while (n2 < n) {
                revRobotRXSensor param = revRobotRXSensorArray[n2];
                if (param.value == value) {
                    return param;
                }
                ++n2;
            }
            return null;
        }
    }

    public enum revRobotTrackingDistance {
        REVTrackingDistanceFar(1),
        REVTrackingDistanceMedium(2),
        REVTrackingDistanceClose(3),
        REVTrackingDistanceVeryClose(4);

        byte value;

        revRobotTrackingDistance(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }

        public static revRobotTrackingDistance getParamWithValue(byte value) {
            revRobotTrackingDistance[] revRobotTrackingDistanceArray = revRobotTrackingDistance.values();
            int n = revRobotTrackingDistanceArray.length;
            int n2 = 0;
            while (n2 < n) {
                revRobotTrackingDistance param = revRobotTrackingDistanceArray[n2];
                if (param.value == value) {
                    return param;
                }
                ++n2;
            }
            return null;
        }
    }

    public enum revRobotTrackingMode {
        REVTrackingUserControl(1),
        REVTrackingChase(2),
        REVTrackingTurret(3),
        REVTrackingAvoid(4),
        REVTrackingBeacon(5),
        REVTrackingRamp(6),
        REVTrackingCircle(7);

        byte value;

        revRobotTrackingMode(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    public enum revRobotTrackingSignalDirection {
        REVRobotTrackingSignalRight(0),
        REVRobotTrackingSignalLeft(1);

        byte value;

        revRobotTrackingSignalDirection(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    public enum revRobotTrackingSpeed {
        REVRobotTrackingSpeed1(1),
        REVRobotTrackingSpeed2(2),
        REVRobotTrackingSpeed3(3),
        REVRobotTrackingSpeed4(4),
        REVRobotTrackingSpeed5(5),
        REVRobotTrackingSpeed6(6),
        REVRobotTrackingSpeed7(7),
        REVRobotTrackingSpeed8(8),
        REVRobotTrackingSpeed9(9),
        REVRobotTrackingSpeed10(10);

        byte value;

        revRobotTrackingSpeed(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }

        public static revRobotTrackingSpeed getParamWithValue(byte value) {
            revRobotTrackingSpeed[] revRobotTrackingSpeedArray = revRobotTrackingSpeed.values();
            int n = revRobotTrackingSpeedArray.length;
            int n2 = 0;
            while (n2 < n) {
                revRobotTrackingSpeed param = revRobotTrackingSpeedArray[n2];
                if (param.value == value) {
                    return param;
                }
                ++n2;
            }
            return null;
        }
    }

    public enum revTXDirection {
        REVTXFront(1),
        REVTXAll(2),
        REVTXPlane(3);

        byte value;

        revTXDirection(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    public enum revTrackingUpdateStatus {
        REVTrackingLostBeacon(0),
        REVTrackingFoundBeacon(1);

        byte value;

        revTrackingUpdateStatus(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    public enum revTraction {
        REVStandardTraction(1),
        REVHighTraction(2),
        REVLowTraction(3);

        byte value;

        revTraction(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }
    }
}

