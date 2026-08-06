package com.wowwee.revandroidsampleproject.utils;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

public final class BroadcastReceiverUtils {

    private BroadcastReceiverUtils() {
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public static boolean registerReceiver(Context context,
                                           BroadcastReceiver receiver,
                                           IntentFilter filter,
                                           boolean isRegistered,
                                           boolean exported) {
        if (isRegistered || context == null || receiver == null || filter == null) {
            return isRegistered;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                int receiverFlag = exported ? Context.RECEIVER_EXPORTED : Context.RECEIVER_NOT_EXPORTED;
                context.registerReceiver(receiver, filter, receiverFlag);
            } else {
                context.registerReceiver(receiver, filter);
            }
        } catch (SecurityException | IllegalArgumentException ex) {
            Log.e(BroadcastReceiverUtils.class.getSimpleName(), "Failed to register broadcast receiver.", ex);
            return false;
        }

        return true;
    }

    public static void unregisterReceiver(Context context,
                                          BroadcastReceiver receiver,
                                          boolean isRegistered,
                                          String logTag) {
        boolean canUnregister = isRegistered && context != null && receiver != null;
        if (canUnregister) {
            try {
                context.unregisterReceiver(receiver);
            } catch (IllegalArgumentException ex) {
                Log.w(logTag, "Broadcast receiver already unregistered.", ex);
            }
        }
    }
}

