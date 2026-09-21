package com.example.mydailyroutine.core.platform

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

val Slovenian: Locale = Locale.forLanguageTag("sl")

/** The second complete translation. Both are listed in `res/xml/locales_config.xml`. */
val English: Locale = Locale.forLanguageTag("en")

/**
 * The language the interface speaks.
 *
 * The app ships two complete translations: Slovenian and English. Slovenian is the *default*
 * resource set (`values/`), so a phone in a language nobody translated falls back to Slovenian
 * rather than to half-translated English. A phone set to English gets English — including English
 * date patterns, which is why every screen asks [uiLocale] instead of hard-coding a language.
 */
fun uiLocaleFor(deviceLanguage: String): Locale =
    if (deviceLanguage == English.language) English else Slovenian

fun uiLocale(device: LocaleList = LocaleList.getDefault()): Locale = uiLocaleFor(device[0].language)

/**
 * The context every activity and the application itself runs on: the app's own chosen locale,
 * applied to the whole resource set, so `R.string` and `getDisplayName` agree on one language.
 */
fun Context.withRoutineLocale(): Context {
    val configuration = Configuration(resources.configuration)
    configuration.setLocales(LocaleList(uiLocale(configuration.locales)))
    return createConfigurationContext(configuration)
}
