package com.example.mydailyroutine

import android.app.Application
import android.content.Context
import com.example.mydailyroutine.core.platform.RoutineLocale
import com.example.mydailyroutine.core.platform.StartupTrace
import com.example.mydailyroutine.core.platform.applyLocaleToProcessDefaults
import com.example.mydailyroutine.core.platform.withRoutineLocale
import com.example.mydailyroutine.core.preferences.readPersistedAppLanguage
import com.example.mydailyroutine.app.di.AppGraph

class RoutineApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.withRoutineLocale())
    }

    override fun onCreate() {
        super.onCreate()
        // The earliest moment in this process that can be measured against the first drawn day (N19).
        StartupTrace.markProcessStart()
        // The reader's explicit language choice, if any, before this process formats a single
        // string: the graph built below hands every service that renders user-visible text a
        // context resolved from this mirror, so the first notification, seed or widget already
        // speaks the language the reader picked.
        RoutineLocale.userChoice = readPersistedAppLanguage(this)
        // The process default follows the same choice the resources make, so number and date
        // formatting cannot drift a language away from the labels around them.
        applyLocaleToProcessDefaults()
        graph = AppGraph(this)
        graph.requestRefresh()
    }
}
