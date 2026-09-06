package com.example.mydailyroutine

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.mydailyroutine.data.TimelineRepository
import com.example.mydailyroutine.data.local.RoutineDatabase
import com.example.mydailyroutine.notifications.ScheduleAlarmScheduler
import com.example.mydailyroutine.ui.RoutineApp
import com.example.mydailyroutine.ui.RoutineViewModel
import com.example.mydailyroutine.ui.RoutineViewModelFactory
import com.example.mydailyroutine.ui.theme.MyDailyRoutineTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var fastAddRequest by mutableIntStateOf(0)

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* The app continues to work offline when notifications are declined. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = TimelineRepository(RoutineDatabase.getInstance(applicationContext))
        val alarmScheduler = ScheduleAlarmScheduler(applicationContext, repository)
        val viewModel = ViewModelProvider(this, RoutineViewModelFactory(repository, alarmScheduler))[RoutineViewModel::class.java]
        if (intent?.getBooleanExtra(EXTRA_OPEN_FAST_ADD, false) == true) fastAddRequest++
        setContent {
            MyDailyRoutineTheme {
                RoutineApp(viewModel = viewModel, openFastAddRequest = fastAddRequest)
            }
        }

        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        lifecycleScope.launch(Dispatchers.IO) {
            repository.initialize()
            ScheduleAlarmScheduler(applicationContext, repository).scheduleUpcoming(daysAhead = 7)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_FAST_ADD, false)) fastAddRequest++
    }

    companion object {
        const val EXTRA_OPEN_FAST_ADD = "open_fast_add"
    }
}
