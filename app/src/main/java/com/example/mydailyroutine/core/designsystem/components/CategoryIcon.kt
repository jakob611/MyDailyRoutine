package com.example.mydailyroutine.core.designsystem.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.mydailyroutine.domain.model.RoutineCategory

/** Shared semantic icon vocabulary; category color and icon always describe the same concept. */
fun categoryIcon(category: RoutineCategory): ImageVector = when(category) {
    RoutineCategory.SCHOOL -> Icons.Outlined.School
    RoutineCategory.FOCUS_ANALYTICAL -> Icons.Outlined.Calculate
    RoutineCategory.FOCUS_SYNTHESIZING -> Icons.Outlined.EditNote
    RoutineCategory.ADMIN -> Icons.Outlined.Checklist
    RoutineCategory.REST_BUFFER -> Icons.Outlined.Spa
    RoutineCategory.EMERGENCY_RESERVE -> Icons.Outlined.Savings
}
