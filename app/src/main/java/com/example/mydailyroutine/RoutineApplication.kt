package com.example.mydailyroutine

import android.app.Application
import android.content.Context
import com.example.mydailyroutine.core.platform.RoutineLocale
import com.example.mydailyroutine.core.platform.captureDeviceLanguage
import com.example.mydailyroutine.core.platform.StartupTrace
import com.example.mydailyroutine.core.platform.applyLocaleToProcessDefaults
import com.example.mydailyroutine.core.platform.withRoutineLocale
import com.example.mydailyroutine.core.preferences.readPersistedAppLanguage
import com.example.mydailyroutine.app.di.AppGraph

class RoutineApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun attachBaseContext(base: Context) {
        // Both inputs to the language rule, read before `super`, because `withRoutineLocale` on the
        // last line already needs the answer and `attachBaseContext` runs before `onCreate`.
        // The choice used to be read in `onCreate`, which left the application's own base context
        // resolved by the device rather than by the reader: every service the graph builds got a
        // correctly localised context, but the first `applicationContext.getString(...)` anyone
        // wrote would have spoken the wrong language for no visible reason.
        captureDeviceLanguage(base)
        RoutineLocale.userChoice = readPersistedAppLanguage(base)
        super.attachBaseContext(base.withRoutineLocale())
    }

    override fun onCreate() {
        super.onCreate()
        // The earliest moment in this process that can be measured against the first drawn day (N19).
        StartupTrace.markProcessStart()
        // The choice was mirrored in `attachBaseContext`, before the first string of this process
        // was formatted; the graph built below hands every service that renders user-visible text a
        // context resolved from that mirror, so the first notification, seed or widget already
        // speaks the language the reader picked.
        // The process default follows the same choice the resources make, so number and date
        // formatting cannot drift a language away from the labels around them.
        applyLocaleToProcessDefaults()
        graph = AppGraph(this)
        graph.requestRefresh()
    }
}
