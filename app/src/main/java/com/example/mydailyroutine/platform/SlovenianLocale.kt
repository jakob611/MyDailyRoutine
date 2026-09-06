package com.example.mydailyroutine.platform

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

val Slovenian: Locale = Locale.forLanguageTag("sl")

fun Context.withSlovenianLocale(): Context {
    val configuration = Configuration(resources.configuration)
    configuration.setLocales(LocaleList(Slovenian))
    return createConfigurationContext(configuration)
}
