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
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import com.example.mydailyroutine.core.designsystem.glass.LocalGlassTilt
import com.example.mydailyroutine.core.designsystem.glass.rememberGlassTilt
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mydailyroutine.app.di.appGraph
import com.example.mydailyroutine.core.platform.withRoutineLocale
import com.example.mydailyroutine.features.settings.presentation.NotificationAccess
import com.example.mydailyroutine.core.designsystem.theme.MyDailyRoutineTheme
import com.example.mydailyroutine.core.presentation.TimelineAction
import com.example.mydailyroutine.domain.model.ScheduleValidation
import com.example.mydailyroutine.app.presentation.RoutineApp
import com.example.mydailyroutine.app.presentation.RoutineViewModel
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<RoutineViewModel> {
        viewModelFactory { initializer { RoutineViewModel(appGraph.repository, appGraph.preferences, createSavedStateHandle(), appGraph.exampleData, appGraph.planning, appGraph.execution, appGraph.patterns, appGraph.backup, appGraph.goals) } }
    }
    private var access by mutableStateOf(NotificationAccess(false, false))
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshAccess()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withRoutineLocale())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        // Edge-to-edge asks the system for a contrast scrim under the gesture bar on Android 10+,
        // and that scrim follows the *system* theme: with the phone in light mode it paints a pale
        // strip under an otherwise black app. The app draws its own legible backdrop, so refuse it.
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        enforceDarkSystemBars()
        if (savedInstanceState == null) consumeIntent(intent)
        refreshAccess()
        setContent {
            MyDailyRoutineTheme {
                CompositionLocalProvider(LocalGlassTilt provides rememberGlassTilt()) {
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
    }

    override fun onResume() {
        super.onResume()
        refreshAccess()
        enforceDarkSystemBars()
    }

    /**
     * Some devices reapply their own bar appearance when a window regains focus, which shows up as
     * the navigation bar flashing white between screens. Assert ours on every resume; the theme
     * also covers the windows this activity never sees directly (dialogs, sheets).
     */
    private fun enforceDarkSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND) {
            // A deadline shared from ManageBac or any other app lands as a pre-filled quick-add in the Tasks sheet.
            val shared = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!shared.isNullOrBlank()) {
                // The parser is pure JVM, so the localized placeholder travels in from here.
                val draft = com.example.mydailyroutine.core.platform.ShareTextParser.parse(shared, getString(R.string.shared_task_default_title))
                viewModel.onAction(TimelineAction.OpenSharedTask(draft.title, draft.dueEpochDay))
            }
            return
        }
        if (intent.action != ACTION_OPEN_DAY && intent.action != ACTION_FAST_ADD) return
        val date = intent.getStringExtra(EXTRA_DATE)?.let(ScheduleValidation::parseDate) ?: LocalDate.now()
        viewModel.onAction(TimelineAction.SelectDate(date, openDay = true))
        if (intent.action == ACTION_FAST_ADD) viewModel.onAction(TimelineAction.OpenAdd)
        when (intent.getStringExtra(EXTRA_NOTIFY_ACTION)) {
            "start" -> viewModel.onAction(TimelineAction.StartExecutionById(intent.getLongExtra(EXTRA_NOTIFY_ID, 0L), date))
            "actual" -> viewModel.onAction(TimelineAction.RequestActualById(intent.getLongExtra(EXTRA_NOTIFY_ID, 0L)))
        }
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

        const val EXTRA_NOTIFY_ACTION = "notify_action"
        const val EXTRA_NOTIFY_ID = "notify_block"

        fun notifyActionIntent(context: Context, kind: String, id: Long, date: LocalDate): Intent =
            Intent(context, MainActivity::class.java)
                .setAction(ACTION_OPEN_DAY)
                .putExtra(EXTRA_DATE, date.toString())
                .putExtra(EXTRA_NOTIFY_ACTION, kind)
                .putExtra(EXTRA_NOTIFY_ID, id)

        fun openDayIntent(context: Context, date: LocalDate): Intent = Intent(context, MainActivity::class.java)
            .setAction(ACTION_OPEN_DAY).setData(Uri.parse("mydailyroutine://day/$date"))
            .putExtra(EXTRA_DATE, date.toString())
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        fun fastAddIntent(context: Context): Intent = Intent(context, MainActivity::class.java)
            .setAction(ACTION_FAST_ADD).setData(Uri.parse("mydailyroutine://fast-add"))
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
