package com.example.mydailyroutine.core.designsystem.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.consumeAll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity

/**
 * Every bottom sheet in the app enters through ModalBottomSheet's own animation; this host gives them
 * the matching exit: on close the sheet animates to Hidden before leaving composition, instead of popping.
 * The content must pass the hoisted [SheetState] into its ModalBottomSheet so hide() drives the real panel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineSheet(visible: Boolean, content: @Composable (sheetState: SheetState) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mounted by remember { mutableStateOf(visible) }
    LaunchedEffect(visible) {
        if (visible) mounted = true
        else if (mounted) {
            sheetState.hide()
            mounted = false
        }
    }
    if (mounted) content(sheetState)
}

/**
 * Stops a fast fling inside a sheet's body from handing its leftover velocity to the sheet's own
 * drag logic. Without this, every hard flick that reaches the content's edge also pushes the panel:
 * the sheet sinks a few pixels, the spring pulls it back, the next flick sinks it again — the whole
 * screen jitters while the reader is scrolling fast inside it.
 *
 * Apply this modifier **above the scrollable in the modifier chain** (or on the lazy list node,
 * which already sits above its internal scrollable): a nested scroll node only sees the scrolling
 * of the nodes below it, so it must be the body scrollable's ancestor to consume the fling's
 * residual before it propagates up to the sheet's drag logic. Only the *fling's* leftover is
 * consumed — a finger dragging the content at the edge still follows the sheet, which is the
 * deliberate, pleasant part of the behaviour, so closing by slow drag keeps working.
 */
fun Modifier.sheetFlingStabilizer(): Modifier = this.nestedScroll(
    object : NestedScrollConnection {
        // onPostFling is only ever dispatched for real flings, so consuming everything here
        // is exactly the residual that would otherwise reach the sheet and sink it.
        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
            available.consumeAll()
    },
)
