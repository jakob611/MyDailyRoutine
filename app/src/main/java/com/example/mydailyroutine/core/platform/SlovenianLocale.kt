package com.example.mydailyroutine.core.platform

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import java.util.Locale

val Slovenian: Locale = Locale.forLanguageTag("sl")

/** The second complete translation. Both are listed in `res/xml/locales_config.xml`. */
val English: Locale = Locale.forLanguageTag("en")

/**
 * The language the interface speaks.
 *
 * Two complete translations ship, and the resolution is the first rule that matches:
 *
 * 1. **The reader's explicit choice** ([RoutineLocale.userChoice]) — Slovenian or English,
 *    independent of what the device says. The settings sheet and the first onboarding screen
 *    write it; the process start reads it back before the first string is formatted.
 * 2. **The device's language.** A phone set to English gets English — including English date
 *    patterns, which is why every screen asks [uiLocale] instead of hard-coding a language.
 *    Everything else gets Slovenian, because Slovenian is the *default* resource set
 *    (`values/`): a phone in a language nobody translated falls back to a complete language
 *    rather than to a half-translation.
 */
fun uiLocaleFor(deviceLanguage: String, userChoice: String? = RoutineLocale.userChoice): Locale =
    when (userChoice) {
        Slovenian.language -> Slovenian
        English.language -> English
        else -> if (deviceLanguage == English.language) English else Slovenian
    }

/**
 * Captures the language Android resolved for this app, as rule 2 of [uiLocaleFor] means it.
 *
 * That is the per-app language from system settings when the reader set one there — the app ships
 * `res/xml/locales_config.xml`, so that picker offers both translations — and the device's own
 * language otherwise. Both arrive in the configuration the framework hands the app.
 *
 * Call from `attachBaseContext` and nowhere else: that is the one moment the app holds a
 * configuration it has not yet rewritten. [withRoutineLocale] is also used by the object graph and
 * by the widget, on contexts that already carry the app's own choice; capturing there would feed
 * the app's answer back in as the question.
 */
fun captureDeviceLanguage(base: Context) {
    RoutineLocale.deviceLanguage = base.resources.configuration.locales.firstLanguage()
}

private fun LocaleList.firstLanguage(): String? = takeIf { !it.isEmpty }?.get(0)?.language

/**
 * The device's language as far as this app is concerned.
 *
 * Deliberately not `LocaleList.getDefault()`: [applyLocaleToProcessDefaults] writes that list, so
 * reading it back would mean asking the app what the phone says. An English phone whose reader
 * picked Slovenian and then went back to "as on the device" would stay Slovenian until the process
 * died, because rule 2 would read the Slovenian this app had just written.
 *
 * The system resources are the fallback rather than the source: they are the device and nothing
 * else, so they cannot answer for a per-app language. They only run in a process that never saw
 * [captureDeviceLanguage] — no such process ships, but a missing language must degrade to the
 * device's, not to a crash.
 */
private val resolvedDeviceLanguage: String
    get() = RoutineLocale.deviceLanguage
        ?: Resources.getSystem().configuration.locales.firstLanguage()
        ?: Slovenian.language

fun uiLocale(): Locale = uiLocaleFor(resolvedDeviceLanguage)

/**
 * Re-applies the app's locale to the process-wide defaults (`Locale.setDefault` and
 * `LocaleList.setDefault`).
 *
 * [com.example.mydailyroutine.RoutineApplication] calls this once, at process start, after reading
 * the stored choice; it is called again when the reader changes the choice mid-process (the
 * `RestartForLocale` effect), so
 * the framework-facing defaults — anything that reads the process default instead of asking
 * [uiLocale] — keep agreeing with the labels around them.
 */
fun applyLocaleToProcessDefaults() {
    val locale = uiLocale()
    Locale.setDefault(locale)
    LocaleList.setDefault(LocaleList(locale))
}

/**
 * The context every activity and the application itself runs on: the app's own chosen locale,
 * applied to the whole resource set, so `R.string` and `getDisplayName` agree on one language.
 */
fun Context.withRoutineLocale(): Context {
    val configuration = Configuration(resources.configuration)
    configuration.setLocales(LocaleList(uiLocale()))
    return createConfigurationContext(configuration)
}

/**
 * The two inputs to [uiLocaleFor], held where any thread can read them without a context.
 *
 * This object exists because [Context.withRoutineLocale] runs inside `attachBaseContext` — before
 * the asynchronous preference flow can answer anything — and yet it must already know which
 * language to apply. So the choice travels in two places with one owner:
 *
 * * **Storage** is `DataStore`, the single source of truth (`setAppLanguage` in
 *   `core.preferences`). Because nothing can await a coroutine this early, the same value is also
 *   shadowed in a one-key `SharedPreferences` file, which is what `readPersistedAppLanguage` reads
 *   synchronously; `setAppLanguage` writes both, and the preference flow heals any drift.
 * * **This mirror** is what every locale resolution in the process reads: the process start loads
 *   it from storage before the first string is formatted, and the settings/onboarding action
 *   updates it the moment the reader taps, so the very restart that follows already speaks the
 *   new language.
 *
 * A value outside the two complete translations is normalized to `null` on the way in, so the
 * mirror can never point the interface at a language that does not ship.
 */
object RoutineLocale {
    @Volatile
    var userChoice: String? = null
        set(value) {
            field = value?.takeIf { it in setOf(Slovenian.language, English.language) }
        }

    /** Rule 2's input, written once per process by [captureDeviceLanguage]. Not normalized: a
     *  language the app does not translate is a valid answer here and falls back inside the rule. */
    @Volatile
    var deviceLanguage: String? = null
}
