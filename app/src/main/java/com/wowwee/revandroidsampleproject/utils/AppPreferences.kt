package com.wowwee.revandroidsampleproject.utils

import android.content.Context
import androidx.core.content.edit

object AppPreferences {

    private const val PREFS_NAME = "rev_scan_prefs"
    private const val PREF_HAS_CONNECTED_REV = "has_connected_rev"
    private const val PREF_HAS_SEEN_DRIVER_MODE_INSTRUCTIONS = "has_seen_driver_mode_instructions"
    private const val PREF_PATH_MODE_INSTRUCTIONS_SEEN = "path_mode_instructions_seen"
    private const val PREF_KIOSK_ENABLE_ALLOWED_AT_MS = "kiosk_enable_allowed_at_ms"
    private const val PREF_KIOSK_LOCK_GLOBALLY_ENABLED = "kiosk_lock_globally_enabled"

    @JvmStatic
    fun hasConnectedRevBefore(context: Context?): Boolean =
        getBoolean(context, PREF_HAS_CONNECTED_REV, false)

    @JvmStatic
    fun markHasConnectedRev(context: Context?) {
        putBoolean(context, PREF_HAS_CONNECTED_REV, true)
    }

    @JvmStatic
    fun hasSeenDriverModeInstructions(context: Context?): Boolean =
        getBoolean(context, PREF_HAS_SEEN_DRIVER_MODE_INSTRUCTIONS, false)

    @JvmStatic
    fun markSeenDriverModeInstructions(context: Context?) {
        putBoolean(context, PREF_HAS_SEEN_DRIVER_MODE_INSTRUCTIONS, true)
    }

    @JvmStatic
    fun hasSeenPathModeInstructions(context: Context?): Boolean =
        getBoolean(context, PREF_PATH_MODE_INSTRUCTIONS_SEEN, false)

    @JvmStatic
    fun markSeenPathModeInstructions(context: Context?) {
        putBoolean(context, PREF_PATH_MODE_INSTRUCTIONS_SEEN, true)
    }

    @JvmStatic
    fun kioskEnableAllowedAtMs(context: Context?): Long =
        getLong(context, PREF_KIOSK_ENABLE_ALLOWED_AT_MS, 0L)

    @JvmStatic
    fun setKioskEnableAllowedAtMs(context: Context?, value: Long) {
        putLong(context, PREF_KIOSK_ENABLE_ALLOWED_AT_MS, value)
    }

    @JvmStatic
    fun isKioskLockGloballyEnabled(context: Context?): Boolean =
        getBoolean(context, PREF_KIOSK_LOCK_GLOBALLY_ENABLED, true)

    @JvmStatic
    fun setKioskLockGloballyEnabled(context: Context?, enabled: Boolean) {
        putBoolean(context, PREF_KIOSK_LOCK_GLOBALLY_ENABLED, enabled)
    }

    @JvmStatic
    fun getBoolean(context: Context?, key: String, defaultValue: Boolean): Boolean {
        val prefs = prefs(context) ?: return defaultValue
        return prefs.getBoolean(key, defaultValue)
    }

    @JvmStatic
    fun putBoolean(context: Context?, key: String, value: Boolean) {
        val prefs = prefs(context) ?: return
        prefs.edit { putBoolean(key, value) }
    }

    @JvmStatic
    fun getString(context: Context?, key: String, defaultValue: String?): String? {
        val prefs = prefs(context) ?: return defaultValue
        return prefs.getString(key, defaultValue)
    }

    @JvmStatic
    fun putString(context: Context?, key: String, value: String?) {
        val prefs = prefs(context) ?: return
        prefs.edit { putString(key, value) }
    }

    @JvmStatic
    fun getLong(context: Context?, key: String, defaultValue: Long): Long {
        val prefs = prefs(context) ?: return defaultValue
        return prefs.getLong(key, defaultValue)
    }

    @JvmStatic
    fun putLong(context: Context?, key: String, value: Long) {
        val prefs = prefs(context) ?: return
        prefs.edit { putLong(key, value) }
    }

    private fun prefs(context: Context?) =
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
