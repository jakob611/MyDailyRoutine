package com.example.mydailyroutine.features.routines.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.presentation.*
import com.example.mydailyroutine.domain.model.ScheduleValidation
import com.example.mydailyroutine.domain.routines.SleepSchedule

@Composable
fun SleepSettings(schedule: SleepSchedule, busy: Boolean, onAction: (TimelineAction) -> Unit) {
    var enabled by rememberSaveable(schedule) { mutableStateOf(schedule.enabled) }
    var bed by rememberSaveable(schedule) { mutableStateOf(schedule.bedtime.clockLabel()) }
    var wake by rememberSaveable(schedule) { mutableStateOf(schedule.wakeTime.clockLabel()) }
    var days by rememberSaveable(schedule) { mutableIntStateOf(schedule.weekdaysMask) }
    var morning by rememberSaveable(schedule) { mutableStateOf(schedule.morningBufferMinutes.toString()) }
    var invalid by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.sleep_heading),style=MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.sleep_hint),style=MaterialTheme.typography.bodySmall,color=RoutineColors.TextSecondary)
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text(stringResource(R.string.sleep_enable),Modifier.weight(1f))
            Switch(enabled,{ enabled=it },enabled=!busy)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(bed,{ bed=it;invalid=false },label={ Text(stringResource(R.string.sleep_bedtime)) },leadingIcon={ Icon(Icons.Outlined.Bedtime,null) },singleLine=true,enabled=!busy,modifier=Modifier.weight(1f))
            OutlinedTextField(wake,{ wake=it;invalid=false },label={ Text(stringResource(R.string.sleep_wake)) },leadingIcon={ Icon(Icons.Outlined.WbSunny,null) },singleLine=true,enabled=!busy,modifier=Modifier.weight(1f))
        }
        val start=ScheduleValidation.parseTime(bed); val end=ScheduleValidation.parseTime(wake)
        if (start!=null && end!=null && start!=end) Text(stringResource(R.string.sleep_planned_duration,durationLabel(com.example.mydailyroutine.domain.model.nominalMinutes(start,end))),style=MaterialTheme.typography.labelMedium,color=RoutineColors.Sage)
        Text(stringResource(R.string.sleep_days),style=MaterialTheme.typography.titleSmall)
        WeekdayPicker(days,!busy) { days=it }
        TextButton(enabled=!busy,onClick={ days=com.example.mydailyroutine.domain.routines.Weekdays.shifted(com.example.mydailyroutine.domain.routines.Weekdays.WORKDAYS,-1) }) {
            Text(stringResource(R.string.sleep_before_workdays))
        }
        Text(stringResource(R.string.sleep_days_hint),style=MaterialTheme.typography.bodySmall,color=RoutineColors.TextSecondary)
        OutlinedTextField(morning,{ morning=it.filter(Char::isDigit).take(3) },label={ Text(stringResource(R.string.sleep_morning)) },singleLine=true,enabled=!busy)
        Text(stringResource(R.string.sleep_morning_hint),style=MaterialTheme.typography.bodySmall,color=RoutineColors.TextSecondary)
        if(invalid) Text(stringResource(R.string.sleep_invalid),style=MaterialTheme.typography.bodySmall,color=RoutineColors.Warning)
        FilledTonalButton(enabled=!busy,onClick={
            val value=runCatching { SleepSchedule(enabled,requireNotNull(start),requireNotNull(end),days,morning.toInt()) }.getOrNull()
            invalid=value==null
            value?.let { onAction(TimelineAction.SaveSleep(it,context.getString(R.string.sleep_title),context.getString(R.string.sleep_morning_title))) }
        }) { Text(stringResource(R.string.sleep_save)) }
    }
}
