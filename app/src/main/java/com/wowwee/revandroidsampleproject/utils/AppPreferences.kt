package com.wowwee.revandroidsampleproject.utils

import android.content.Context
import androidx.core.content.edit
import java.util.Locale
import kotlin.random.Random

private const val DEFAULT_KIOSK_MODE_ON = true

object AppPreferences {

    private const val PREFS_NAME = "rev_scan_prefs"
    private const val PREF_HAS_CONNECTED_REV = "has_connected_rev"
    private const val PREF_HAS_SEEN_DRIVER_MODE_INSTRUCTIONS = "has_seen_driver_mode_instructions"
    private const val PREF_HAS_SEEN_ADVANCED_MODE_INSTRUCTIONS = "has_seen_advanced_mode_instructions"
    private const val PREF_PATH_MODE_INSTRUCTIONS_SEEN = "path_mode_instructions_seen"
    private const val PREF_KIOSK_ENABLE_ALLOWED_AT_MS = "kiosk_enable_allowed_at_ms"
    private const val PREF_KIOSK_LOCK_GLOBALLY_ENABLED = "kiosk_lock_globally_enabled"
    private const val PREF_KIOSK_LOCK_DISABLED_BY_USER = "kiosk_lock_disabled_by_user"
    private const val PREF_LAST_PRIMARY_CAR_ID = "last_primary_car_id"
    private const val PREF_SIMULATOR_ASSIGNED_REV_NAME = "simulator_assigned_rev_name"
    private const val PREF_CAR_PROFILE_NAME_PREFIX = "car_profile_name_"
    private const val PREF_CAR_PROFILE_COLOR_PREFIX = "car_profile_color_"
    private const val DEFAULT_CAR_COLOR_HEX = "#3F51B5"

    @JvmStatic
    fun hasConnectedRevBefore(context: Context?): Boolean =
        getBoolean(context, PREF_HAS_CONNECTED_REV, false)

    @JvmStatic
    fun markHasConnectedRev(context: Context?) {
        putBoolean(context, PREF_HAS_CONNECTED_REV, DEFAULT_KIOSK_MODE_ON)
    }

    @JvmStatic
    fun hasSeenDriverModeInstructions(context: Context?): Boolean =
        getBoolean(context, PREF_HAS_SEEN_DRIVER_MODE_INSTRUCTIONS, false)

    @JvmStatic
    fun markSeenDriverModeInstructions(context: Context?) {
        putBoolean(context, PREF_HAS_SEEN_DRIVER_MODE_INSTRUCTIONS, DEFAULT_KIOSK_MODE_ON)
    }

    @JvmStatic
    fun hasSeenAdvancedModeInstructions(context: Context?): Boolean =
        getBoolean(context, PREF_HAS_SEEN_ADVANCED_MODE_INSTRUCTIONS, false)

    @JvmStatic
    fun markSeenAdvancedModeInstructions(context: Context?) {
        putBoolean(context, PREF_HAS_SEEN_ADVANCED_MODE_INSTRUCTIONS, DEFAULT_KIOSK_MODE_ON)
    }

    @JvmStatic
    fun hasSeenPathModeInstructions(context: Context?): Boolean =
        getBoolean(context, PREF_PATH_MODE_INSTRUCTIONS_SEEN, false)

    @JvmStatic
    fun markSeenPathModeInstructions(context: Context?) {
        putBoolean(context, PREF_PATH_MODE_INSTRUCTIONS_SEEN, DEFAULT_KIOSK_MODE_ON)
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
        getBoolean(context, PREF_KIOSK_LOCK_GLOBALLY_ENABLED, DEFAULT_KIOSK_MODE_ON)

    @JvmStatic
    fun setKioskLockGloballyEnabled(context: Context?, enabled: Boolean) {
        putBoolean(context, PREF_KIOSK_LOCK_GLOBALLY_ENABLED, enabled)
    }

    @JvmStatic
    fun isKioskLockDisabledByUser(context: Context?): Boolean =
        getBoolean(context, PREF_KIOSK_LOCK_DISABLED_BY_USER, false)

    @JvmStatic
    fun setKioskLockDisabledByUser(context: Context?, disabled: Boolean) {
        putBoolean(context, PREF_KIOSK_LOCK_DISABLED_BY_USER, disabled)
    }

    @JvmStatic
    fun setLastPrimaryCarId(context: Context?, carId: String?) {
        val normalized = carId?.trim()?.takeIf { it.isNotEmpty() }
        putString(context, PREF_LAST_PRIMARY_CAR_ID, normalized)
    }

    @JvmStatic
    fun lastPrimaryCarId(context: Context?): String? {
        return getString(context, PREF_LAST_PRIMARY_CAR_ID, null)?.trim()?.takeIf { it.isNotEmpty() }
    }

    @JvmStatic
    fun defaultCarColorHex(): String = DEFAULT_CAR_COLOR_HEX

    @JvmStatic
    fun getOrCreateSimulatorAssignedRevName(context: Context?): String {
        val existing = getString(context, PREF_SIMULATOR_ASSIGNED_REV_NAME, null)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        if (existing != null) {
            return existing
        }

        val generated = "REV-${Random.nextInt(1000, 10000)}"
        putString(context, PREF_SIMULATOR_ASSIGNED_REV_NAME, generated)
        return generated
    }

    @JvmStatic
    fun saveCarProfile(context: Context?, carId: String, displayName: String, colorHex: String) {
        val normalizedCarId = normalizeCarId(carId) ?: return
        putString(context, PREF_CAR_PROFILE_NAME_PREFIX + normalizedCarId, displayName.trim())
        putString(context, PREF_CAR_PROFILE_COLOR_PREFIX + normalizedCarId, normalizeColorHex(colorHex))
    }

    @JvmStatic
    fun carProfileName(context: Context?, carId: String, fallbackName: String): String {
        val normalizedCarId = normalizeCarId(carId) ?: return fallbackName
        return getString(context, PREF_CAR_PROFILE_NAME_PREFIX + normalizedCarId, fallbackName)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: fallbackName
    }

    @JvmStatic
    fun carProfileColorHex(context: Context?, carId: String, fallbackColorHex: String = DEFAULT_CAR_COLOR_HEX): String {
        val normalizedCarId = normalizeCarId(carId) ?: return normalizeColorHex(fallbackColorHex)
        val stored = getString(context, PREF_CAR_PROFILE_COLOR_PREFIX + normalizedCarId, fallbackColorHex)
        return normalizeColorHex(stored)
    }

    @JvmStatic
    fun hasCarProfile(context: Context?, carId: String): Boolean {
        val normalizedCarId = normalizeCarId(carId) ?: return false
        val hasName = !getString(context, PREF_CAR_PROFILE_NAME_PREFIX + normalizedCarId, null).isNullOrBlank()
        val hasColor = !getString(context, PREF_CAR_PROFILE_COLOR_PREFIX + normalizedCarId, null).isNullOrBlank()
        return hasName || hasColor
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

    private fun normalizeCarId(carId: String?): String? {
        val raw = carId?.trim()?.uppercase(Locale.US) ?: return null
        if (raw.isEmpty()) return null
        return raw.replace(Regex("[^A-Z0-9]"), "_")
    }

    private fun normalizeColorHex(colorHex: String?): String {
        val candidate = colorHex?.trim()?.uppercase(Locale.US) ?: return DEFAULT_CAR_COLOR_HEX
        return if (Regex("^#[0-9A-F]{6}$").matches(candidate)) candidate else DEFAULT_CAR_COLOR_HEX
    }
}
