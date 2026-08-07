package com.wowwee.revandroidsampleproject.utils

import android.os.SystemClock
import android.util.Log
import kotlin.math.roundToInt

object DriveCommandSampler {

    // Toggle this flag while testing to enable/disable command sampling logs.
    @JvmField
    var isEnabled: Boolean = true

    private const val TAG = "DriveCmdSampler"
    private val lastTimestampBySource = HashMap<String, Long>()

    @JvmStatic
    fun logDrive(source: String, x: Float, y: Float, note: String = "") {
        if (!isEnabled) {
            return
        }

        val now = SystemClock.elapsedRealtime()
        val previous = lastTimestampBySource[source]
        val dtMs = if (previous == null) -1L else now - previous
        lastTimestampBySource[source] = now

        val xScaled = (x * 31f).roundToInt().coerceIn(-31, 31)
        val yScaled = (y * 31f).roundToInt().coerceIn(-31, 31)
        val dtLabel = if (dtMs >= 0L) dtMs.toString() else "first"

        Log.d(
            TAG,
            "src=$source dtMs=$dtLabel x=${format(x)} y=${format(y)} x31=$xScaled y31=$yScaled $note".trim()
        )
    }

    private fun format(value: Float): String {
        return String.format(java.util.Locale.US, "%.3f", value)
    }
}

