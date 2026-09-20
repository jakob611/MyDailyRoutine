package com.example.mydailyroutine.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import kotlin.math.abs
import kotlinx.coroutines.launch

/** One drum row. Five rows stay visible, exactly as on an iOS picker. */
private val WheelItem = 44.dp
private val WheelViewport = WheelItem * 5

/**
 * Pure geometry of a drum: which row sits in the selection window. Kept free of Compose so a unit
 * test can pin it down: the pick is the row whose centre is closest to the viewport centre.
 */
internal fun wheelCenterIndex(offsets: List<Int>, sizes: List<Int>, indices: List<Int>, viewportCenter: Int): Int? =
    indices.indices.minByOrNull { i -> abs(offsets[i] + sizes[i] / 2f - viewportCenter) }?.let { indices[it] }

/**
 * An iOS-style countdown drum: one column of values, the middle row between two hairlines, the rows
 * above and below fading into the panel. Dragging snaps row by row, a tap on a visible row spins it
 * to the middle, and every settled row answers with a light tick — the answer a physical crown gives.
 */
@Composable
private fun WheelDrum(
    count: Int,
    state: LazyListState,
    tag: String,
) {
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    val haptics = LocalRoutineHaptics.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.collect { scrolling -> if (!scrolling) haptics.tap() }
    }
    Box(Modifier.width(76.dp)) {
        LazyColumn(
            state = state,
            flingBehavior = fling,
            modifier = Modifier.fillMaxWidth().height(WheelViewport).testTag(tag),
            contentPadding = PaddingValues(vertical = WheelItem * 2),
        ) {
            items(count, key = { it }) { index ->
                Box(
                    Modifier.fillMaxWidth().height(WheelItem).testTag("$tag-$index")
                        .clickable { haptics.tap(); scope.launch { state.animateScrollToItem(index) } },
                    contentAlignment = Alignment.Center,
                ) {
                    RoutineLabel("%02d".format(index), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        // The selection window: two hairlines, the only chrome a drum needs.
        Box(Modifier.align(Alignment.Center).fillMaxWidth().height(WheelItem)
            .border(1.dp, RoutineColors.CardBorder))
        // Fade masks: rows dissolve into the panel instead of stopping at a clip edge.
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(WheelItem * 2)
            .background(Brush.verticalGradient(listOf(RoutineColors.SheetSurface, Color.Transparent))))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(WheelItem * 2)
            .background(Brush.verticalGradient(listOf(Color.Transparent, RoutineColors.SheetSurface))))
    }
}

/**
 * The row in the selection window. The snap fling only ever rests on a row boundary and the drum
 * pads two rows top and bottom, so at rest the first visible row *is* the centred one; confirm
 * always happens at rest, because a finger cannot press the button mid-fling and keep it pressed.
 */
private fun centerIndexOf(state: LazyListState): Int = state.firstVisibleItemIndex

/**
 * The picker: two drums, hours and minutes, on one dark panel. Confirm reads the rows that are
 * actually centred, so what sits between the hairlines is exactly what you get.
 */
@Composable
fun TimeWheelDialog(
    initialHour: Int,
    initialMinute: Int,
    wheelTag: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val hourState = rememberLazyListState(initialHour.coerceIn(0, 23))
    val minuteState = rememberLazyListState(initialMinute.coerceIn(0, 59))
    val haptics = LocalRoutineHaptics.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoutineShapes.GlassPanel,
            color = RoutineColors.SheetSurface,
            border = BorderStroke(1.dp, RoutineColors.CardBorder),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(RoutineSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RoutineSpacing.md),
            ) {
                RoutineText(
                    stringResource(R.string.time_pick_title),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = RoutineTextDefaults.Body,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WheelDrum(24, hourState, "$wheelTag-hour")
                    RoutineLabel(":", style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = RoutineSpacing.sm))
                    WheelDrum(60, minuteState, "$wheelTag-minute")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                    SheetSecondaryButton(stringResource(R.string.cancel), onDismiss, modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            haptics.selection()
                            onConfirm("%02d:%02d".format(centerIndexOf(hourState), centerIndexOf(minuteState)))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoutineColors.Primary,
                            contentColor = RoutineColors.InkOnPrimary,
                        ),
                        shape = RoutineShapes.Pill,
                        modifier = Modifier.weight(1f).testTag("$wheelTag-confirm"),
                    ) { RoutineLabel(stringResource(R.string.confirm)) }
                }
            }
        }
    }
}
