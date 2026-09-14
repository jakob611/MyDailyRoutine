package com.example.mydailyroutine.features.routines.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.*
import com.example.mydailyroutine.core.presentation.*
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.routines.RoutineOrigin
import com.example.mydailyroutine.features.timeline.components.NowMarker
import java.time.ZonedDateTime

/** Compact context cards keep five-minute breaks and overnight sleep from overwhelming the day. */
@Composable
fun ManagedRoutineCard(block: ResolvedTimelineItem.Block, now: ZonedDateTime, showNow: Boolean, busy: Boolean,
    modifier: Modifier = Modifier, onAction: (TimelineAction) -> Unit) {
    var expanded by remember(block.key) { mutableStateOf(false) }
    val haptics = LocalRoutineHaptics.current
    val sleep = block.origin == RoutineOrigin.SLEEP
    val morning = block.origin == RoutineOrigin.MORNING_BUFFER
    val tint = if(block.isSuppressed) RoutineColors.TextMuted else RoutineColors.Sage
    var cardH by remember { mutableFloatStateOf(0f) }
    Column(modifier.fillMaxWidth(), verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.width(60.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                Text(minuteLabel(block.startMinute),style=MaterialTheme.typography.bodySmall,color=RoutineColors.TextSecondary)
                Text(minuteLabel(block.endMinute),style=MaterialTheme.typography.bodySmall,color=RoutineColors.TextMuted)
            }
            Box(Modifier.weight(1f).onSizeChanged { cardH = it.height.toFloat() }) {
            OutlinedCard(onClick={ expanded=!expanded;haptics.tap() },modifier=Modifier.fillMaxWidth(),shape=RoutineShapes.Card,
                border=BorderStroke(1.dp,RoutineColors.CardBorder),colors=CardDefaults.outlinedCardColors(containerColor=RoutineColors.Surface1)) {
                Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Icon(if(sleep) Icons.Outlined.Bedtime else if(morning) Icons.Outlined.WbSunny else Icons.Outlined.Spa,null,Modifier.size(20.dp),tint=tint)
                        Text(block.title,Modifier.weight(1f),style=MaterialTheme.typography.titleSmall,color=tint)
                        if(!sleep) Text(stringResource(R.string.duration_minutes,block.durationMinutes),style=MaterialTheme.typography.labelSmall,color=tint)
                    }
                    Text(when {
                        block.companionConflict -> stringResource(R.string.break_no_room)
                        block.isSuppressed -> stringResource(R.string.school_inactive,block.holidayTitle.orEmpty())
                        sleep -> stringResource(R.string.managed_sleep_notice)
                        morning -> stringResource(R.string.managed_morning_notice)
                        else -> stringResource(R.string.break_follows_lesson)
                    },style=MaterialTheme.typography.bodySmall,color=RoutineColors.TextSecondary)
                    if(expanded) Row {
                        TextButton(enabled=!busy,onClick={onAction(TimelineAction.Skip(block))}) { Text(stringResource(R.string.skip_date)) }
                        if(sleep || morning) TextButton(onClick={onAction(TimelineAction.OpenSettings)}) { Text(stringResource(R.string.sleep_manage)) }
                        else TextButton(enabled=!busy,onClick={onAction(TimelineAction.RequestDelete(block))}) { Text(stringResource(R.string.delete)) }
                    }
                }
            }
            if (showNow && cardH > 0f) {
                val nowMinute = now.hour * 60 + now.minute
                val span = (block.endMinute - block.startMinute).coerceAtLeast(1)
                val progress = ((nowMinute - block.startMinute).toFloat() / span).coerceIn(0f, 1f)
                val offset = with(LocalDensity.current) { (cardH * progress).coerceIn(12f, (cardH - 12f).coerceAtLeast(12f)).toDp() }
                NowMarker(now.toLocalTime().clockLabel(), Modifier.fillMaxWidth().offset(y = offset))
            }
            }
        }
    }
}
