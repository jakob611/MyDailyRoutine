package com.example.mydailyroutine.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mydailyroutine.domain.health.HealthConfig
import com.example.mydailyroutine.domain.health.PeriodicBreakConfig
import com.example.mydailyroutine.domain.model.SchedulePreferences
import com.example.mydailyroutine.domain.routines.EntryDefaults
import com.example.mydailyroutine.domain.model.ScheduleValidation
import com.example.mydailyroutine.domain.repository.PreferencesRepository
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.scheduleDataStore by preferencesDataStore(name = "schedule_preferences")

class DataStorePreferencesRepository(context: Context, private val onChanged: () -> Unit) : PreferencesRepository {
    private val store = context.applicationContext.scheduleDataStore
    private val muteKey = booleanPreferencesKey("mute_during_school")
    private val startKey = intPreferencesKey("school_start_minute")
    private val endKey = intPreferencesKey("school_end_minute")
    private val teachingEndKey = longPreferencesKey("teaching_end_epoch_day")
    private val healingKey = booleanPreferencesKey("automatic_healing_enabled")
    private val hapticsKey = booleanPreferencesKey("haptics_enabled")
    private val lessonKey = intPreferencesKey("default_lesson_minutes")
    private val breakKey = intPreferencesKey("default_lesson_break_minutes")
    private val periodicKey = booleanPreferencesKey("periodic_break_enabled")
    private val periodicEveryKey = intPreferencesKey("periodic_break_every_minutes")
    private val periodicLenKey = intPreferencesKey("periodic_break_minutes")
    private val defaults = SchedulePreferences()

    override val preferences = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { values ->
        SchedulePreferences(
            muteDuringSchoolHours = values[muteKey] ?: defaults.muteDuringSchoolHours,
            schoolStart = time(values[startKey], defaults.schoolStart),
            schoolEnd = time(values[endKey], defaults.schoolEnd),
            hapticsEnabled = values[hapticsKey] ?: true,
            automaticHealingEnabled = values[healingKey] ?: true,
            entryDefaults = EntryDefaults(values[lessonKey]?.takeIf { it in 1..240 } ?: 45, values[breakKey]?.takeIf { it in 1..60 } ?: 5),
            health = HealthPreferenceCodec.read(values),
            planning = PlanningPreferenceCodec.read(values),
            periodicBreak = PeriodicBreakConfig(
                values[periodicKey] ?: false,
                values[periodicEveryKey]?.takeIf { it in 30..240 } ?: 60,
                values[periodicLenKey]?.takeIf { it in 1..60 } ?: 5,
            ),
            teachingEndDate = values[teachingEndKey]?.let { runCatching { LocalDate.ofEpochDay(it) }.getOrNull() }
                ?: defaults.teachingEndDate,
        )
    }.distinctUntilChanged()

    private fun time(minutes: Int?, fallback: LocalTime): LocalTime =
        minutes?.takeIf { it in 0..1439 }?.let { LocalTime.ofSecondOfDay(it * 60L) } ?: fallback

    override suspend fun setMuteDuringSchoolHours(muted: Boolean) {
        store.edit { it[muteKey] = muted }
        onChanged()
    }

    override suspend fun setSchoolWindow(start: LocalTime, end: LocalTime) {
        ScheduleValidation.times(start, end)
        store.edit {
            it[startKey] = start.toSecondOfDay() / 60
            it[endKey] = end.toSecondOfDay() / 60
        }
        onChanged()
    }

    override suspend fun setTeachingEndDate(date: LocalDate) {
        store.edit { it[teachingEndKey] = date.toEpochDay() }
        onChanged()
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        store.edit { it[hapticsKey] = enabled }
        onChanged()
    }

    override suspend fun setHealthConfig(config: HealthConfig) {
        store.edit { HealthPreferenceCodec.write(it, config) }
        onChanged()
    }

    override suspend fun setPlanningConfig(config: com.example.mydailyroutine.domain.planning.PlanningConfig) {
        store.edit { PlanningPreferenceCodec.write(it, config) }
        onChanged()
    }

    override suspend fun setAutomaticHealingEnabled(enabled: Boolean) {
        store.edit { it[healingKey] = enabled }
        onChanged()
    }

    override suspend fun setEntryDefaults(defaults: EntryDefaults) {
        store.edit { it[lessonKey] = defaults.lessonDurationMinutes; it[breakKey] = defaults.lessonBreakMinutes }
        onChanged()
    }
    override suspend fun setPeriodicBreak(config: PeriodicBreakConfig) {
        store.edit {
            it[periodicKey] = config.enabled
            it[periodicEveryKey] = config.everyMinutes
            it[periodicLenKey] = config.breakMinutes
        }
        onChanged()
    }
}
