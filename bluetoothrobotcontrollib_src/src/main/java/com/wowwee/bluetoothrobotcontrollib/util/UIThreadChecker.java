/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.os.Looper
 *  android.util.Log
 */
package com.wowwee.bluetoothrobotcontrollib.util;

import android.os.Looper;
import android.util.Log;

public class UIThreadChecker {
    public static boolean checkIsUIThread(String debugMessage) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.d("UIThreadChecker", debugMessage + " is on UI Thread");
            return true;
        }
        Log.d("UIThreadChecker", debugMessage + " is NOT on UI Thread");
        return false;
    }
}

