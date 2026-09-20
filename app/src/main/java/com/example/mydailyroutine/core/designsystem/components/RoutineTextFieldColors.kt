package com.example.mydailyroutine.core.designsystem.components

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors

/** Recessed opaque input wells; focus/error outlines and text still come from Material semantics. */
@Composable
fun RoutineTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = RoutineColors.SurfaceLowest,
    unfocusedContainerColor = RoutineColors.SurfaceLowest,
    disabledContainerColor = RoutineColors.SurfaceLowest,
    errorContainerColor = RoutineColors.SurfaceLowest,
)
