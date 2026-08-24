package com.wowwee.revandroidsampleproject.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.wowwee.revandroidsampleproject.fragments.KioskLockInterface
import java.lang.ref.WeakReference

class KioskLockManager {
    companion object {
        const val KIOSK_REENABLE_HOLDOFF_AFTER_UNLOCK_MS = 15_000L
    }

    private val tag = "KioskLockManager"
    private val delayHandler = Handler(Looper.getMainLooper())

    private var pendingEnableRunnable: Runnable? = null
    private var lastShouldLock = false

    fun isKioskLockGloballyEnabled(activity: FragmentActivity): Boolean =
        AppPreferences.isKioskLockGloballyEnabled(activity)

    fun setKioskLockGloballyEnabled(
        activity: FragmentActivity,
        enabled: Boolean,
        lockTarget: KioskLockInterface?
    ) {
        AppPreferences.setKioskLockGloballyEnabled(activity, enabled)
        Log.d(tag, "setKioskLockGloballyEnabled(): enabled=$enabled")
        syncKioskState(activity, lockTarget)
    }

    fun onHostResume(activity: FragmentActivity, lockTarget: KioskLockInterface?) {
        syncKioskState(activity, lockTarget)
    }

    fun onHostPause(activity: FragmentActivity, lockTarget: KioskLockInterface?) {
        // Avoid turning kiosk on while the app is backgrounded.
        if (shouldLockForTarget(activity, lockTarget)) {
            extendHoldoffFromNow(activity, "host pause")
        }
        cancelPendingEnable()
    }

    fun onHostDestroy(activity: FragmentActivity) {
        Log.d(tag, "onHostDestroy(): cancel pending enable + exit kiosk if active")
        if (lastShouldLock) {
            extendHoldoffFromNow(activity, "host destroy")
        }
        cancelPendingEnable()
        exitKioskIfActive(activity)
    }

    fun onBackPressed(activity: FragmentActivity, lockTarget: KioskLockInterface?): Boolean {
        val target = lockTarget ?: return false
        if (!shouldLockForTarget(activity, target)) {
            return false
        }

        val consumed = target.onKioskBackPressed()
        Log.d(tag, "onBackPressed(): consumed=$consumed target=${target.javaClass.simpleName}")
        syncKioskState(activity, lockTarget)
        return consumed
    }

    fun syncKioskState(activity: FragmentActivity, lockTarget: KioskLockInterface?) {
        val previousShouldLock = lastShouldLock
        val globalEnabled = AppPreferences.isKioskLockGloballyEnabled(activity)
        val shouldLock = lockTarget?.isKioskLockEnabled() == true && globalEnabled
        val kioskEnabled = isInAnyLockTaskMode(activity)

        if (!shouldLock) {
            if (previousShouldLock || kioskEnabled) {
                extendHoldoffFromNow(activity, "lock exit transition")
            }
            cancelPendingEnable()
            exitKioskIfActive(activity)
            val kioskEnabledNow = isInAnyLockTaskMode(activity)
            lockTarget?.onKioskModeChanged(kioskEnabledNow)
            lastShouldLock = false
            Log.d(tag, "syncKioskState(): shouldLock=false kioskEnabledNow=$kioskEnabledNow")
            return
        }

        if (kioskEnabled) {
            cancelPendingEnable()
            lockTarget?.onKioskModeChanged(true)
            lastShouldLock = true
            return
        }

        val enableAtMs = maxOf(kioskEnableAllowedAtMs(activity), System.currentTimeMillis())
        Log.d(tag, "syncKioskState(): schedule enableAtMs=$enableAtMs")
        scheduleEnable(activity, lockTarget ?: return, enableAtMs)
        lastShouldLock = true
    }

    private fun scheduleEnable(
        activity: FragmentActivity,
        lockTarget: KioskLockInterface,
        enableAtMs: Long
    ) {
        cancelPendingEnable()
        val delayMs = maxOf(0L, enableAtMs - System.currentTimeMillis())
        val activityRef = WeakReference(activity)
        val targetRef = WeakReference(lockTarget)

        pendingEnableRunnable = Runnable {
            pendingEnableRunnable = null

            val host = activityRef.get() ?: return@Runnable
            val target = targetRef.get() ?: return@Runnable

            if (!shouldLockForTarget(host, target)) {
                return@Runnable
            }

            val now = System.currentTimeMillis()
            val allowedAtMs = kioskEnableAllowedAtMs(host)
            if (now < allowedAtMs) {
                scheduleEnable(host, target, allowedAtMs)
                return@Runnable
            }

            Log.d(tag, "scheduleEnable.run(): holdoff passed, entering kiosk")
            enterKioskIfPossible(host)
            val enabled = isInAnyLockTaskMode(host)
            target.onKioskModeChanged(enabled)
            Log.d(tag, "scheduleEnable.run(): kiosk enabled result=$enabled")
        }
        pendingEnableRunnable?.let {
            delayHandler.postDelayed(it, delayMs)
        }
    }

    private fun cancelPendingEnable() {
        val runnable = pendingEnableRunnable ?: return
        delayHandler.removeCallbacks(runnable)
        pendingEnableRunnable = null
    }

    private fun setKioskEnableAllowedAtMs(activity: FragmentActivity, value: Long) {
        AppPreferences.setKioskEnableAllowedAtMs(activity, value)
    }

    private fun kioskEnableAllowedAtMs(activity: FragmentActivity): Long {
        return AppPreferences.kioskEnableAllowedAtMs(activity)
    }

    private fun extendHoldoffFromNow(activity: FragmentActivity, reason: String) {
        val now = System.currentTimeMillis()
        val holdoffUntil = maxOf(
            kioskEnableAllowedAtMs(activity),
            now + KIOSK_REENABLE_HOLDOFF_AFTER_UNLOCK_MS
        )
        Log.d(tag, "extendHoldoffFromNow(): reason=$reason holdoffUntil=$holdoffUntil")
        setKioskEnableAllowedAtMs(activity, holdoffUntil)
    }

    private fun shouldLockForTarget(activity: FragmentActivity, lockTarget: KioskLockInterface?): Boolean {
        if (lockTarget == null) {
            return false
        }
        return AppPreferences.isKioskLockGloballyEnabled(activity) && lockTarget.isKioskLockEnabled()
    }

    private fun enterKioskIfPossible(activity: FragmentActivity) {
        if (isInAnyLockTaskMode(activity)) {
            return
        }

        try {
            activity.startLockTask()
            Log.d(tag, "enterKioskIfPossible(): started lock task")
        } catch (ex: IllegalStateException) {
            Log.w(tag, "enterKioskIfPossible(): failed to start lock task", ex)
        } catch (ex: SecurityException) {
            Log.w(tag, "enterKioskIfPossible(): security exception", ex)
        }
    }

    private fun exitKioskIfActive(activity: FragmentActivity) {
        if (!isInAnyLockTaskMode(activity)) {
            return
        }

        try {
            activity.stopLockTask()
            Log.d(tag, "exitKioskIfActive(): stopped lock task")
        } catch (ex: IllegalStateException) {
            Log.w(tag, "exitKioskIfActive(): failed to stop lock task", ex)
        }
    }

    private fun isInAnyLockTaskMode(activity: FragmentActivity): Boolean {
        val activityManager = activity.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return false

        return activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }
}
