package com.example.mydailyroutine.features.settings.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.selection.SelectionContainer
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.domain.routines.SleepSchedule
import com.example.mydailyroutine.features.routines.presentation.SleepSettings
import com.example.mydailyroutine.features.entry.presentation.EntryDefaultsSettings
import com.example.mydailyroutine.domain.calendar.SlovenianAcademicCalendar
import com.example.mydailyroutine.domain.health.HealthConfig
import com.example.mydailyroutine.domain.health.WarningType
import com.example.mydailyroutine.domain.model.SchedulePreferences
import com.example.mydailyroutine.domain.model.ScheduleValidation
import com.example.mydailyroutine.domain.model.Subject
import com.example.mydailyroutine.core.designsystem.theme.*
import com.example.mydailyroutine.core.presentation.TimelineAction
import com.example.mydailyroutine.core.presentation.clockLabel

@Immutable
data class NotificationAccess(val notificationsEnabled: Boolean, val exactAlarmsAllowed: Boolean)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    preferences: SchedulePreferences, subjects: List<Subject>, busy: Boolean, access: NotificationAccess, exampleLoaded: Boolean, sleep: SleepSchedule,
    exportJson: String? = null,
    onAction: (TimelineAction) -> Unit, onDismiss: () -> Unit,
    requestNotifications: () -> Unit, requestExactAlarms: () -> Unit, openNotificationSettings: () -> Unit,
) {
    var start by rememberSaveable(preferences.schoolStart) { mutableStateOf(preferences.schoolStart.clockLabel()) }
    var end by rememberSaveable(preferences.schoolEnd) { mutableStateOf(preferences.schoolEnd.clockLabel()) }
    var teachingEnd by rememberSaveable(preferences.teachingEndDate) { mutableStateOf(preferences.teachingEndDate.toString()) }
    var error by rememberSaveable { mutableStateOf<Int?>(null) }
    var deleteSubjectId by rememberSaveable { mutableStateOf<Long?>(null) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoutineShapes.Sheet, containerColor = RoutineColors.Surface1, tonalElevation = 0.dp) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.privacy_summary), color = RoutineColors.TextSecondary)
            Text(stringResource(R.string.privacy_delete_warning), style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextMuted)
            HorizontalDivider()
            SleepSettings(sleep,busy,onAction)
            HorizontalDivider()
            EntryDefaultsSettings(preferences.entryDefaults,busy) { onAction(TimelineAction.SaveEntryDefaults(it)) }
            HorizontalDivider()
            Text(stringResource(R.string.settings_reminders), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(if (access.notificationsEnabled) R.string.notifications_allowed else R.string.notifications_disabled))
            if (!access.notificationsEnabled) FilledTonalButton(onClick = requestNotifications) { Text(stringResource(R.string.allow_notifications)) }
            Text(stringResource(if (access.exactAlarmsAllowed) R.string.exact_allowed else R.string.exact_disabled), style = MaterialTheme.typography.bodySmall)
            if (!access.exactAlarmsAllowed) FilledTonalButton(onClick = requestExactAlarms) { Text(stringResource(R.string.allow_exact)) }
            TextButton(onClick = openNotificationSettings) { Text(stringResource(R.string.system_notification_settings)) }
            SettingSwitch(stringResource(R.string.quiet_title), stringResource(R.string.quiet_description), preferences.muteDuringSchoolHours, !busy) { onAction(TimelineAction.SetMute(it)) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(start, { start = it; error = null }, label = { Text(stringResource(R.string.quiet_from)) }, singleLine = true, modifier = Modifier.weight(1f), enabled = !busy)
                OutlinedTextField(end, { end = it; error = null }, label = { Text(stringResource(R.string.quiet_until)) }, singleLine = true, modifier = Modifier.weight(1f), enabled = !busy)
            }
            Text(stringResource(R.string.quiet_hint), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(enabled = !busy, onClick = {
                val from = ScheduleValidation.parseTime(start); val until = ScheduleValidation.parseTime(end)
                if (from == null || until == null || from == until) error = R.string.error_time_range
                else onAction(TimelineAction.SetSchoolWindow(from, until))
            }) { Text(stringResource(R.string.save_quiet)) }
            SettingSwitch(stringResource(R.string.execution_automatic), stringResource(R.string.execution_automatic_hint), preferences.automaticHealingEnabled, !busy) { onAction(TimelineAction.SetAutomaticHealing(it)) }
            SettingSwitch(stringResource(R.string.settings_haptics), stringResource(R.string.settings_haptics_hint), preferences.hapticsEnabled, !busy) { onAction(TimelineAction.SetHaptics(it)) }
            HorizontalDivider()
            Text(stringResource(R.string.settings_countdown), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.calendar_scope_hint), style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = preferences.teachingEndDate == SlovenianAcademicCalendar.teachingEnd, onClick = { onAction(TimelineAction.SetTeachingEnd(SlovenianAcademicCalendar.teachingEnd)) }, enabled = !busy,
                    label = { Text(stringResource(R.string.regular_year_preset)) })
                FilterChip(selected = preferences.teachingEndDate == SlovenianAcademicCalendar.finalYearTeachingEnd, onClick = { onAction(TimelineAction.SetTeachingEnd(SlovenianAcademicCalendar.finalYearTeachingEnd)) }, enabled = !busy,
                    label = { Text(stringResource(R.string.final_year_preset)) })
            }
            OutlinedTextField(teachingEnd, { teachingEnd = it; error = null }, label = { Text(stringResource(R.string.teaching_end_label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy)
            Text(stringResource(R.string.teaching_end_hint), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(enabled = !busy, onClick = {
                val date = ScheduleValidation.parseDate(teachingEnd)
                if (date == null) error = R.string.error_date else onAction(TimelineAction.SetTeachingEnd(date))
            }) { Text(stringResource(R.string.save_teaching_end)) }
            error?.let { Text(stringResource(it), color = RoutineColors.Crimson) }
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.saved_subjects), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(enabled = !busy, onClick = { onAction(TimelineAction.EditSubject()) }) { Text(stringResource(R.string.add)) }
            }
            Text(stringResource(R.string.subject_presets_hint), style = MaterialTheme.typography.bodySmall)
            if (subjects.isEmpty()) Text(stringResource(R.string.no_subjects_hint), style = MaterialTheme.typography.bodyMedium)
            subjects.forEach { subject ->
                ListItem(headlineContent = { Text(subject.name) }, supportingContent = { Text(stringResource(R.string.subject_default_duration, subject.defaultDurationMinutes)) },
                    trailingContent = { TextButton(enabled = !busy, onClick = { deleteSubjectId = subject.id }) { Text(stringResource(R.string.delete)) } },
                    modifier = Modifier.clickable(enabled = !busy) { onAction(TimelineAction.EditSubject(subject)) })
            }
            HorizontalDivider()
            Text(stringResource(R.string.settings_health), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.settings_health_body), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.health_explanation), style = MaterialTheme.typography.bodySmall, color = RoutineColors.Warning)
            OutlinedButton(onClick = { advanced = !advanced }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.advanced_settings)) }
            AnimatedVisibility(advanced, enter = fadeIn(tween(TransitionMillis)), exit = fadeOut(tween(TransitionMillis))) {
                AdvancedHealthSettings(preferences.health, busy) { onAction(TimelineAction.SetHealthConfig(it)) }
            PeriodicBreakSettings(preferences.periodicBreak, busy) { onAction(it) }
            }
            HorizontalDivider()
            PlanningSettings(preferences.planning, busy) { onAction(TimelineAction.SetPlanningConfig(it)) }
            HorizontalDivider()
            Text(stringResource(R.string.demo_heading), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.demo_description), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(enabled = !busy && !exampleLoaded, onClick = { onAction(TimelineAction.RequestDemo) }) {
                Text(stringResource(if (exampleLoaded) R.string.demo_loaded else R.string.load_example_data))
            }
            Text(stringResource(R.string.battery_note), style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextMuted)
            HorizontalDivider()
            Text(stringResource(R.string.backup_heading), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.backup_description), style = MaterialTheme.typography.bodySmall)
            Button(enabled = !busy, onClick = { onAction(TimelineAction.ExportSchedule) }) { Text(stringResource(R.string.backup_export)) }
            var importText by rememberSaveable { mutableStateOf("") }
            OutlinedTextField(importText, { importText = it }, label = { Text(stringResource(R.string.backup_import_hint)) }, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp), maxLines = 6)
            Button(enabled = !busy && importText.isNotBlank(), onClick = { onAction(TimelineAction.ImportSchedule(importText)) }) { Text(stringResource(R.string.backup_import)) }
        }
    }
    subjects.firstOrNull { it.id == deleteSubjectId }?.let { subject ->
        AlertDialog(onDismissRequest = { deleteSubjectId = null }, title = { Text(stringResource(R.string.delete_subject_title, subject.name)) },
            text = { Text(stringResource(R.string.delete_subject_body)) },
            confirmButton = { TextButton(enabled = !busy, onClick = { onAction(TimelineAction.DeleteSubject(subject.id)); deleteSubjectId = null }) { Text(stringResource(R.string.delete_subject)) } },
            dismissButton = { TextButton(onClick = { deleteSubjectId = null }) { Text(stringResource(R.string.cancel)) } })
    }
    var showExport by remember { mutableStateOf(false) }
    LaunchedEffect(exportJson) { showExport = exportJson != null }
    if (showExport && exportJson != null) {
        AlertDialog(
            onDismissRequest = { showExport = false },
            title = { Text(stringResource(R.string.backup_export)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.backup_export_hint), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    SelectionContainer { Text(exportJson, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
                    val clipboard = LocalClipboardManager.current
                    TextButton(onClick = { clipboard.setText(AnnotatedString(exportJson)) }) { Text(stringResource(R.string.backup_copy)) }
                }
            },
            confirmButton = { TextButton(onClick = { showExport = false }) { Text(stringResource(R.string.close)) } },
        )
    }
}

@Composable
private fun SettingSwitch(title: String, description: String, value: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(description, style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextSecondary) }
        Switch(value, onChange, enabled = enabled)
    }
}

@Composable
private fun AdvancedHealthSettings(config: HealthConfig, busy: Boolean, onSave: (HealthConfig) -> Unit) {
    var focus by rememberSaveable(config) { mutableStateOf(config.focusLimitMinutes.toString()) }
    var cognitive by rememberSaveable(config) { mutableStateOf(config.cognitiveLimitMinutes.toString()) }
    var transition by rememberSaveable(config) { mutableStateOf(config.transitionMinutes.toString()) }
    var daily by rememberSaveable(config) { mutableStateOf(config.dailyFocusLimitMinutes.toString()) }
    var sedentary by rememberSaveable(config) { mutableStateOf(config.sedentaryLimitMinutes.toString()) }
    var gapMin by rememberSaveable(config) { mutableStateOf(config.fragmentedMinMinutes.toString()) }
    var gapMax by rememberSaveable(config) { mutableStateOf(config.fragmentedMaxMinutes.toString()) }
    var flags by rememberSaveable(config) { mutableIntStateOf(WarningType.entries.fold(0) { mask, type -> if (config.isEnabled(type)) mask or (1 shl type.ordinal) else mask }) }
    var invalid by rememberSaveable { mutableStateOf(false) }
    fun enabled(type: WarningType) = flags and (1 shl type.ordinal) != 0
    fun toggle(type: WarningType, value: Boolean) { flags = if (value) flags or (1 shl type.ordinal) else flags and (1 shl type.ordinal).inv() }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.advanced_hint), style = MaterialTheme.typography.bodySmall)
        RuleField(stringResource(R.string.threshold_focus), focus, { focus = it }, 1..720, enabled(WarningType.CONCENTRATION_LIMIT), busy) { toggle(WarningType.CONCENTRATION_LIMIT, it) }
        RuleField(stringResource(R.string.threshold_cognitive), cognitive, { cognitive = it }, 30..720, enabled(WarningType.HIGH_COGNITIVE_LOAD), busy) { toggle(WarningType.HIGH_COGNITIVE_LOAD, it) }
        RuleField(stringResource(R.string.threshold_transition), transition, { transition = it }, 0..240, enabled(WarningType.INSUFFICIENT_TRANSITION), busy) { toggle(WarningType.INSUFFICIENT_TRANSITION, it) }
        RuleField(stringResource(R.string.threshold_daily), daily, { daily = it }, 30..1440, enabled(WarningType.BURNOUT_RISK), busy) { toggle(WarningType.BURNOUT_RISK, it) }
        RuleField(stringResource(R.string.threshold_sedentary), sedentary, { sedentary = it }, 15..720, enabled(WarningType.PHYSICAL_RESET), busy) { toggle(WarningType.PHYSICAL_RESET, it) }
        SettingSwitch(stringResource(R.string.threshold_fragmented), stringResource(R.string.rule_enabled), enabled(WarningType.FRAGMENTED_TIME), !busy) { toggle(WarningType.FRAGMENTED_TIME, it) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MinuteField(gapMin, { gapMin = it }, stringResource(R.string.threshold_min), 1..1439, !busy && enabled(WarningType.FRAGMENTED_TIME), Modifier.weight(1f))
            MinuteField(gapMax, { gapMax = it }, stringResource(R.string.threshold_max), 1..1439, !busy && enabled(WarningType.FRAGMENTED_TIME), Modifier.weight(1f))
        }
        if (invalid) Text(stringResource(R.string.error_thresholds), color = RoutineColors.Crimson)
        Button(enabled = !busy, onClick = {
            val parsed = runCatching { HealthConfig(
                focus.toInt(), cognitive.toInt(), transition.toInt(), daily.toInt(), sedentary.toInt(), gapMin.toInt(), gapMax.toInt(),
                enabled(WarningType.CONCENTRATION_LIMIT), enabled(WarningType.HIGH_COGNITIVE_LOAD), enabled(WarningType.INSUFFICIENT_TRANSITION),
                enabled(WarningType.BURNOUT_RISK), enabled(WarningType.PHYSICAL_RESET), enabled(WarningType.FRAGMENTED_TIME),
            ) }.getOrNull()
            invalid = parsed == null
            parsed?.let(onSave)
        }) { Text(stringResource(R.string.save_thresholds)) }
        TextButton(enabled = !busy, onClick = { invalid = false; onSave(HealthConfig()) }) { Text(stringResource(R.string.reset_thresholds)) }
    }
}

@Composable
private fun RuleField(title: String, value: String, onValue: (String) -> Unit, range: IntRange, enabled: Boolean, busy: Boolean, onEnabled: (Boolean) -> Unit) {
    Column {
        SettingSwitch(title, stringResource(R.string.rule_enabled), enabled, !busy, onEnabled)
        MinuteField(value, onValue, stringResource(R.string.threshold_minutes), range, enabled && !busy, Modifier.fillMaxWidth())
    }
}

@Composable
private fun MinuteField(value: String, onValue: (String) -> Unit, label: String, range: IntRange, enabled: Boolean, modifier: Modifier) {
    OutlinedTextField(value, { onValue(it.filter(Char::isDigit).take(4)) }, label = { Text(label) },
        supportingText = { Text(stringResource(R.string.threshold_range, range.first, range.last)) },
        singleLine = true, enabled = enabled, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = modifier)
}

@Composable
private fun PeriodicBreakSettings(
    config: PeriodicBreakConfig,
    busy: Boolean,
    onAction: (TimelineAction) -> Unit,
) {
    var enabled by rememberSaveable(config) { mutableStateOf(config.enabled) }
    var every by rememberSaveable(config) { mutableStateOf(config.everyMinutes.toString()) }
    var len by rememberSaveable(config) { mutableStateOf(config.breakMinutes.toString()) }
    fun push() = onAction(TimelineAction.SetPeriodicBreak(PeriodicBreakConfig(enabled, every.toIntOrNull() ?: 60, len.toIntOrNull() ?: 5)))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.periodic_break_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.periodic_break_description), style = MaterialTheme.typography.bodySmall)
        }
        Switch(enabled, { enabled = it; push() }, enabled = !busy)
    }
    AnimatedVisibility(enabled) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(every, { every = it.filter(Char::isDigit).take(3); push() }, label = { Text(stringResource(R.string.periodic_break_every)) }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(len, { len = it.filter(Char::isDigit).take(2); push() }, label = { Text(stringResource(R.string.periodic_break_len)) }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            Text(stringResource(R.string.periodic_break_hint), style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextMuted)
        }
    }
}
