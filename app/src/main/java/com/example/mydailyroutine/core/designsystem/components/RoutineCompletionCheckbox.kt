package com.example.mydailyroutine.core.designsystem.components

import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors

/** Completion is success green; settings/selection controls retain the primary interaction colour. */
@Composable
fun RoutineCompletionCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = CheckboxDefaults.colors(
            checkedColor = RoutineColors.Success,
            checkmarkColor = RoutineColors.Background,
        ),
    )
}
