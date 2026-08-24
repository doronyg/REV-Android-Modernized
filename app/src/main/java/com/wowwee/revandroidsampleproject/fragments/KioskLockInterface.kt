package com.wowwee.revandroidsampleproject.fragments

/**
 * Contract for fragments that want activity-level kiosk lock handling.
 */
interface KioskLockInterface {
    /**
     * Whether this fragment should run under kiosk lock handling.
     */
    fun isKioskLockEnabled(): Boolean

    /**
     * Called by the host activity when the Back button is pressed.
     * Return true when consumed.
     */
    fun onKioskBackPressed(): Boolean

    /**
     * Optional callback for lock-task state updates.
     */
    fun onKioskModeChanged(isLocked: Boolean) {
        // Optional for implementers.
    }
}

