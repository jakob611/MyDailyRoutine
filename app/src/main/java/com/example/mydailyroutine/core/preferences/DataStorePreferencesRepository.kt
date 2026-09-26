package com.example.mydailyroutine.core.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mydailyroutine.domain.health.HealthConfig
import com.example.mydailyroutine.domain.health.PeriodicBreakConfig
import com.example.mydailyroutine.domain.model.AppLanguage
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
import kotlinx.coroutines.flow.onEach

private val Context.scheduleDataStore by preferencesDataStore(name = "schedule_preferences")

private val appLanguageKey = stringPreferencesKey("app_language")

private const val LocaleMirrorFile = "locale_mirror"
private const val LocaleMirrorLanguage = "app_language"
private const val LocaleMirrorAdopted = "adopted"

/**
 * A one-key `SharedPreferences` file that shadows the language choice.
 *
 * `DataStore` is the source of truth for every preference, including this one, and it is
 * deliberately asynchronous. `attachBaseContext` is deliberately synchronous: it must decide which
 * language the whole process speaks before anything can await a coroutine. Those two facts do not
 * meet, so the one value needed that early is also kept where a synchronous read is the supported
 * operation rather than a blocking workaround.
 *
 * Deliberately **not** `applicationContext`: inside `Application.attachBaseContext` the application
 * object is still being built and `getApplicationContext()` answers null. The context handed to
 * that method can open its own preferences, and the instance returned here holds no reference back
 * to it.
 */
private fun Context.localeMirror(): SharedPreferences =
    getSharedPreferences(LocaleMirrorFile, Context.MODE_PRIVATE)

private fun SharedPreferences.writeLanguage(language: String?) {
    if (getBoolean(LocaleMirrorAdopted, false) && getString(LocaleMirrorLanguage, null) == language) return
    edit().putBoolean(LocaleMirrorAdopted, true).putString(LocaleMirrorLanguage, language).apply()
}

/**
 * The reader's language choice, read synchronously at process start.
 *
 * One small file read on the cold-start critical path — a path an alarm or a widget can also wake.
 * It used to be a `runBlocking` that held the main thread while DataStore read and deserialised the
 * entire preference file for this single string.
 *
 * The mirror can only be behind the store if the process died between the two writes, or if a
 * restore brought back one file without the other; the preference flow rewrites it on its first
 * emission, so the drift lasts one launch and then heals itself.
 */
fun readPersistedAppLanguage(context: Context): String? =
    // Guarded because this is the first line of the process: anything thrown here is a launch
    // crash with no screen to report it on. Failing to read the mirror is not an error state, it
    // is the absence of a choice — which already has a defined answer, the device's language.
    runCatching { context.localeMirror().getString(LocaleMirrorLanguage, null) }
        .getOrNull()?.let(AppLanguage::normalize)

class DataStorePreferencesRepository(context: Context, private val onChanged: () -> Unit) : PreferencesRepository {
    private val store = context.applicationContext.scheduleDataStore
    private val localeMirror = context.localeMirror()
    private val muteKey = booleanPreferencesKey("mute_during_school")
    private val nameKey = stringPreferencesKey("user_name")
    private val onboardingKey = booleanPreferencesKey("onboarding_done")
    private val recoveryKey = booleanPreferencesKey("notify_recovery_breaks")
    private val startKey = intPreferencesKey("school_start_minute")
    private val endKey = intPreferencesKey("school_end_minute")
    private val teachingEndKey = longPreferencesKey("teaching_end_epoch_day")
    private val healingKey = booleanPreferencesKey("automatic_healing_enabled")
    private val hapticsKey = booleanPreferencesKey("haptics_enabled")
    private val soundKey = booleanPreferencesKey("sound_effects_enabled")
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
            userName = values[nameKey].orEmpty().take(40),
            // An install that already holds settings belongs to someone who was using the app before
            // the first-run flow existed: asking them to answer it now would be a regression, so a
            // missing flag means "already past it" as soon as the store has anything else in it.
            onboardingDone = values[onboardingKey] ?: values.asMap().isNotEmpty(),
            muteDuringSchoolHours = values[muteKey] ?: defaults.muteDuringSchoolHours,
            notifyRecovery = values[recoveryKey] ?: defaults.notifyRecovery,
            schoolStart = time(values[startKey], defaults.schoolStart),
            schoolEnd = time(values[endKey], defaults.schoolEnd),
            hapticsEnabled = values[hapticsKey] ?: true,
            soundEffectsEnabled = values[soundKey] ?: true,
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
            // A tag outside the two shipped translations reads as "no choice", never as a third
            // language: the store is trusted for the key, not for its grammar.
            appLanguage = AppLanguage.normalize(values[appLanguageKey]),
        )
    }.onEach {
        // The synchronous mirror is written by setAppLanguage, but this is the place that sees
        // every value the store ever holds — including one that arrived without going through this
        // process. Writing only on a real difference keeps it to a comparison per emission.
        localeMirror.writeLanguage(it.appLanguage)
    }.distinctUntilChanged()

    private fun time(minutes: Int?, fallback: LocalTime): LocalTime =
        minutes?.takeIf { it in 0..1439 }?.let { LocalTime.ofSecondOfDay(it * 60L) } ?: fallback

    override suspend fun setUserName(name: String) {
        store.edit { it[nameKey] = name.trim().take(40) }
    }

    override suspend fun completeOnboarding() {
        store.edit { it[onboardingKey] = true }
    }

    override suspend fun setMuteDuringSchoolHours(muted: Boolean) {
        store.edit { it[muteKey] = muted }
        onChanged()
    }

    override suspend fun setRecoveryNotifications(enabled: Boolean) {
        store.edit { it[recoveryKey] = enabled }
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

    override suspend fun setSoundEffectsEnabled(enabled: Boolean) {
        store.edit { it[soundKey] = enabled }
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

    override suspend fun setAppLanguage(language: String?) {
        val normalized = AppLanguage.normalize(language)
        store.edit {
            if (normalized == null) it.remove(appLanguageKey) else it[appLanguageKey] = normalized
        }
        // After the store, never before it: should the process die between the two writes, the next
        // start reads a mirror that is behind the truth rather than ahead of it — the interface
        // speaks the previous language and the settings sheet shows the previous choice, which is
        // one consistent state instead of two contradicting ones.
        localeMirror.writeLanguage(normalized)
        onChanged()
    }
}
