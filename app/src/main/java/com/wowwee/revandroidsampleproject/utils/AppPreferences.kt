package com.wowwee.revandroidsampleproject.utils

import android.content.Context

object AppPreferences {

    private const val PREFS_NAME = "rev_scan_prefs"
    private const val PREF_HAS_CONNECTED_REV = "has_connected_rev"

    @JvmStatic
    fun hasConnectedRevBefore(context: Context?): Boolean =
        getBoolean(context, PREF_HAS_CONNECTED_REV, false)

    @JvmStatic
    fun markHasConnectedRev(context: Context?) {
        putBoolean(context, PREF_HAS_CONNECTED_REV, true)
    }

    @JvmStatic
    fun getBoolean(context: Context?, key: String, defaultValue: Boolean): Boolean {
        val prefs = prefs(context) ?: return defaultValue
        return prefs.getBoolean(key, defaultValue)
    }

    @JvmStatic
    fun putBoolean(context: Context?, key: String, value: Boolean) {
        val prefs = prefs(context) ?: return
        prefs.edit().putBoolean(key, value).apply()
    }

    @JvmStatic
    fun getString(context: Context?, key: String, defaultValue: String?): String? {
        val prefs = prefs(context) ?: return defaultValue
        return prefs.getString(key, defaultValue)
    }

    @JvmStatic
    fun putString(context: Context?, key: String, value: String?) {
        val prefs = prefs(context) ?: return
        prefs.edit().putString(key, value).apply()
    }

    private fun prefs(context: Context?) =
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
