package com.example.mydailyroutine.domain.health

/**
 * Configuration for proactive, periodic focus-break insertion.
 *
 * Unlike the reactive [RecoveryPlanner] (which offers a break only once a block already exceeds the
 * ultradian limit), this drives *scheduled* micro-breaks every [everyMinutes] of focused work.
 *
 * It is intentionally conservative and never annoying:
 * - breaks are only suggested where there is no natural rest already (an explicit break, a meal,
 *   a free gap, or the longer lunch), so we never double-insert;
 * - the last segment is left alone (no break is suggested after the final focus block);
 * - all values are user-tunable from Settings.
 */
data class PeriodicBreakConfig(
    val enabled: Boolean = false,
    val everyMinutes: Int = 60,
    val breakMinutes: Int = 5,
) {
    init {
        require(everyMinutes in 30..240) { "everyMinutes must be 30..240, was $everyMinutes" }
        require(breakMinutes in 1..60) { "breakMinutes must be 1..60, was $breakMinutes" }
    }
}
