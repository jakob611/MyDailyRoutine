package com.example.mydailyroutine.core.preferences

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.mydailyroutine.domain.health.HealthConfig

/** Atomic, version-independent primitives; corrupt/out-of-range settings fall back without overwriting. */
object HealthPreferenceCodec {
    private fun int(name: String) = intPreferencesKey("health_$name")
    private fun enabled(name: String) = booleanPreferencesKey("health_${name}_enabled")
    fun read(values: Preferences): HealthConfig = runCatching {
        val defaults = HealthConfig()
        HealthConfig(
            focusLimitMinutes = values[int("focus")] ?: defaults.focusLimitMinutes,
            cognitiveLimitMinutes = values[int("cognitive")] ?: defaults.cognitiveLimitMinutes,
            transitionMinutes = values[int("transition")] ?: defaults.transitionMinutes,
            dailyFocusLimitMinutes = values[int("daily")] ?: defaults.dailyFocusLimitMinutes,
            sedentaryLimitMinutes = values[int("sedentary")] ?: defaults.sedentaryLimitMinutes,
            fragmentedMinMinutes = values[int("fragmented_min")] ?: defaults.fragmentedMinMinutes,
            fragmentedMaxMinutes = values[int("fragmented_max")] ?: defaults.fragmentedMaxMinutes,
            concentrationEnabled = values[enabled("focus")] ?: true,
            cognitiveEnabled = values[enabled("cognitive")] ?: true,
            transitionEnabled = values[enabled("transition")] ?: true,
            dailyFocusEnabled = values[enabled("daily")] ?: true,
            sedentaryEnabled = values[enabled("sedentary")] ?: true,
            fragmentationEnabled = values[enabled("fragmented")] ?: true,
        )
    }.getOrDefault(HealthConfig())

    fun write(values: MutablePreferences, config: HealthConfig) {
        values[int("focus")] = config.focusLimitMinutes
        values[int("cognitive")] = config.cognitiveLimitMinutes
        values[int("transition")] = config.transitionMinutes
        values[int("daily")] = config.dailyFocusLimitMinutes
        values[int("sedentary")] = config.sedentaryLimitMinutes
        values[int("fragmented_min")] = config.fragmentedMinMinutes
        values[int("fragmented_max")] = config.fragmentedMaxMinutes
        values[enabled("focus")] = config.concentrationEnabled
        values[enabled("cognitive")] = config.cognitiveEnabled
        values[enabled("transition")] = config.transitionEnabled
        values[enabled("daily")] = config.dailyFocusEnabled
        values[enabled("sedentary")] = config.sedentaryEnabled
        values[enabled("fragmented")] = config.fragmentationEnabled
    }
}
