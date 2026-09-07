package com.example.mydailyroutine.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.action.ActionParameters
import androidx.glance.unit.ColorProvider
import com.example.mydailyroutine.domain.planning.AgendaProjection
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.material3.ColorProviders
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.example.mydailyroutine.MainActivity
import com.example.mydailyroutine.R
import com.example.mydailyroutine.di.AppGraph
import com.example.mydailyroutine.di.appGraph
import com.example.mydailyroutine.domain.calendar.SlovenianAcademicCalendar
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.SchedulePreferences
import com.example.mydailyroutine.domain.scheduling.OccurrenceTimes
import com.example.mydailyroutine.platform.Slovenian
import com.example.mydailyroutine.platform.withSlovenianLocale
import com.example.mydailyroutine.ui.theme.OledColorScheme
import com.example.mydailyroutine.ui.theme.CategoryStyle
import com.example.mydailyroutine.ui.theme.categoryStyle
import com.example.mydailyroutine.ui.theme.RoutineColors
import com.example.mydailyroutine.ui.timeline.labelRes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*

private data class WidgetRow(val title: String, val time: String, val status: String, val active: Boolean, val next: Boolean, val style: CategoryStyle, val progress: Float? = null)
private data class WidgetAgenda(val date: LocalDate, val days: Long, val countdown: String, val rows: List<WidgetRow>, val updatedAt: String,
    val error: Boolean = false, val loading: Boolean = false, val reserveRemaining: Int = 0)
private data class WidgetSnapshot(val date: LocalDate, val items: List<ResolvedTimelineItem>, val preferences: SchedulePreferences,
    val error: Boolean = false, val loading: Boolean = false)

// A render token, not a second agenda. Room remains the only source of scheduled items.
private val renderRevision = longPreferencesKey("render_revision")
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Slovenian)
private val widgetColors = ColorProviders(light = OledColorScheme, dark = OledColorScheme)

class AgendaWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(250.dp, 180.dp), DpSize(300.dp, 300.dp), DpSize(360.dp, 420.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val localized = context.withSlovenianLocale()
        val graph = context.appGraph
        val initial = agendaFlow(graph, LocalDate.now()).first()
        provideContent {
            GlanceTheme(colors = widgetColors) {
                val revision = currentState<Preferences>()[renderRevision] ?: 0L
                val moment = remember(revision) { Instant.now() to ZoneId.systemDefault() }
                val date = moment.first.atZone(moment.second).toLocalDate()
                key(date) {
                    val updates = remember(date, graph, revision) { agendaFlow(graph, date) }
                    val snapshot by updates.collectAsState(initial = if (initial.date == date) initial else WidgetSnapshot(date, emptyList(), initial.preferences, loading = true))
                    val agenda = remember(snapshot, moment) { resolveAgenda(localized, snapshot, moment.first, moment.second) }
                    AgendaContent(localized, agenda)
                }
            }
        }
    }
}

private fun agendaFlow(graph: AppGraph, date: LocalDate): Flow<WidgetSnapshot> = combine(graph.repository.getTimelineForDate(date), graph.preferences.preferences) { items, preferences ->
    WidgetSnapshot(date, items, preferences)
}.catch { error ->
    if (error is CancellationException) throw error
    emit(WidgetSnapshot(date, emptyList(), SchedulePreferences(), error = true))
}

private fun resolveAgenda(context: Context, snapshot: WidgetSnapshot, now: Instant, zone: ZoneId): WidgetAgenda {
    val projection = AgendaProjection.from(snapshot.items, now, zone)
    val rows = (listOfNotNull(projection.active) + projection.upcoming).map { item ->
        val window = OccurrenceTimes.window(item, zone)
        val active = item.key == projection.active?.key
        WidgetRow(item.title, context.getString(R.string.time_range, window.start.atZone(zone).format(timeFormat), window.end.atZone(zone).format(timeFormat)),
            context.getString(if (active) R.string.now else R.string.up_next), active, !active,
            categoryStyle(item.category, item.subject?.colorHex), if (active) projection.progress else null)
    }
    val days = SlovenianAcademicCalendar.daysRemaining(snapshot.date, snapshot.preferences.teachingEndDate)
    val countdown = if (snapshot.loading) context.getString(R.string.widget_updating) else if (days > 0)
        context.resources.getQuantityString(R.plurals.days_to_teaching_end, days.toInt(), days) else context.getString(R.string.widget_complete)
    return WidgetAgenda(snapshot.date, days, countdown, rows, now.atZone(zone).format(timeFormat), snapshot.error, snapshot.loading, projection.reserveRemainingMinutes)
}

/** Updates both active Glance sessions and cold widgets; updates never depend on a periodic timer. */
suspend fun refreshAgendaWidgets(context: Context) {
    val widget = AgendaWidget()
    GlanceAppWidgetManager(context).getGlanceIds(AgendaWidget::class.java).forEach { id ->
        updateAppWidgetState(context, id) { preferences ->
            val previous = preferences[renderRevision] ?: 0L
            preferences[renderRevision] = if (previous == Long.MAX_VALUE) 0 else previous + 1
        }
        widget.update(context, id)
    }
}

@Composable
private fun AgendaContent(context: Context, agenda: WidgetAgenda) {
    val openDay = actionStartActivity(MainActivity.openDayIntent(context, agenda.date))
    val roomy = LocalSize.current.height >= 260.dp
    Column(GlanceModifier.fillMaxSize().appWidgetBackground().background(RoutineColors.Background).cornerRadius(16.dp).padding(16.dp)) {
        WidgetText(context, agenda.date.format(DateTimeFormatter.ofPattern("EEE, d. MMM", Slovenian)), 18f, bold = true, modifier = GlanceModifier.fillMaxWidth().clickable(openDay))
        if (roomy && agenda.days > 0) WidgetText(context, agenda.days.toString(), 36f, bold = true)
        WidgetText(context, agenda.countdown, 12f, RoutineColors.TextSecondary)
        WidgetText(context, context.getString(R.string.reserve_remaining, agenda.reserveRemaining), 12f, RoutineColors.Sage)
        Spacer(GlanceModifier.height(10.dp))
        LazyColumn(GlanceModifier.fillMaxWidth().defaultWeight()) {
            if (agenda.error || agenda.loading || agenda.rows.isEmpty()) item {
                WidgetText(context, context.getString(when { agenda.error -> R.string.widget_error; agenda.loading -> R.string.widget_updating; else -> R.string.widget_empty }),
                    14f, modifier = GlanceModifier.padding(vertical = 8.dp).clickable(openDay))
            }
            items(agenda.rows.take(60)) { row ->
                Column(GlanceModifier.fillMaxWidth().padding(bottom = 6.dp).cornerRadius(16.dp)
                    .background(if (row.active || row.next) row.style.container else RoutineColors.Surface1)
                    .clickable(openDay).padding(10.dp)) {
                    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        WidgetText(context, row.time, 12f, RoutineColors.TextSecondary, modifier = GlanceModifier.defaultWeight())
                        WidgetText(context, row.status, 11f, if (row.active || row.next) row.style.content else RoutineColors.TextMuted, bold = true)
                    }
                    WidgetText(context, row.title, 15f, bold = true)
                    row.progress?.let { progress ->
                        LinearProgressIndicator(progress = progress, modifier = GlanceModifier.fillMaxWidth().height(5.dp),
                            color = ColorProvider(row.style.accent), backgroundColor = ColorProvider(RoutineColors.Surface2))
                    }
                }
            }
            if (agenda.rows.size > 60) item {
                WidgetText(context, context.getString(R.string.widget_more, agenda.rows.size - 60), 12f, RoutineColors.Amber, modifier = GlanceModifier.clickable(openDay))
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            WidgetText(context, context.getString(R.string.widget_as_of, agenda.updatedAt), 10f, RoutineColors.TextMuted, modifier = GlanceModifier.defaultWeight().clickable(actionRunCallback<RefreshAgendaAction>()))
            Box(GlanceModifier.background(RoutineColors.Focus.container).cornerRadius(24.dp).clickable(actionStartActivity(MainActivity.fastAddIntent(context))).padding(10.dp)) {
                WidgetText(context, context.getString(R.string.widget_add), 12f, RoutineColors.Focus.content, bold = true)
            }
        }
    }
}

/** Glance TextStyle has no fontFeatureSettings. TextView interop guarantees bundled font + tnum on API 24+. */
@Composable
private fun WidgetText(context: Context, text: String, size: Float, color: Color = RoutineColors.TextPrimary,
    bold: Boolean = false, modifier: GlanceModifier = GlanceModifier) {
    val views = RemoteViews(context.packageName, if (bold) R.layout.widget_text_bold else R.layout.widget_text).apply {
        setTextViewText(R.id.widget_text, text)
        setTextColor(R.id.widget_text, color.toArgb())
        setTextViewTextSize(R.id.widget_text, TypedValue.COMPLEX_UNIT_SP, size)
    }
    AndroidRemoteViews(views, modifier.wrapContentHeight())
}

class AgendaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AgendaWidget()
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.appGraph.requestRefresh()
    }
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        context.appGraph.requestRefresh()
    }
}

/** User-requested refresh, not a polling worker. */
class RefreshAgendaAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        refreshAgendaWidgets(context)
    }
}
