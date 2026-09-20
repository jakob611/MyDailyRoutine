package com.example.mydailyroutine.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.mydailyroutine.core.designsystem.glass.GlassRole
import com.example.mydailyroutine.core.designsystem.glass.GlassStyles
import com.example.mydailyroutine.core.designsystem.theme.OledColorScheme
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.categoryStyle
import com.example.mydailyroutine.domain.model.RoutineCategory
import org.junit.Assert.*
import org.junit.Test

class PaletteTest {
    @Test fun materialSurfaceHierarchyMatchesProductRoles() {
        with(OledColorScheme) {
            assertEquals(RoutineColors.Background, surface)
            assertEquals(RoutineColors.SurfaceLowest, surfaceContainerLowest)
            assertEquals(RoutineColors.SurfaceLow, surfaceContainerLow)
            assertEquals(RoutineColors.SurfaceContainer, surfaceContainer)
            assertEquals(RoutineColors.SurfaceHigh, surfaceContainerHigh)
            assertEquals(RoutineColors.SurfaceHighest, surfaceContainerHighest)
            assertEquals(Color.Transparent, surfaceTint)
        }
    }

    @Test fun interactionTimerFocusAndCompletionAreDistinct() {
        assertEquals(RoutineColors.Primary, OledColorScheme.primary)
        assertEquals(RoutineColors.Error, OledColorScheme.error)
        val semantics = setOf(RoutineColors.Primary, RoutineColors.Timer,
            RoutineColors.FocusAccent, RoutineColors.Success, RoutineColors.Warning, RoutineColors.Error)
        assertEquals(6, semantics.size)
        assertEquals(RoutineColors.FocusAccent, categoryStyle(RoutineCategory.FOCUS_ANALYTICAL).accent)
        assertEquals(RoutineColors.ProjectAccent, categoryStyle(RoutineCategory.FOCUS_SYNTHESIZING).accent)
    }

    @Test fun subjectSwatchesAreOpaqueAndSavedColoursAreNotRewritten() {
        assertEquals(6, RoutineColors.subjectSwatches.distinct().size)
        assertTrue(RoutineColors.subjectSwatches.all { it in 0xFF000000L..0xFFFFFFFFL })
        val saved = 0xFF7DE2D1L
        assertEquals(saved.toInt(), categoryStyle(RoutineCategory.SCHOOL, saved).accent.toArgb())
        assertEquals(RoutineColors.School.content, categoryStyle(RoutineCategory.SCHOOL, saved).content)
    }

    @Test fun fallbackSurfacesAreOpaqueAndEffectsAreModerate() {
        GlassRole.entries.forEach { role ->
            assertEquals(1f, role.surface.alpha, 0f)
            assertTrue(role.style.blur.value in 4f..10f)
            assertTrue(role.style.rim <= 0.16f)
        }
        assertEquals(0.52f, GlassStyles.Card.surfaceAlpha, 0f)
        assertEquals(0.58f, GlassStyles.Compact.surfaceAlpha, 0f)
    }
}
