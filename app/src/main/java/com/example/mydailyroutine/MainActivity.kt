package com.example.mydailyroutine

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mydailyroutine.app.di.appGraph
import com.example.mydailyroutine.core.platform.withSlovenianLocale
import com.example.mydailyroutine.features.settings.presentation.NotificationAccess
import com.example.mydailyroutine.core.designsystem.theme.MyDailyRoutineTheme
import com.example.mydailyroutine.core.presentation.TimelineAction
import com.example.mydailyroutine.domain.model.ScheduleValidation
import com.example.mydailyroutine.app.presentation.RoutineApp
import com.example.mydailyroutine.app.presentation.RoutineViewModel
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<RoutineViewModel> {
        viewModelFactory { initializer { RoutineViewModel(appGraph.repository, appGraph.preferences, createSavedStateHandle(), appGraph.exampleData, appGraph.planning, appGraph.execution) } }
    }
    private var access by mutableStateOf(NotificationAccess(false, false))
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshAccess()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withSlovenianLocale())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK))
        if (savedInstanceState == null) consumeIntent(intent)
        refreshAccess()
        setContent {
            MyDailyRoutineTheme {
                RoutineApp(viewModel, access,
                    requestNotifications = {
                        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else openNotificationSettings()
                    },
                    requestExactAlarms = {
                        if (Build.VERSION.SDK_INT >= 31) openSettings(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
                    },
                    openNotificationSettings = ::openNotificationSettings,
                )
            }
        }
    }

    override fun onResume() { super.onResume(); refreshAccess() }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent) {
        if (intent.action != ACTION_OPEN_DAY && intent.action != ACTION_FAST_ADD) return
        val date = intent.getStringExtra(EXTRA_DATE)?.let(ScheduleValidation::parseDate) ?: LocalDate.now()
        viewModel.onAction(TimelineAction.SelectDate(date, openDay = true))
        if (intent.action == ACTION_FAST_ADD) viewModel.onAction(TimelineAction.OpenAdd)
    }

    private fun refreshAccess() {
        access = NotificationAccess(appGraph.notifier.canNotify(), appGraph.scheduler.canScheduleExactAlarms())
        appGraph.requestRefresh()
    }

    private fun openNotificationSettings() {
        val settings = if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        openSettings(settings)
    }

    private fun openSettings(intent: Intent) {
        try { startActivity(intent) }
        catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }

    companion object {
        const val ACTION_OPEN_DAY = "com.example.mydailyroutine.OPEN_DAY"
        const val ACTION_FAST_ADD = "com.example.mydailyroutine.FAST_ADD"
        const val EXTRA_DATE = "selected_date"

        fun openDayIntent(context: Context, date: LocalDate): Intent = Intent(context, MainActivity::class.java)
            .setAction(ACTION_OPEN_DAY).setData(Uri.parse("mydailyroutine://day/$date"))
            .putExtra(EXTRA_DATE, date.toString())
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        fun fastAddIntent(context: Context): Intent = Intent(context, MainActivity::class.java)
            .setAction(ACTION_FAST_ADD).setData(Uri.parse("mydailyroutine://fast-add"))
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
