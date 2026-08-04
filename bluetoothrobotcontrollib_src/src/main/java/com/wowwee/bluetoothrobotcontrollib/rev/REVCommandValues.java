/*
 * Decompiled with CFR 0.152.
 */
package com.wowwee.bluetoothrobotcontrollib.rev;

public class REVCommandValues {
    public static final int REV_BLUETOOTH_PRODUCT_ID = 15;
    public static final int REV_RAMP_BLUETOOTH_PRODUCT_ID = 16;
    public static final int REV_BLUETOOTH_PRODUCT_DFU_ID = 17;
    public static final int REV_RAMP_BLUETOOTH_PRODUCT_DFU_ID = 18;
    public static byte kRevActivation_FactoryDefault = 0;
    public static byte kRevActivation_Activate = 1;
    public static byte kRevActivation_ActivationSentToFlurry = (byte)2;
    public static byte kRevActivation_HackerUartUsed = (byte)4;
    public static byte kRevActivation_HackerUartUsedSentToFlurry = (byte)8;
    public static byte kRevDrive_Continuous = (byte)120;
    public static byte kRevBatteryLevel = (byte)121;
    public static byte kRevPlaySound = (byte)6;
    public static byte kRevSetTrackingMode = (byte)-111;
    public static byte kRevSetTrackingSensorOnOff = (byte)-110;
    public static byte kRevTrackingSensorStatus = (byte)-109;
    public static byte kRevTrackingSettings = (byte)-108;
    public static byte kRevSendIRCommand = (byte)-107;
    public static byte kRevGetLEDColor = (byte)-125;
    public static byte kRevSetLEDColor = (byte)-124;
    public static byte kRevFlashLEDColor = (byte)-119;
    public static byte kRevPulsateLEDColor = (byte)-112;
    public static byte kRevGetHardwareVersion = (byte)25;
    public static byte kRevSetBumpNotifyOnOff = (byte)-105;
    public static byte kRevBumpNotify = (byte)-104;
    public static byte kRevGetSoftwareVersion = (byte)20;
    public static byte kRevTurnLeftByTime = (byte)115;
    public static byte kRevTurnRightByTime = (byte)116;
    public static byte kRevDriveForwardByTime = (byte)113;
    public static byte kRevSetTraction = (byte)-106;
    public static byte kRevStop = (byte)119;
    public static byte kRevSetSoundVolume = (byte)21;
    public static byte kRevGetSoundVolume = (byte)22;
    public static byte kRevGetMotorVoltage = (byte)-93;
    public static byte kRevConnectedBroadcast = (byte)40;
    public static byte kRevCurrentTraction = (byte)-106;
    public static byte kRevTrackingStatusUpdate = (byte)-103;
    public static byte kRevRampUpdateNotify = (byte)-94;
    public static byte kRevSetVolume = (byte)21;
    public static byte kRevGetVolume = (byte)22;
    public static byte kRevSetUserStatus = (byte)18;
    public static byte kRevGetUserStatus = (byte)19;
    public static byte kRevSoundFile_A3400_34K_ONEKHZ_500MS_8K16BIT_A34 = 1;
    public static byte kRevSoundFile_A3400_34K_SHORT_MUTE_FOR_STOP_A34 = (byte)2;
    public static byte kRevSoundFile_REV_ATTACK_GUN_1_FIRE_A34 = (byte)3;
    public static byte kRevSoundFile_REV_ATTACK_GUN_2_FIRE_A34 = (byte)4;
    public static byte kRevSoundFile_REV_ATTACK_GUN_3_FIRE_A34 = (byte)5;
    public static byte kRevSoundFile_REV_ATTACK_GUN_4_FIRE_A34 = (byte)6;
    public static byte kRevSoundFile_REV_ATTACK_GUN_5_FIRE_A34 = (byte)7;
    public static byte kRevSoundFile_REV_ATTACK_GUN_6_FIRE_A34 = (byte)8;
    public static byte kRevSoundFile_REV_ATTACK_SPECIAL_AIRSTRIKE_CHARGE_A34 = (byte)9;
    public static byte kRevSoundFile_REV_ATTACK_SPECIAL_AIRSTRIKE_FIRE_A34 = (byte)10;
    public static byte kRevSoundFile_REV_ATTACK_SPECIAL_BURSTSHOT_CHARGE_A34 = (byte)11;
    public static byte kRevSoundFile_REV_ATTACK_SPECIAL_BURSTSHOT_FIRE_A34 = (byte)12;
    public static byte kRevSoundFile_REV_ATTACK_SPECIAL_CANNON_CHARGE_A34 = (byte)13;
    public static byte kRevSoundFile_REV_ATTACK_SPECIAL_CANNON_FIRE_A34 = (byte)14;
    public static byte kRevSoundFile_REV_ATTACK_SPECIAL_EMP_CHARGE_A34 = (byte)15;
    public static byte kRevSoundFile_REV_ATTACK_SPECIAL_EMP_FIRE_A34 = (byte)16;
    public static byte kRevSoundFile_REV_ATTACK_SPECIAL_GRENADELAUNCHER_CHARGE_A34 = (byte)17;
    public static byte kRevSoundFile_REV_ATTACK_SPECIAL_GRENADELAUNCHER_FIRE_A34 = (byte)18;
    public static byte kRevSoundFile_REV_ATTACK_SPECIAL_MISSILE_CHARGE_A34 = (byte)19;
    public static byte kRevSoundFile_REV_ATTACK_SPECIAL_MISSILE_FIRE_A34 = (byte)20;
    public static byte kRevSoundFile_REV_ATTACK_SPECIAL_SHOCKWAVE_CHARGE_A34 = (byte)21;
    public static byte kRevSoundFile_REV_ATTACK_SPECIAL_SHOCKWAVE_FIRE_A34 = (byte)22;
    public static byte kRevSoundFile_REV_ATTACK_SPECIAL_TRACTORBEAM_CHARGE_A34 = (byte)23;
    public static byte kRevSoundFile_REV_ATTACK_SPECIAL_TRACTORBEAM_FIRE_A34 = (byte)24;
    public static byte kRevSoundFile_REV_BOOSTER_BERSERKER_ON_A34 = (byte)25;
    public static byte kRevSoundFile_REV_BOOSTER_BERSERKER_LOOP_A34 = (byte)26;
    public static byte kRevSoundFile_REV_BOOSTER_HEALTHRECHARGE_A34 = (byte)27;
    public static byte kRevSoundFile_REV_BOOSTER_INVERTDRIVE_A34 = (byte)28;
    public static byte kRevSoundFile_REV_BOOSTER_SHIELD_ACTIVATE_A34 = (byte)29;
    public static byte kRevSoundFile_REV_BOOSTER_SHIELD_DEACTIVATE_A34 = (byte)30;
    public static byte kRevSoundFile_REV_BOOSTER_SHIELD_ON_A34 = (byte)31;
    public static byte kRevSoundFile_REV_BOOSTER_SLUG_A34 = (byte)32;
    public static byte kRevSoundFile_REV_BOOSTER_TURBO_A34 = (byte)33;
    public static byte kRevSoundFile_REV_CAR_ALERTED_A34 = (byte)34;
    public static byte kRevSoundFile_REV_CAR_ALERTED_RAMP_A34 = (byte)35;
    public static byte kRevSoundFile_REV_CAR_CONFIRM_1_A34 = (byte)36;
    public static byte kRevSoundFile_REV_CAR_CONFIRM_2_A34 = (byte)37;
    public static byte kRevSoundFile_REV_CAR_CONFIRM_3_A34 = (byte)38;
    public static byte kRevSoundFile_REV_CAR_CONNECT_A34 = (byte)39;
    public static byte kRevSoundFile_REV_CAR_DISCONNECT_A34 = (byte)40;
    public static byte kRevSoundFile_REV_CAR_HORN_1_A34 = (byte)41;
    public static byte kRevSoundFile_REV_CAR_LOWBATTERY_A34 = (byte)42;
    public static byte kRevSoundFile_REV_CAR_PARKED_A34 = (byte)43;
    public static byte kRevSoundFile_REV_CAR_REVIVE_A34 = (byte)44;
    public static byte kRevSoundFile_REV_CAR_SIREN_1_A34 = (byte)45;
    public static byte kRevSoundFile_REV_CAR_TYRESCREECH_1_A34 = (byte)46;
    public static byte kRevSoundFile_REV_CAR_TYRESCREECH_2_A34 = (byte)47;
    public static byte kRevSoundFile_REV_CAR_TYRESCREECH_3_A34 = (byte)48;
    public static byte kRevSoundFile_REV_CAR_WARNING_A34 = (byte)49;
    public static byte kRevSoundFile_REV_DAMAGE_EXPLOSION_1_A34 = (byte)50;
    public static byte kRevSoundFile_REV_DAMAGE_EXPLOSION_2_A34 = (byte)51;
    public static byte kRevSoundFile_REV_DAMAGE_EXPLOSION_3_A34 = (byte)52;
    public static byte kRevSoundFile_REV_DAMAGE_EXPLOSION_4_A34 = (byte)53;
    public static byte kRevSoundFile_REV_DAMAGE_GLASS_1_A34 = (byte)54;
    public static byte kRevSoundFile_REV_DAMAGE_GLASS_2_A34 = (byte)55;
    public static byte kRevSoundFile_REV_DAMAGE_MALFUNCTION_1_A34 = (byte)56;
    public static byte kRevSoundFile_REV_DAMAGE_MALFUNCTION_2_A34 = (byte)57;
    public static byte kRevSoundFile_REV_DAMAGE_METAL_1_A34 = (byte)58;
    public static byte kRevSoundFile_REV_DAMAGE_METAL_2_A34 = (byte)59;
    public static byte kRevSoundFile_REV_DAMAGE_RICOCHET_1_A34 = (byte)60;
    public static byte kRevSoundFile_REV_DAMAGE_RICOCHET_2_A34 = (byte)61;
    public static byte kRevSoundFile_REV_DAMAGE_SHIELDSHATTER_A34 = (byte)62;
    public static byte kRevSoundFile_REV_DAMAGE_SPECIAL_AIRSTRIKE_IMPACT_1_A34 = (byte)63;
    public static byte kRevSoundFile_REV_DAMAGE_SPECIAL_AIRSTRIKE_IMPACT_2_A34 = (byte)64;
    public static byte kRevSoundFile_REV_DAMAGE_SPECIAL_CANNON_IMPACT_A34 = (byte)65;
    public static byte kRevSoundFile_REV_DAMAGE_SPECIAL_EMP_IMPACT_A34 = (byte)66;
    public static byte kRevSoundFile_REV_DAMAGE_SPECIAL_GRENADELAUNCHER_IMPACT_A34 = (byte)67;
    public static byte kRevSoundFile_REV_DAMAGE_SPECIAL_MISSILE_IMPACT_A34 = (byte)68;
    public static byte kRevSoundFile_REV_DAMAGE_SPECIAL_SHOCKWAVE_IMPACT_A34 = (byte)69;
    public static byte kRevSoundFile_REV_DAMAGE_SPECIAL_TRACTORBEAM_IMPACT_A34 = (byte)70;
    public static byte kRevSoundFile_REV_DAMAGE_DEATH_1_A34 = (byte)71;
    public static byte kRevSoundFile_REV_DAMAGE_DEATH_2_A34 = (byte)72;
    public static byte kRevSoundFile_REV_DAMAGE_DEATH_3_A34 = (byte)73;

    public enum kRevDriveContinuousValue {
        kRevDriveCont_FW_Speed1(0),
        kRevDriveCont_FW_Speed2(1),
        kRevDriveCont_FW_Speed3(2),
        kRevDriveCont_FW_Speed4(3),
        kRevDriveCont_FW_Speed5(4),
        kRevDriveCont_FW_Speed6(5),
        kRevDriveCont_FW_Speed7(6),
        kRevDriveCont_FW_Speed8(7),
        kRevDriveCont_FW_Speed9(8),
        kRevDriveCont_FW_Speed10(9),
        kRevDriveCont_FW_Speed11(16),
        kRevDriveCont_FW_Speed12(17),
        kRevDriveCont_FW_Speed13(18),
        kRevDriveCont_FW_Speed14(19),
        kRevDriveCont_FW_Speed15(20),
        kRevDriveCont_FW_Speed16(21),
        kRevDriveCont_FW_Speed17(22),
        kRevDriveCont_FW_Speed18(23),
        kRevDriveCont_FW_Speed19(24),
        kRevDriveCont_FW_Speed20(25),
        kRevDriveCont_FW_Speed21(32),
        kRevDriveCont_FW_Speed22(33),
        kRevDriveCont_FW_Speed23(34),
        kRevDriveCont_FW_Speed24(35),
        kRevDriveCont_FW_Speed25(36),
        kRevDriveCont_FW_Speed26(37),
        kRevDriveCont_FW_Speed27(38),
        kRevDriveCont_FW_Speed28(39),
        kRevDriveCont_FW_Speed29(40),
        kRevDriveCont_FW_Speed30(41),
        kRevDriveCont_FW_Speed31(48),
        kRevDriveCont_FW_Speed32(49),
        kRevDriveCont_BW_Speed1(32),
        kRevDriveCont_BW_Speed2(33),
        kRevDriveCont_BW_Speed3(34),
        kRevDriveCont_BW_Speed4(35),
        kRevDriveCont_BW_Speed5(36),
        kRevDriveCont_BW_Speed6(37),
        kRevDriveCont_BW_Speed7(38),
        kRevDriveCont_BW_Speed8(39),
        kRevDriveCont_BW_Speed9(40),
        kRevDriveCont_BW_Speed10(41),
        kRevDriveCont_BW_Speed11(48),
        kRevDriveCont_BW_Speed12(49),
        kRevDriveCont_BW_Speed13(50),
        kRevDriveCont_BW_Speed14(51),
        kRevDriveCont_BW_Speed15(52),
        kRevDriveCont_BW_Speed16(53),
        kRevDriveCont_BW_Speed17(54),
        kRevDriveCont_BW_Speed18(55),
        kRevDriveCont_BW_Speed19(56),
        kRevDriveCont_BW_Speed20(57),
        kRevDriveCont_BW_Speed21(64),
        kRevDriveCont_BW_Speed22(65),
        kRevDriveCont_BW_Speed23(66),
        kRevDriveCont_BW_Speed24(67),
        kRevDriveCont_BW_Speed25(68),
        kRevDriveCont_BW_Speed26(69),
        kRevDriveCont_BW_Speed27(70),
        kRevDriveCont_BW_Speed28(71),
        kRevDriveCont_BW_Speed29(72),
        kRevDriveCont_BW_Speed30(73),
        kRevDriveCont_BW_Speed31(80),
        kRevDriveCont_BW_Speed32(81),
        kRevDriveCont_Left_Speed1(96),
        kRevDriveCont_Left_Speed2(97),
        kRevDriveCont_Left_Speed3(98),
        kRevDriveCont_Left_Speed4(99),
        kRevDriveCont_Left_Speed5(100),
        kRevDriveCont_Left_Speed6(101),
        kRevDriveCont_Left_Speed7(102),
        kRevDriveCont_Left_Speed8(103),
        kRevDriveCont_Left_Speed9(104),
        kRevDriveCont_Left_Speed10(105),
        kRevDriveCont_Left_Speed11(112),
        kRevDriveCont_Left_Speed12(113),
        kRevDriveCont_Left_Speed13(114),
        kRevDriveCont_Left_Speed14(115),
        kRevDriveCont_Left_Speed15(116),
        kRevDriveCont_Left_Speed16(117),
        kRevDriveCont_Left_Speed17(118),
        kRevDriveCont_Left_Speed18(119),
        kRevDriveCont_Left_Speed19(120),
        kRevDriveCont_Left_Speed20(121),
        kRevDriveCont_Left_Speed21(-128),
        kRevDriveCont_Left_Speed22(-127),
        kRevDriveCont_Left_Speed23(-126),
        kRevDriveCont_Left_Speed24(-125),
        kRevDriveCont_Left_Speed25(-124),
        kRevDriveCont_Left_Speed26(-123),
        kRevDriveCont_Left_Speed27(-122),
        kRevDriveCont_Left_Speed28(-121),
        kRevDriveCont_Left_Speed29(-120),
        kRevDriveCont_Left_Speed30(-119),
        kRevDriveCont_Left_Speed31(-112),
        kRevDriveCont_Left_Speed32(-111),
        kRevDriveCont_Right_Speed1(64),
        kRevDriveCont_Right_Speed2(65),
        kRevDriveCont_Right_Speed3(66),
        kRevDriveCont_Right_Speed4(67),
        kRevDriveCont_Right_Speed5(68),
        kRevDriveCont_Right_Speed6(69),
        kRevDriveCont_Right_Speed7(70),
        kRevDriveCont_Right_Speed8(71),
        kRevDriveCont_Right_Speed9(72),
        kRevDriveCont_Right_Speed10(73),
        kRevDriveCont_Right_Speed11(80),
        kRevDriveCont_Right_Speed12(81),
        kRevDriveCont_Right_Speed13(82),
        kRevDriveCont_Right_Speed14(83),
        kRevDriveCont_Right_Speed15(84),
        kRevDriveCont_Right_Speed16(85),
        kRevDriveCont_Right_Speed17(86),
        kRevDriveCont_Right_Speed18(87),
        kRevDriveCont_Right_Speed19(88),
        kRevDriveCont_Right_Speed20(89),
        kRevDriveCont_Right_Speed21(96),
        kRevDriveCont_Right_Speed22(97),
        kRevDriveCont_Right_Speed23(98),
        kRevDriveCont_Right_Speed24(99),
        kRevDriveCont_Right_Speed25(100),
        kRevDriveCont_Right_Speed26(101),
        kRevDriveCont_Right_Speed27(102),
        kRevDriveCont_Right_Speed28(103),
        kRevDriveCont_Right_Speed29(104),
        kRevDriveCont_Right_Speed30(105),
        kRevDriveCont_Right_Speed31(112),
        kRevDriveCont_Right_Speed32(113);

        byte value;

        kRevDriveContinuousValue(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }
    }
}

