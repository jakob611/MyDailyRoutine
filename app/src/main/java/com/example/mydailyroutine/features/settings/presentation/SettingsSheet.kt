package com.example.mydailyroutine.features.settings.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.core.designsystem.components.RoutineSwitch
import com.example.mydailyroutine.core.designsystem.components.RoutineTimeField
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.components.CategoryTabs
import com.example.mydailyroutine.core.designsystem.components.RoutineLabel
import com.example.mydailyroutine.core.designsystem.components.RoutineSheetListScaffold
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.components.SheetSecondaryButton
import com.example.mydailyroutine.core.designsystem.components.SettingRow
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.example.mydailyroutine.core.presentation.TimelineAction
import com.example.mydailyroutine.core.presentation.clockLabel
import com.example.mydailyroutine.domain.calendar.SlovenianAcademicCalendar
import com.example.mydailyroutine.domain.health.HealthConfig
import com.example.mydailyroutine.domain.health.PeriodicBreakConfig
import com.example.mydailyroutine.domain.health.WarningType
import com.example.mydailyroutine.domain.model.SchedulePreferences
import com.example.mydailyroutine.domain.model.ScheduleValidation
import com.example.mydailyroutine.domain.model.Subject
import com.example.mydailyroutine.domain.routines.SleepSchedule
import com.example.mydailyroutine.features.entry.presentation.EntryDefaultsSettings
import com.example.mydailyroutine.features.routines.presentation.SleepSettings
import java.time.LocalDate
import java.time.LocalTime

@Immutable
data class NotificationAccess(val notificationsEnabled: Boolean, val exactAlarmsAllowed: Boolean)

/**
 * Settings are split into five short tabs instead of one 40-item scroll: rhythm, reminders, plan,
 * rules and data. Each tab is a handful of fields with exactly one save action.
 */
internal enum class SettingsTab { RHYTHM, REMINDERS, PLAN, RULES, DATA }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    preferences: SchedulePreferences,
    subjects: List<Subject>,
    busy: Boolean,
    access: NotificationAccess,
    exampleLoaded: Boolean,
    sleep: SleepSchedule,
    exportJson: String? = null,
    onAction: (TimelineAction) -> Unit,
    onDismiss: () -> Unit,
    requestNotifications: () -> Unit,
    requestExactAlarms: () -> Unit,
    openNotificationSettings: () -> Unit,
    sheetState: SheetState,
) {
    var tab by rememberSaveable { mutableStateOf(SettingsTab.RHYTHM) }
    var start by rememberSaveable(preferences.schoolStart) { mutableStateOf(preferences.schoolStart.clockLabel()) }
    var end by rememberSaveable(preferences.schoolEnd) { mutableStateOf(preferences.schoolEnd.clockLabel()) }
    var teachingEnd by rememberSaveable(preferences.teachingEndDate) { mutableStateOf(preferences.teachingEndDate.toString()) }
    var quietError by rememberSaveable { mutableStateOf(false) }
    var teachingError by rememberSaveable { mutableStateOf(false) }
    var importText by rememberSaveable { mutableStateOf("") }
    val haptics = LocalRoutineHaptics.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoutineShapes.Sheet,
        containerColor = RoutineColors.SheetSurface,
        tonalElevation = 0.dp,
    ) {
        RoutineSheetListScaffold(
            title = stringResource(R.string.settings_title),
            subtitle = stringResource(R.string.privacy_summary),
            closeLabel = stringResource(R.string.close),
            onClose = onDismiss,
            modifier = Modifier.testTag("settings-sheet"),
        ) {
            item(key = "settings-tabs") {
                CategoryTabs(
                    entries = SettingsTab.entries.toList(),
                    selected = tab,
                    label = { entry -> settingsTabLabel(entry) },
                    onSelect = { entry -> haptics.selection(); tab = entry },
                    tagPrefix = "settings-tab",
                    enabled = !busy,
                )
            }
            when (tab) {
                SettingsTab.RHYTHM -> rhythmTab(sleep, preferences, busy, onAction)
                SettingsTab.REMINDERS -> remindersTab(
                    preferences = preferences,
                    access = access,
                    busy = busy,
                    start = start,
                    end = end,
                    error = quietError,
                    onStart = { start = it; quietError = false },
                    onEnd = { end = it; quietError = false },
                    onError = { quietError = it },
                    onSaveWindow = { from, until -> onAction(TimelineAction.SetSchoolWindow(from, until)) },
                    requestNotifications = requestNotifications,
                    requestExactAlarms = requestExactAlarms,
                    openNotificationSettings = openNotificationSettings,
                    onAction = onAction,
                )
                SettingsTab.PLAN -> planTab(
                    preferences = preferences,
                    busy = busy,
                    teachingEnd = teachingEnd,
                    onTeachingEnd = { teachingEnd = it; teachingError = false },
                    onError = { teachingError = it },
                    onSaveTeachingEnd = { date -> onAction(TimelineAction.SetTeachingEnd(date)) },
                    onAction = onAction,
                )
                SettingsTab.RULES -> rulesTab(preferences, busy, onAction)
                SettingsTab.DATA -> dataTab(
                    subjects = subjects,
                    busy = busy,
                    exampleLoaded = exampleLoaded,
                    importText = importText,
                    onImportText = { importText = it },
                    onAction = onAction,
                )
            }
        }
    }
    var showExport by remember { mutableStateOf(false) }
    LaunchedEffect(exportJson) { showExport = exportJson != null }
    if (showExport && exportJson != null) {
        AlertDialog(
            onDismissRequest = { showExport = false },
            title = { RoutineText(stringResource(R.string.backup_export), style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                    RoutineText(stringResource(R.string.backup_export_hint), style = MaterialTheme.typography.bodySmall,
                        color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
                    SelectionContainer {
                        RoutineText(exportJson, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            maxLines = 12)
                    }
                    val clipboard = LocalClipboardManager.current
                    TextButton(onClick = { clipboard.setText(AnnotatedString(exportJson)) }) {
                        RoutineLabel(stringResource(R.string.backup_copy), style = MaterialTheme.typography.labelLarge)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExport = false }) {
                    RoutineLabel(stringResource(R.string.close), style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
}

@Composable
private fun settingsTabLabel(tab: SettingsTab): String = when (tab) {
    SettingsTab.RHYTHM -> stringResource(R.string.settings_tab_rhythm)
    SettingsTab.REMINDERS -> stringResource(R.string.settings_tab_reminders)
    SettingsTab.PLAN -> stringResource(R.string.settings_tab_plan)
    SettingsTab.RULES -> stringResource(R.string.settings_tab_rules)
    SettingsTab.DATA -> stringResource(R.string.settings_tab_data)
}

private fun LazyListScope.rhythmTab(
    sleep: SleepSchedule,
    preferences: SchedulePreferences,
    busy: Boolean,
    onAction: (TimelineAction) -> Unit,
) {
    item(key = "settings-privacy") {
        RoutineText(stringResource(R.string.privacy_delete_warning), style = MaterialTheme.typography.bodySmall,
            color = RoutineColors.TextMuted, maxLines = RoutineTextDefaults.Paragraph)
    }
    item(key = "settings-sleep") { SleepSettings(sleep, busy, onAction) }
    item(key = "settings-entry-defaults") {
        EntryDefaultsSettings(preferences.entryDefaults, busy) { onAction(TimelineAction.SaveEntryDefaults(it)) }
    }
    item(key = "settings-feedback") {
        Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
            SettingRow(
                title = stringResource(R.string.settings_haptics),
                description = stringResource(R.string.settings_haptics_hint),
                control = {
                    RoutineSwitch(preferences.hapticsEnabled, { onAction(TimelineAction.SetHaptics(it)) }, enabled = !busy)
                },
            )
            SettingRow(
                title = stringResource(R.string.execution_automatic),
                description = stringResource(R.string.execution_automatic_hint),
                control = {
                    RoutineSwitch(preferences.automaticHealingEnabled, { onAction(TimelineAction.SetAutomaticHealing(it)) },
                        enabled = !busy)
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.remindersTab(
    preferences: SchedulePreferences,
    access: NotificationAccess,
    busy: Boolean,
    start: String,
    end: String,
    error: Boolean,
    onStart: (String) -> Unit,
    onEnd: (String) -> Unit,
    onError: (Boolean) -> Unit,
    onSaveWindow: (LocalTime, LocalTime) -> Unit,
    requestNotifications: () -> Unit,
    requestExactAlarms: () -> Unit,
    openNotificationSettings: () -> Unit,
    onAction: (TimelineAction) -> Unit,
) {
    item(key = "settings-permissions") {
        Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
            RoutineText(
                text = stringResource(if (access.notificationsEnabled) R.string.notifications_allowed else R.string.notifications_disabled),
                maxLines = RoutineTextDefaults.Paragraph,
            )
            if (!access.notificationsEnabled) {
                SheetSecondaryButton(stringResource(R.string.allow_notifications), requestNotifications)
            }
            RoutineText(
                text = stringResource(if (access.exactAlarmsAllowed) R.string.exact_allowed else R.string.exact_disabled),
                style = MaterialTheme.typography.bodySmall,
                color = RoutineColors.TextSecondary,
                maxLines = RoutineTextDefaults.Paragraph,
            )
            if (!access.exactAlarmsAllowed) {
                SheetSecondaryButton(stringResource(R.string.allow_exact), requestExactAlarms)
            }
            TextButton(onClick = openNotificationSettings) {
                RoutineLabel(stringResource(R.string.system_notification_settings),
                    style = MaterialTheme.typography.labelLarge)
            }
        }
    }
    item(key = "settings-recovery-alerts") {
        SettingRow(
            title = stringResource(R.string.recovery_alerts_title),
            description = stringResource(R.string.recovery_alerts_description),
            control = {
                RoutineSwitch(preferences.notifyRecovery, { onAction(TimelineAction.SetRecoveryNotifications(it)) }, enabled = !busy)
            },
        )
    }
    item(key = "settings-quiet") {
        Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
            SettingRow(
                title = stringResource(R.string.quiet_title),
                description = stringResource(R.string.quiet_description),
                control = {
                    RoutineSwitch(preferences.muteDuringSchoolHours, { onAction(TimelineAction.SetMute(it)) }, enabled = !busy)
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                RoutineTimeField(
                    value = start,
                    onPick = { v ->
                        onStart(v)
                        val from = ScheduleValidation.parseTime(v)
                        val until = ScheduleValidation.parseTime(end)
                        if (from == null || until == null || from == until) onError(true)
                        else { onError(false); onSaveWindow(from, until) }
                    },
                    label = stringResource(R.string.quiet_from),
                    enabled = !busy,
                    supporting = if (error) stringResource(R.string.error_time_range) else null,
                    modifier = Modifier.weight(1f),
                    wheelTag = "quiet-start",
                )
                RoutineTimeField(
                    value = end,
                    onPick = { v ->
                        onEnd(v)
                        val from = ScheduleValidation.parseTime(start)
                        val until = ScheduleValidation.parseTime(v)
                        if (from == null || until == null || from == until) onError(true)
                        else { onError(false); onSaveWindow(from, until) }
                    },
                    label = stringResource(R.string.quiet_until),
                    enabled = !busy,
                    supporting = if (error) stringResource(R.string.error_time_range) else null,
                    modifier = Modifier.weight(1f),
                    wheelTag = "quiet-end",
                )
            }
            RoutineText(stringResource(R.string.quiet_hint), style = MaterialTheme.typography.bodySmall,
                color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.planTab(
    preferences: SchedulePreferences,
    busy: Boolean,
    teachingEnd: String,
    onTeachingEnd: (String) -> Unit,
    onError: (Boolean) -> Unit,
    onSaveTeachingEnd: (LocalDate) -> Unit,
    onAction: (TimelineAction) -> Unit,
) {
    item(key = "settings-teaching-end") {
        Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
            RoutineText(stringResource(R.string.settings_countdown), style = MaterialTheme.typography.titleLarge,
                maxLines = RoutineTextDefaults.Body)
            RoutineText(stringResource(R.string.calendar_scope_hint), style = MaterialTheme.typography.bodySmall,
                color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
            ) {
                FilterChip(
                    selected = preferences.teachingEndDate == SlovenianAcademicCalendar.teachingEnd,
                    onClick = { onAction(TimelineAction.SetTeachingEnd(SlovenianAcademicCalendar.teachingEnd)) },
                    enabled = !busy,
                    shape = RoutineShapes.Chip,
                    label = { RoutineLabel(stringResource(R.string.regular_year_preset)) },
                )
                FilterChip(
                    selected = preferences.teachingEndDate == SlovenianAcademicCalendar.finalYearTeachingEnd,
                    onClick = { onAction(TimelineAction.SetTeachingEnd(SlovenianAcademicCalendar.finalYearTeachingEnd)) },
                    enabled = !busy,
                    shape = RoutineShapes.Chip,
                    label = { RoutineLabel(stringResource(R.string.final_year_preset)) },
                )
            }
            OutlinedTextField(
                value = teachingEnd,
                onValueChange = onTeachingEnd,
                label = { RoutineText(stringResource(R.string.teaching_end_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !busy,
            )
            RoutineText(stringResource(R.string.teaching_end_hint), style = MaterialTheme.typography.bodySmall,
                color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
            SheetSecondaryButton(
                label = stringResource(R.string.save_teaching_end),
                enabled = !busy,
                onClick = {
                    val date = ScheduleValidation.parseDate(teachingEnd)
                    if (date == null) onError(true) else {
                        onError(false)
                        onSaveTeachingEnd(date)
                    }
                },
            )
        }
    }
    item(key = "settings-planning") {
        PlanningSettings(preferences.planning, busy) { onAction(TimelineAction.SetPlanningConfig(it)) }
    }
}

private fun LazyListScope.rulesTab(
    preferences: SchedulePreferences,
    busy: Boolean,
    onAction: (TimelineAction) -> Unit,
) {
    item(key = "settings-health-intro") {
        Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
            RoutineText(stringResource(R.string.settings_health_body), style = MaterialTheme.typography.bodySmall,
                color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
            RoutineText(stringResource(R.string.health_explanation), style = MaterialTheme.typography.bodySmall,
                color = RoutineColors.Warning, maxLines = RoutineTextDefaults.Paragraph)
        }
    }
    item(key = "settings-health-rules") {
        AdvancedHealthSettings(preferences.health, busy) { onAction(TimelineAction.SetHealthConfig(it)) }
    }
    item(key = "settings-periodic") {
        PeriodicBreakSettings(preferences.periodicBreak, busy) { onAction(it) }
    }
}

private fun LazyListScope.dataTab(
    subjects: List<Subject>,
    busy: Boolean,
    exampleLoaded: Boolean,
    importText: String,
    onImportText: (String) -> Unit,
    onAction: (TimelineAction) -> Unit,
) {
    item(key = "settings-subjects-header") {
        SettingRow(
            title = stringResource(R.string.saved_subjects),
            description = stringResource(R.string.subject_presets_hint),
            control = {
                TextButton(enabled = !busy, onClick = { onAction(TimelineAction.EditSubject()) }) {
                    RoutineLabel(stringResource(R.string.add), style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
    if (subjects.isEmpty()) {
        item(key = "settings-subjects-empty") {
            RoutineText(stringResource(R.string.no_subjects_hint), style = MaterialTheme.typography.bodyMedium,
                color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
        }
    } else {
        item(key = "settings-subjects-hint") {
            RoutineText(stringResource(R.string.subject_edit_hint), style = MaterialTheme.typography.bodySmall,
                color = RoutineColors.TextMuted, maxLines = RoutineTextDefaults.Paragraph)
        }
        items(subjects, key = { "subject:${it.id}" }) { subject ->
            OutlinedCard(
                shape = RoutineShapes.Card,
                border = BorderStroke(1.dp, RoutineColors.Border),
                modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) { onAction(TimelineAction.EditSubject(subject)) },
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(RoutineSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
                        RoutineText(subject.name, style = MaterialTheme.typography.titleMedium,
                            maxLines = RoutineTextDefaults.Body)
                        RoutineLabel(
                            stringResource(R.string.subject_default_duration, subject.defaultDurationMinutes),
                            style = MaterialTheme.typography.labelSmall,
                            color = RoutineColors.TextSecondary,
                        )
                    }
                    Icon(Icons.Outlined.Edit, null, Modifier.size(20.dp), tint = RoutineColors.TextMuted)
                }
            }
        }
    }
    item(key = "settings-import") {
        Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
            RoutineText(stringResource(R.string.timetable_import_title), style = MaterialTheme.typography.titleLarge,
                maxLines = RoutineTextDefaults.Body)
            RoutineText(stringResource(R.string.timetable_import_settings_hint), style = MaterialTheme.typography.bodySmall,
                color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
            SheetSecondaryButton(stringResource(R.string.timetable_import_open),
                { onAction(TimelineAction.ShowTimetableImport) }, enabled = !busy)
        }
    }
    item(key = "settings-demo") {
        Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
            RoutineText(stringResource(R.string.demo_heading), style = MaterialTheme.typography.titleLarge,
                maxLines = RoutineTextDefaults.Body)
            RoutineText(stringResource(R.string.demo_description), style = MaterialTheme.typography.bodySmall,
                color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
            SheetSecondaryButton(
                label = stringResource(if (exampleLoaded) R.string.demo_loaded else R.string.load_example_data),
                onClick = { onAction(TimelineAction.RequestDemo) },
                enabled = !busy && !exampleLoaded,
            )
            RoutineText(stringResource(R.string.battery_note), style = MaterialTheme.typography.bodySmall,
                color = RoutineColors.TextMuted, maxLines = RoutineTextDefaults.Paragraph)
        }
    }
    item(key = "settings-backup") {
        Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
            RoutineText(stringResource(R.string.backup_heading), style = MaterialTheme.typography.titleLarge,
                maxLines = RoutineTextDefaults.Body)
            RoutineText(stringResource(R.string.backup_description), style = MaterialTheme.typography.bodySmall,
                color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
            Button(
                enabled = !busy,
                onClick = { onAction(TimelineAction.ExportSchedule) },
                shape = RoutineShapes.Pill,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { RoutineLabel(stringResource(R.string.backup_export), style = MaterialTheme.typography.labelLarge) }
            OutlinedTextField(
                value = importText,
                onValueChange = onImportText,
                label = { RoutineText(stringResource(R.string.backup_import_hint)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                maxLines = 6,
                enabled = !busy,
            )
            Button(
                enabled = !busy && importText.isNotBlank(),
                onClick = { onAction(TimelineAction.ImportSchedule(importText)) },
                shape = RoutineShapes.Pill,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { RoutineLabel(stringResource(R.string.backup_import), style = MaterialTheme.typography.labelLarge) }
        }
    }
    item(key = "settings-divider") { HorizontalDivider(color = RoutineColors.Border) }
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
    var flags by rememberSaveable(config) {
        mutableIntStateOf(WarningType.entries.fold(0) { mask, type -> if (config.isEnabled(type)) mask or (1 shl type.ordinal) else mask })
    }
    var invalid by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalRoutineHaptics.current
    fun enabled(type: WarningType) = flags and (1 shl type.ordinal) != 0
    fun toggle(type: WarningType, value: Boolean) {
        flags = if (value) flags or (1 shl type.ordinal) else flags and (1 shl type.ordinal).inv()
    }
    // Thresholds commit themselves once the reader stops typing: a debounced save keeps the sheet
    // free of a second truth ("edited" vs "saved") while never firing on half-typed digits.
    LaunchedEffect(focus, cognitive, transition, daily, sedentary, gapMin, gapMax, flags) {
        kotlinx.coroutines.delay(600)
        val parsed = runCatching {
            HealthConfig(
                focus.toInt(), cognitive.toInt(), transition.toInt(), daily.toInt(), sedentary.toInt(),
                gapMin.toInt(), gapMax.toInt(),
                enabled(WarningType.CONCENTRATION_LIMIT), enabled(WarningType.HIGH_COGNITIVE_LOAD),
                enabled(WarningType.INSUFFICIENT_TRANSITION), enabled(WarningType.BURNOUT_RISK),
                enabled(WarningType.PHYSICAL_RESET), enabled(WarningType.FRAGMENTED_TIME),
            )
        }.getOrNull()
        invalid = parsed == null
        parsed?.let(onSave)
    }
    Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.md)) {
        RoutineText(stringResource(R.string.advanced_hint), style = MaterialTheme.typography.bodySmall,
            color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
        RuleField(stringResource(R.string.threshold_focus), focus, { focus = it }, 1..720,
            enabled(WarningType.CONCENTRATION_LIMIT), busy) { toggle(WarningType.CONCENTRATION_LIMIT, it) }
        RuleField(stringResource(R.string.threshold_cognitive), cognitive, { cognitive = it }, 30..720,
            enabled(WarningType.HIGH_COGNITIVE_LOAD), busy) { toggle(WarningType.HIGH_COGNITIVE_LOAD, it) }
        RuleField(stringResource(R.string.threshold_transition), transition, { transition = it }, 0..240,
            enabled(WarningType.INSUFFICIENT_TRANSITION), busy) { toggle(WarningType.INSUFFICIENT_TRANSITION, it) }
        RuleField(stringResource(R.string.threshold_daily), daily, { daily = it }, 30..1440,
            enabled(WarningType.BURNOUT_RISK), busy) { toggle(WarningType.BURNOUT_RISK, it) }
        RuleField(stringResource(R.string.threshold_sedentary), sedentary, { sedentary = it }, 15..720,
            enabled(WarningType.PHYSICAL_RESET), busy) { toggle(WarningType.PHYSICAL_RESET, it) }
        SettingRow(
            title = stringResource(R.string.threshold_fragmented),
            description = stringResource(R.string.rule_enabled),
            control = {
                RoutineSwitch(enabled(WarningType.FRAGMENTED_TIME), { toggle(WarningType.FRAGMENTED_TIME, it) }, enabled = !busy)
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
            MinuteField(gapMin, { gapMin = it }, stringResource(R.string.threshold_min), 1..1439,
                !busy && enabled(WarningType.FRAGMENTED_TIME), Modifier.weight(1f))
            MinuteField(gapMax, { gapMax = it }, stringResource(R.string.threshold_max), 1..1439,
                !busy && enabled(WarningType.FRAGMENTED_TIME), Modifier.weight(1f))
        }
        if (invalid) {
            RoutineText(stringResource(R.string.error_thresholds), style = MaterialTheme.typography.bodySmall,
                color = RoutineColors.Warning, maxLines = RoutineTextDefaults.Paragraph)
        }
        TextButton(enabled = !busy, onClick = { invalid = false; onSave(HealthConfig()) }) {
            RoutineLabel(stringResource(R.string.reset_thresholds), style = MaterialTheme.typography.labelLarge,
                color = RoutineColors.Error)
        }
    }
}

@Composable
private fun RuleField(
    title: String,
    value: String,
    onValue: (String) -> Unit,
    range: IntRange,
    enabled: Boolean,
    busy: Boolean,
    onEnabled: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
        SettingRow(
            title = title,
            description = stringResource(R.string.rule_enabled),
            control = { RoutineSwitch(enabled, onEnabled, enabled = !busy) },
        )
        MinuteField(value, onValue, stringResource(R.string.threshold_minutes), range, enabled && !busy,
            Modifier.fillMaxWidth())
    }
}

@Composable
private fun MinuteField(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    range: IntRange,
    enabled: Boolean,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValue(it.filter(Char::isDigit).take(4)) },
        label = { RoutineText(label) },
        supportingText = {
            RoutineLabel(stringResource(R.string.threshold_range, range.first, range.last),
                style = MaterialTheme.typography.labelSmall)
        },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
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
    fun push() = onAction(
        TimelineAction.SetPeriodicBreak(
            PeriodicBreakConfig(
                enabled,
                (every.toIntOrNull() ?: 60).coerceIn(30, 240),
                (len.toIntOrNull() ?: 5).coerceIn(1, 60),
            ),
        ),
    )
    Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
        SettingRow(
            title = stringResource(R.string.periodic_break_title),
            description = stringResource(R.string.periodic_break_description),
            control = { RoutineSwitch(enabled, { enabled = it; push() }, enabled = !busy) },
        )
        if (enabled) {
            Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
                RoutineText(stringResource(R.string.periodic_break_every), style = MaterialTheme.typography.titleSmall,
                    maxLines = RoutineTextDefaults.Body)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm), verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
                    listOf(45, 60, 90, 120).forEach { minutes ->
                        FilterChip(selected = config.everyMinutes == minutes,
                            onClick = { every = minutes.toString(); push() }, enabled = !busy,
                            label = { RoutineLabel(stringResource(R.string.sleep_minutes_format, minutes)) },
                            shape = RoutineShapes.Chip)
                    }
                }
                RoutineText(stringResource(R.string.periodic_break_len), style = MaterialTheme.typography.titleSmall,
                    maxLines = RoutineTextDefaults.Body)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm), verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
                    listOf(5, 10, 15).forEach { minutes ->
                        FilterChip(selected = config.breakMinutes == minutes,
                            onClick = { len = minutes.toString(); push() }, enabled = !busy,
                            label = { RoutineLabel(stringResource(R.string.sleep_minutes_format, minutes)) },
                            shape = RoutineShapes.Chip)
                    }
                }
            }
            RoutineText(stringResource(R.string.periodic_break_hint), style = MaterialTheme.typography.bodySmall,
                color = RoutineColors.TextMuted, maxLines = RoutineTextDefaults.Paragraph)
        }
    }
}
