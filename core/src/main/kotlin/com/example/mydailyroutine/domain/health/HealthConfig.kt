package com.example.mydailyroutine.domain.health

/** User settings are immutable data; defaults preserve the original six deterministic rules. */
data class HealthConfig(
    val focusLimitMinutes: Int = 90,
    val cognitiveLimitMinutes: Int = 180,
    val transitionMinutes: Int = 30,
    val dailyFocusLimitMinutes: Int = 300,
    val sedentaryLimitMinutes: Int = 120,
    val fragmentedMinMinutes: Int = 45,
    val fragmentedMaxMinutes: Int = 90,
    val concentrationEnabled: Boolean = true,
    val cognitiveEnabled: Boolean = true,
    val transitionEnabled: Boolean = true,
    val dailyFocusEnabled: Boolean = true,
    val sedentaryEnabled: Boolean = true,
    val fragmentationEnabled: Boolean = true,
) {
    init {
        require(focusLimitMinutes in 1..720)
        require(cognitiveLimitMinutes in 30..720)
        require(transitionMinutes in 0..240)
        require(dailyFocusLimitMinutes in 30..1440)
        require(sedentaryLimitMinutes in 15..720)
        require(fragmentedMinMinutes in 1..1439 && fragmentedMaxMinutes in fragmentedMinMinutes..1439)
    }
    fun isEnabled(type: WarningType): Boolean = when (type) {
        WarningType.CONCENTRATION_LIMIT -> concentrationEnabled
        WarningType.HIGH_COGNITIVE_LOAD -> cognitiveEnabled
        WarningType.INSUFFICIENT_TRANSITION -> transitionEnabled
        WarningType.BURNOUT_RISK -> dailyFocusEnabled
        WarningType.PHYSICAL_RESET -> sedentaryEnabled
        WarningType.FRAGMENTED_TIME -> fragmentationEnabled
    }
    companion object {
        const val COGNITIVE_RECOVERY_MINUTES = 20
        const val PHYSICAL_RECOVERY_MINUTES = 5
        const val SUGGESTED_BREAK_MINUTES = 15
    }
}
