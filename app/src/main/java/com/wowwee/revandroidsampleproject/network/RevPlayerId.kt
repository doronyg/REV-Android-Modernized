package com.wowwee.revandroidsampleproject.network

import java.util.Locale

/**
 * Wrapper for a unique REV identifier (typically BLE MAC address).
 */
data class RevPlayerId(val value: String) {
    init {
        require(value.isNotBlank()) { "RevPlayerId value must not be blank." }
    }

    fun normalized(): String = value.trim().uppercase(Locale.US)

    override fun toString(): String = value

    companion object {
        fun from(value: String): RevPlayerId = RevPlayerId(value.trim())
    }
}

