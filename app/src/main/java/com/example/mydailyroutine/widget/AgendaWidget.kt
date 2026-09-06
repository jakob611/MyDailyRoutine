package com.example.mydailyroutine.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.currentState
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.mydailyroutine.MainActivity
import com.example.mydailyroutine.di.appGraph
import com.example.mydailyroutine.di.AppGraph
import com.example.mydailyroutine.domain.calendar.SlovenianAcademicCalendar
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.SchedulePreferences
import com.example.mydailyroutine.domain.scheduling.OccurrenceTimes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch

private data class WidgetRow(val title: String, val time: String, val status: String, val active: Boolean)
private data class WidgetAgenda(
    val date: LocalDate, val countdown: String, val rows: List<WidgetRow>, val updatedAt: String,
    val error: Boolean = false, val loading: Boolean = false,
)
private data class WidgetSnapshot(
    val date: LocalDate,
    val items: List<ResolvedTimelineItem>,
    val preferences: SchedulePreferences,
    val error: Boolean = false,
    val loading: Boolean = false,
)

// Only a render invalidation token is stored in Glance state. Agenda facts live exclusively in Room.
private val renderRevision = longPreferencesKey("render_revision")
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

class AgendaWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(250.dp, 180.dp), DpSize(300.dp, 300.dp), DpSize(360.dp, 420.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val graph = context.appGraph
        val firstDate = LocalDate.now()
        val initial = agendaFlow(graph, firstDate).first()
        provideContent {
            GlanceTheme {
                val revision = currentState<Preferences>()[renderRevision] ?: 0L
                // update() does not restart an already-running provideGlance session. A revision
                // explicitly refreshes the clock/zone at a boundary, even if Room data is unchanged.
                val moment = remember(revision) { Instant.now() to ZoneId.systemDefault() }
                val date = moment.first.atZone(moment.second).toLocalDate()
                key(date) {
                    val updates = remember(date, graph, revision) { agendaFlow(graph, date) }
                    // Collect only within Glance's bounded composition session, never in a service.
                    val snapshot by updates.collectAsState(initial = if (initial.date == date) initial else
                        WidgetSnapshot(date, emptyList(), initial.preferences, loading = true))
                    val agenda = remember(snapshot, moment) { resolveAgenda(snapshot, moment.first, moment.second) }
                    AgendaContent(context, agenda)
                }
            }
        }
    }
}

private fun agendaFlow(graph: AppGraph, date: LocalDate): Flow<WidgetSnapshot> = combine(
    graph.repository.getTimelineForDate(date), graph.preferences.preferences,
) { items, preferences -> WidgetSnapshot(date, items, preferences) }.catch { error ->
    if (error is CancellationException) throw error
    emit(WidgetSnapshot(date, emptyList(), SchedulePreferences(), error = true))
}

private fun resolveAgenda(snapshot: WidgetSnapshot, now: Instant, zone: ZoneId): WidgetAgenda {
    val remaining = snapshot.items.filter { item ->
        !item.isCompleted && when (item) {
            is ResolvedTimelineItem.Block -> !item.isSuppressed && OccurrenceTimes.window(item, zone).end > now
            is ResolvedTimelineItem.Milestone -> true
        }
    }
    val nextKey = remaining.filterIsInstance<ResolvedTimelineItem.Block>()
        .filter { OccurrenceTimes.window(it, zone).start > now }.minByOrNull { OccurrenceTimes.window(it, zone).start }?.key
    val rows = remaining.map { item ->
        when (item) {
            is ResolvedTimelineItem.Block -> {
                val window = OccurrenceTimes.window(item, zone)
                val active = now >= window.start && now < window.end
                WidgetRow(item.title,
                    "${window.start.atZone(zone).format(timeFormat)}–${window.end.atZone(zone).format(timeFormat)}",
                    when { active -> "● NOW"; item.key == nextKey -> "UP NEXT"; else -> item.category.name.replace('_', ' ').lowercase() }, active)
            }
            is ResolvedTimelineItem.Milestone -> WidgetRow(item.title, item.dueTime?.format(timeFormat) ?: "All day", if (item.isExam) "EXAM" else "DEADLINE", false)
        }
    }
    val days = SlovenianAcademicCalendar.daysRemaining(snapshot.date, snapshot.preferences.teachingEndDate)
    return WidgetAgenda(snapshot.date,
        if (snapshot.loading) "Updating your day…" else if (days > 0) "$days days to school-year end" else "Teaching year complete",
        rows, now.atZone(zone).format(timeFormat), error = snapshot.error, loading = snapshot.loading)
}

/** Notify both existing Glance sessions and cold widgets; updating the revision is not an agenda cache. */
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
    Column(GlanceModifier.fillMaxSize().appWidgetBackground().background(GlanceTheme.colors.widgetBackground).cornerRadius(24.dp).padding(16.dp)) {
        Text(agenda.date.format(DateTimeFormatter.ofPattern("EEE, d MMM")), modifier = GlanceModifier.clickable(openDay),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp))
        Text(agenda.countdown, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
        Spacer(GlanceModifier.height(10.dp))
        LazyColumn(GlanceModifier.fillMaxWidth().defaultWeight()) {
            if (agenda.error || agenda.loading || agenda.rows.isEmpty()) item {
                Text(when {
                    agenda.error -> "Open the app to check your local schedule."
                    agenda.loading -> "Updating your agenda…"
                    else -> "Room to breathe. No remaining blocks today."
                },
                    modifier = GlanceModifier.padding(vertical = 10.dp).clickable(openDay),
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp))
            }
            // Bound RemoteViews size even if the user imports an unusually large daily agenda.
            items(agenda.rows.take(60)) { row ->
                Column(GlanceModifier.fillMaxWidth().padding(bottom = 6.dp).cornerRadius(12.dp)
                    .background(if (row.active) GlanceTheme.colors.primaryContainer else GlanceTheme.colors.surfaceVariant)
                    .clickable(openDay).padding(10.dp)) {
                    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(row.time, modifier = GlanceModifier.defaultWeight(), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp))
                        Text(row.status, style = TextStyle(color = GlanceTheme.colors.primary, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                    }
                    Text(row.title, maxLines = 2, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium))
                }
            }
            if (agenda.rows.size > 60) item {
                Text("Open app for ${agenda.rows.size - 60} more entries", modifier = GlanceModifier.clickable(openDay), style = TextStyle(color = GlanceTheme.colors.primary))
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("As of ${agenda.updatedAt}", modifier = GlanceModifier.defaultWeight(), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp))
            Button(text = "+ Add", onClick = actionStartActivity(MainActivity.fastAddIntent(context)))
        }
    }
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
