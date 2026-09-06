package com.example.mydailyroutine

import android.app.Application
import android.content.Context
import java.util.Locale
import android.os.LocaleList
import com.example.mydailyroutine.platform.Slovenian
import com.example.mydailyroutine.platform.withSlovenianLocale
import com.example.mydailyroutine.di.AppGraph

class RoutineApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.withSlovenianLocale())
    }

    override fun onCreate() {
        super.onCreate()
        Locale.setDefault(Slovenian)
        LocaleList.setDefault(LocaleList(Slovenian))
        graph = AppGraph(this)
        graph.requestRefresh()
    }
}
