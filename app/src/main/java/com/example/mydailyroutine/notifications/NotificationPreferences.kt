package com.example.mydailyroutine.notifications

import android.content.Context

/** Local-only notification controls. Values survive reboot and never leave the device. */
object NotificationPreferences {
    private const val FILE = "routine_preferences"
    private const val MUTE_DURING_SCHOOL_HOURS = "muteDuringSchoolHours"
    private const val SCHOOL_START_MINUTES = "schoolStartMinutes"
    private const val SCHOOL_END_MINUTES = "schoolEndMinutes"

    fun muteDuringSchoolHours(context: Context): Boolean = preferences(context).getBoolean(MUTE_DURING_SCHOOL_HOURS, true)

    fun setMuteDuringSchoolHours(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(MUTE_DURING_SCHOOL_HOURS, enabled).apply()
    }

    fun schoolStartMinutes(context: Context): Int = preferences(context).getInt(SCHOOL_START_MINUTES, 7 * 60 + 45)

    fun schoolEndMinutes(context: Context): Int = preferences(context).getInt(SCHOOL_END_MINUTES, 14 * 60 + 30)

    fun setSchoolWindow(context: Context, startMinutes: Int, endMinutes: Int) {
        preferences(context).edit()
            .putInt(SCHOOL_START_MINUTES, startMinutes.coerceIn(0, 1439))
            .putInt(SCHOOL_END_MINUTES, endMinutes.coerceIn(0, 1439))
            .apply()
    }

    private fun preferences(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
