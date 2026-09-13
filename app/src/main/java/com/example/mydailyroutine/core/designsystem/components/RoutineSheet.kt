package com.example.mydailyroutine.core.designsystem.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.hide
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

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
