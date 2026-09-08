package com.example.mydailyroutine.features.entry.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.domain.routines.EntryDefaults

@Composable
fun EntryDefaultsSettings(defaults: EntryDefaults, busy: Boolean, onSave: (EntryDefaults) -> Unit) {
    var duration by rememberSaveable(defaults) { mutableStateOf(defaults.lessonDurationMinutes.toString()) }
    var pause by rememberSaveable(defaults) { mutableStateOf(defaults.lessonBreakMinutes.toString()) }
    var error by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.entry_defaults_title),style=MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.entry_defaults_hint),style=MaterialTheme.typography.bodySmall,color=RoutineColors.TextSecondary)
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(duration,{ duration=it.filter(Char::isDigit).take(3) },label={ Text(stringResource(R.string.default_lesson_duration)) },enabled=!busy,singleLine=true,modifier=Modifier.weight(1f))
            OutlinedTextField(pause,{ pause=it.filter(Char::isDigit).take(2) },label={ Text(stringResource(R.string.default_lesson_break)) },enabled=!busy,singleLine=true,modifier=Modifier.weight(1f))
        }
        if(error) Text(stringResource(R.string.entry_defaults_invalid),color=RoutineColors.Warning,style=MaterialTheme.typography.bodySmall)
        OutlinedButton(enabled=!busy,onClick={
            val value=runCatching { EntryDefaults(duration.toInt(),pause.toInt()) }.getOrNull()
            error=value==null;value?.let(onSave)
        }) { Text(stringResource(R.string.entry_defaults_save)) }
    }
}
