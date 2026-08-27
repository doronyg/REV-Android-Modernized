package com.wowwee.revandroidsampleproject.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticUtils {
    @JvmStatic
    fun vibrate(context: Context?, durationMs: Long, amplitude : Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        val ctx = context ?: return
        if (durationMs <= 0L) {
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = ctx.getSystemService(VibratorManager::class.java) ?: return
                manager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(durationMs, amplitude)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = ctx.getSystemService(Vibrator::class.java) ?: return
                vibrator.vibrate(
                    VibrationEffect.createOneShot(durationMs, amplitude)
                )
            }
        } catch (_: Throwable) {
        }
    }
}

