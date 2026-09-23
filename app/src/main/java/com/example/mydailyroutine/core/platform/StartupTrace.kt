package com.example.mydailyroutine.core.platform

import android.os.SystemClock
import android.util.Log

/**
 * One number, in the log, about the first minute (N19).
 *
 * The product's first promise is that the day is on screen before the reader has finished taking the
 * phone out of a pocket. That promise cannot be checked on a machine that is idle, so the app measures
 * itself: the moment the process is created, and the moment the first day is actually drawn. The
 * difference is printed once per launch and goes nowhere else — no analytics, no file, no network, and
 * nothing is kept after the process ends, which is the whole point of an app that promises not to
 * phone home.
 *
 * It is a `Log.i` on purpose: the number is for whoever is holding the phone with `adb logcat` open,
 * and for the CI emulator, whose log is uploaded as an artifact.
 */
object StartupTrace {

    private const val Tag = "LockIn"

    /** The budget the plan set for a mid-range phone: 900 ms from launch to a drawn day. */
    const val BudgetMillis = 900L

    private var processStartedAt: Long? = null
    private var firstDayLogged = false

    /** Called from `Application.onCreate`: the earliest moment this process can measure itself. */
    fun markProcessStart() {
        if (processStartedAt == null) processStartedAt = SystemClock.elapsedRealtime()
    }

    /**
     * Called once, when the day is on screen. Silently does nothing when the process start was never
     * marked — a test that instantiates the app by hand should not produce a misleading "0 ms".
     */
    fun firstDayDrawn() {
        if (firstDayLogged) return
        val started = processStartedAt ?: return
        firstDayLogged = true
        val elapsed = SystemClock.elapsedRealtime() - started
        Log.i(Tag, "first-day-rendered: ${elapsed}ms since process start (budget ${BudgetMillis}ms)")
    }
}
