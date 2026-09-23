package com.example.mydailyroutine

import android.app.Application
import android.content.Context
import java.util.Locale
import android.os.LocaleList
import com.example.mydailyroutine.core.platform.StartupTrace
import com.example.mydailyroutine.core.platform.uiLocale
import com.example.mydailyroutine.core.platform.withRoutineLocale
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
        // The process default follows the same choice the resources make, so number and date
        // formatting cannot drift a language away from the labels around them.
        val locale = uiLocale()
        Locale.setDefault(locale)
        LocaleList.setDefault(LocaleList(locale))
        graph = AppGraph(this)
        graph.requestRefresh()
    }
}
