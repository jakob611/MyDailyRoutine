package com.example.mydailyroutine.data.preferences

import androidx.datastore.preferences.core.*
import com.example.mydailyroutine.domain.planning.PlanningConfig

object PlanningPreferenceCodec {
    private fun key(name: String) = intPreferencesKey("planning_$name")
    fun read(values: Preferences): PlanningConfig = runCatching {
        val defaults = PlanningConfig()
        PlanningConfig(values[key("capacity")] ?: defaults.dailyStudyCapacityMinutes,
            values[key("start")] ?: defaults.studyStartMinutes, values[key("end")] ?: defaults.studyEndMinutes,
            values[key("dip")] ?: defaults.dipCenterMinutes, values[key("sigma")] ?: defaults.dipSigmaMinutes,
            values[key("slip")] ?: defaults.defaultSlipMinutes, values[key("focus")] ?: defaults.targetFocusMinutes,
            values[key("transition")] ?: defaults.postSchoolRecoveryMinutes)
    }.getOrDefault(PlanningConfig())
    fun write(values: MutablePreferences, config: PlanningConfig) {
        values[key("capacity")] = config.dailyStudyCapacityMinutes
        values[key("start")] = config.studyStartMinutes
        values[key("end")] = config.studyEndMinutes
        values[key("dip")] = config.dipCenterMinutes
        values[key("sigma")] = config.dipSigmaMinutes
        values[key("slip")] = config.defaultSlipMinutes
        values[key("focus")] = config.targetFocusMinutes
        values[key("transition")] = config.postSchoolRecoveryMinutes
    }
}
