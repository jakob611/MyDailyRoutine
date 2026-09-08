package com.example.mydailyroutine.features.planning.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.domain.learning.StudyTopic
import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.features.entry.presentation.AppDatePicker
import com.example.mydailyroutine.core.designsystem.theme.*
import com.example.mydailyroutine.core.presentation.*
import java.time.Duration
import java.time.LocalDate

@Composable
fun ActualCompletionDialog(block: ResolvedTimelineItem.Block, busy: Boolean, onAction: (TimelineAction) -> Unit) {
    var minutes by rememberSaveable(block.key) { mutableStateOf(Duration.between(block.startsAt, block.endsAt).toMinutes().toString()) }
    var invalid by rememberSaveable { mutableStateOf(false) }
    AlertDialog(onDismissRequest = { onAction(TimelineAction.CloseActual) }, title = { Text(stringResource(R.string.actual_title)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(block.title, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.actual_hint), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(5) }, label = { Text(stringResource(R.string.actual_minutes)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, enabled = !busy)
            if (invalid) Text(stringResource(R.string.actual_invalid), color = RoutineColors.Warning)
        }
    }, confirmButton = { TextButton(enabled = !busy, onClick = {
        val value = minutes.toIntOrNull(); invalid = value == null || value !in 1..10080
        if (!invalid) onAction(TimelineAction.RecordActual(block, value!!))
    }) { Text(stringResource(R.string.actual_save)) } }, dismissButton = { TextButton(onClick = { onAction(TimelineAction.CloseActual) }) { Text(stringResource(R.string.cancel)) } })
}
