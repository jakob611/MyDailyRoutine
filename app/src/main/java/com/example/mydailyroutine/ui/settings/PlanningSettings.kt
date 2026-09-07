package com.example.mydailyroutine.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.domain.model.ScheduleValidation
import com.example.mydailyroutine.domain.planning.PlanningConfig
import com.example.mydailyroutine.ui.theme.RoutineColors
import com.example.mydailyroutine.ui.timeline.minuteLabel

@Composable
fun PlanningSettings(config: PlanningConfig, busy: Boolean, onSave: (PlanningConfig) -> Unit) {
    var capacity by rememberSaveable(config) { mutableStateOf(config.dailyStudyCapacityMinutes.toString()) }
    var start by rememberSaveable(config) { mutableStateOf(minuteLabel(config.studyStartMinutes)) }
    var end by rememberSaveable(config) { mutableStateOf(minuteLabel(config.studyEndMinutes)) }
    var center by rememberSaveable(config) { mutableStateOf(minuteLabel(config.dipCenterMinutes)) }
    var sigma by rememberSaveable(config) { mutableStateOf(config.dipSigmaMinutes.toString()) }
    var slip by rememberSaveable(config) { mutableStateOf(config.defaultSlipMinutes.toString()) }
    var focus by rememberSaveable(config) { mutableStateOf(config.targetFocusMinutes.toString()) }
    var transition by rememberSaveable(config) { mutableStateOf(config.postSchoolRecoveryMinutes.toString()) }
    var error by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.planning_settings_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.model_disclaimer), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(capacity, { capacity = it }, label = { Text(stringResource(R.string.planning_capacity)) }, enabled = !busy, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(start, { start = it }, label = { Text(stringResource(R.string.planning_start)) }, enabled = !busy, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(end, { end = it }, label = { Text(stringResource(R.string.planning_end)) }, enabled = !busy, singleLine = true, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(center, { center = it }, label = { Text(stringResource(R.string.planning_dip)) }, enabled = !busy, singleLine = true)
        OutlinedTextField(sigma, { sigma = it }, label = { Text(stringResource(R.string.planning_sigma)) }, enabled = !busy, singleLine = true)
        OutlinedTextField(slip, { slip = it }, label = { Text(stringResource(R.string.planning_slip)) }, enabled = !busy, singleLine = true)
        OutlinedTextField(focus, { focus = it }, label = { Text(stringResource(R.string.planning_focus)) }, enabled = !busy, singleLine = true)
        OutlinedTextField(transition, { transition = it }, label = { Text(stringResource(R.string.planning_transition)) }, enabled = !busy, singleLine = true)
        Text(stringResource(R.string.planning_limit_hint), style = MaterialTheme.typography.bodySmall)
        if (error) Text(stringResource(R.string.planning_invalid), color = RoutineColors.Warning)
        Button(enabled = !busy, onClick = {
            val value = runCatching { PlanningConfig(capacity.toInt(), requireNotNull(ScheduleValidation.parseTime(start)).toSecondOfDay()/60,
                if (end == "24:00") 1440 else requireNotNull(ScheduleValidation.parseTime(end)).toSecondOfDay()/60,
                requireNotNull(ScheduleValidation.parseTime(center)).toSecondOfDay()/60, sigma.toInt(), slip.toInt(), focus.toInt(), transition.toInt()) }.getOrNull()
            error = value == null; value?.let(onSave)
        }) { Text(stringResource(R.string.planning_save)) }
    }
}
