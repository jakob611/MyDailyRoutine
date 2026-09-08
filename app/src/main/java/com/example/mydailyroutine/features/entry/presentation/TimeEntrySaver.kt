package com.example.mydailyroutine.features.entry.presentation

import androidx.compose.runtime.saveable.listSaver
import com.example.mydailyroutine.domain.routines.TimeEntryState

val TimeEntrySaver = listSaver<TimeEntryState,Any>(
    save={ listOf(it.startText,it.endText,it.durationMinutes) },
    restore={ TimeEntryState(it[0] as String,it[1] as String,it[2] as Int) },
)
