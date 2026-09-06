package com.example.mydailyroutine

import android.app.Application
import com.example.mydailyroutine.di.AppGraph

class RoutineApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        graph.requestRefresh()
    }
}
