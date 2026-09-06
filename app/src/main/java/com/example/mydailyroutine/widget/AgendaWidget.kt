package com.example.mydailyroutine.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionCallback
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionRunCallback
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.defaultWeight
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.mydailyroutine.MainActivity
import com.example.mydailyroutine.data.TimelineRepository
import com.example.mydailyroutine.data.local.RoutineDatabase
import com.example.mydailyroutine.data.seed.SchoolYearBounds
import com.example.mydailyroutine.domain.ResolvedTimelineItem
import com.example.mydailyroutine.domain.TimelineItemKind
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AgendaWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val date = LocalDate.now()
        val items = withContext(Dispatchers.IO) {
            val repository = TimelineRepository(RoutineDatabase.getInstance(context.applicationContext))
            repository.initialize()
            repository.getTimelineForDate(date).first()
        }
        val schoolYearEnd = SchoolYearBounds.forDate(date).end
        provideContent {
            AgendaWidgetContent(
                date = date,
                items = items,
                daysRemaining = ChronoUnit.DAYS.between(date, schoolYearEnd).coerceAtLeast(0),
            )
        }
    }
}

class AgendaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AgendaWidget()
}

class OpenFastAddAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(MainActivity.EXTRA_OPEN_FAST_ADD, true)
            },
        )
    }
}

@Composable
private fun AgendaWidgetContent(date: LocalDate, items: List<ResolvedTimelineItem>, daysRemaining: Long) {
    val now = LocalTime.now()
    val remaining = items.filter { it.kind == TimelineItemKind.ROUTINE && it.endTime >= now }.take(4)
    val active = remaining.firstOrNull { it.startTime <= now && it.endTime > now }
    val background = ColorProvider(Color(0xFFFFF8F1))
    val foreground = ColorProvider(Color(0xFF28231F))
    Column(
        modifier = GlanceModifier.fillMaxSize().background(background).padding(16.dp),
        verticalAlignment = Alignment.Vertical.Top,
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "${date.dayOfMonth} ${date.month.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = TextStyle(color = foreground, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "$daysRemaining days until school year ends",
                    style = TextStyle(color = ColorProvider(Color(0xFF6E625A)), fontSize = 11.sp),
                )
            }
            Text(
                text = "+ Add",
                modifier = GlanceModifier.clickable(actionRunCallback<OpenFastAddAction>()).padding(6.dp),
                style = TextStyle(color = ColorProvider(Color(0xFF7B4A16)), fontSize = 12.sp, fontWeight = FontWeight.Bold),
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        if (remaining.isEmpty()) {
            Text("Your day is clear", style = TextStyle(color = foreground, fontSize = 13.sp))
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                items(remaining) { item ->
                    val isActive = item.stableId == active?.stableId
                    Row(
                        modifier = GlanceModifier.fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .background(ColorProvider(if (isActive) Color(0xFFFFE0B2) else Color.Transparent)),
                        verticalAlignment = Alignment.Vertical.CenterVertically,
                    ) {
                        Text(
                            text = item.startTime.toString().take(5),
                            style = TextStyle(color = ColorProvider(Color(0xFF8A6A4B)), fontSize = 11.sp),
                        )
                        Spacer(GlanceModifier.width(8.dp))
                        Text(
                            text = item.title,
                            style = TextStyle(color = foreground, fontSize = 12.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal),
                        )
                    }
                }
            }
        }
    }
}
