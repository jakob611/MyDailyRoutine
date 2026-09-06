package com.example.mydailyroutine.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.domain.calendar.SlovenianAcademicCalendar
import com.example.mydailyroutine.domain.model.SchedulePreferences
import com.example.mydailyroutine.domain.model.ScheduleValidation
import com.example.mydailyroutine.domain.model.Subject
import com.example.mydailyroutine.ui.timeline.TimelineAction
import com.example.mydailyroutine.ui.timeline.clockLabel

@Immutable
data class NotificationAccess(val notificationsEnabled: Boolean, val exactAlarmsAllowed: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    preferences: SchedulePreferences, subjects: List<Subject>, busy: Boolean, access: NotificationAccess,
    onAction: (TimelineAction) -> Unit, onDismiss: () -> Unit,
    requestNotifications: () -> Unit, requestExactAlarms: () -> Unit, openNotificationSettings: () -> Unit,
) {
    var start by rememberSaveable(preferences.schoolStart) { mutableStateOf(preferences.schoolStart.clockLabel()) }
    var end by rememberSaveable(preferences.schoolEnd) { mutableStateOf(preferences.schoolEnd.clockLabel()) }
    var teachingEnd by rememberSaveable(preferences.teachingEndDate) { mutableStateOf(preferences.teachingEndDate.toString()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteSubjectId by rememberSaveable { mutableStateOf<Long?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Your rhythm", style = MaterialTheme.typography.headlineSmall)
            Text("Entirely offline. No account, network access, analytics, or cloud backup.", style = MaterialTheme.typography.bodyMedium)
            Text("Uninstalling the app or clearing its storage permanently removes your schedule.", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            Text("Reminders", style = MaterialTheme.typography.titleLarge)
            Text(if (access.notificationsEnabled) "Notifications are allowed by Android." else "Notifications are off. Your schedule still works normally.", style = MaterialTheme.typography.bodyMedium)
            if (!access.notificationsEnabled) FilledTonalButton(onClick = requestNotifications) { Text("Allow reminders") }
            Text(if (access.exactAlarmsAllowed) "Precise alarm access is available." else "Precise alarm access is off. Reminders use an approximate fallback and may arrive late.", style = MaterialTheme.typography.bodySmall)
            if (!access.exactAlarmsAllowed) FilledTonalButton(onClick = requestExactAlarms) { Text("Allow precise alarms") }
            TextButton(onClick = openNotificationSettings) { Text("System notification settings") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Quiet during school hours", style = MaterialTheme.typography.titleMedium)
                    Text("Silent, low-priority notifications. Your phone’s system DND is not changed.", style = MaterialTheme.typography.bodySmall)
                }
                Switch(preferences.muteDuringSchoolHours, { onAction(TimelineAction.SetMute(it)) }, enabled = !busy)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(start, { start = it; error = null }, label = { Text("From · HH:mm") }, singleLine = true, modifier = Modifier.weight(1f), enabled = !busy)
                OutlinedTextField(end, { end = it; error = null }, label = { Text("Until · HH:mm") }, singleLine = true, modifier = Modifier.weight(1f), enabled = !busy)
            }
            Text("This daily window includes weekends. An earlier end wraps past midnight. Android controls heads-up display; low-priority quiet reminders normally stay in the notification shade.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(enabled = !busy, onClick = {
                val from = ScheduleValidation.parseTime(start)
                val until = ScheduleValidation.parseTime(end)
                if (from == null || until == null || from == until) error = "Choose two different HH:mm times."
                else onAction(TimelineAction.SetSchoolWindow(from, until))
            }) { Text("Save quiet window") }
            HorizontalDivider()
            Text("School-year countdown", style = MaterialTheme.typography.titleLarge)
            Text("Bundled calendar: ${SlovenianAcademicCalendar.CYCLE_LABEL}. Other years are not guessed or downloaded.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(teachingEnd, { teachingEnd = it; error = null }, label = { Text("Teaching ends · YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy)
            Text("Default: 2027-06-24 for non-final years. National final-year teaching ends 2027-05-21; your IB school may use another date. Changing this countdown does not cancel recurring blocks.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(enabled = !busy, onClick = {
                val parsed = ScheduleValidation.parseDate(teachingEnd)
                if (parsed == null) error = "Enter a valid YYYY-MM-DD date."
                else onAction(TimelineAction.SetTeachingEnd(parsed))
            }) { Text("Save end date") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Saved subjects", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(enabled = !busy, onClick = { onAction(TimelineAction.EditSubject()) }) { Text("Add") }
            }
            if (subjects.isEmpty()) Text("Save your subjects for quick titles, colors, and durations when planning a block.", style = MaterialTheme.typography.bodyMedium)
            subjects.forEach { subject ->
                ListItem(headlineContent = { Text(subject.name) }, supportingContent = { Text("${subject.defaultDurationMinutes} min default") },
                    trailingContent = { TextButton(enabled = !busy, onClick = { deleteSubjectId = subject.id }) { Text("Delete") } },
                    modifier = Modifier.clickable(enabled = !busy) { onAction(TimelineAction.EditSubject(subject)) })
            }
            HorizontalDivider()
            Text("Healthy planning", style = MaterialTheme.typography.titleLarge)
            Text("Guidance uses transparent, deterministic rules. Twenty minutes of unallocated time or explicit recovery resets cognitive load; five minutes resets a deskwork span. An overlapping break does not count as rest. Completed work still counts. These are planning heuristics, not clinical limits.", style = MaterialTheme.typography.bodySmall)
            Text("Battery note: no polling service or foreground timer. Android can delay alarms in deep idle or on restrictive devices, even with precise alarm access. A force-stopped app cannot deliver reminders until you open it again.", style = MaterialTheme.typography.bodySmall)
        }
    }
    subjects.firstOrNull { it.id == deleteSubjectId }?.let { subject ->
        AlertDialog(onDismissRequest = { deleteSubjectId = null }, title = { Text("Delete ${subject.name}?") },
            text = { Text("Blocks and milestones are kept. Their link to this subject is removed, including its color and default duration.") },
            confirmButton = { TextButton(enabled = !busy, onClick = { onAction(TimelineAction.DeleteSubject(subject.id)); deleteSubjectId = null }) { Text("Delete subject") } },
            dismissButton = { TextButton(onClick = { deleteSubjectId = null }) { Text("Cancel") } })
    }
}
