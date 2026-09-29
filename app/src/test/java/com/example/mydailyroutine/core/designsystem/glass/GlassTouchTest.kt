package com.example.mydailyroutine.core.designsystem.glass

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GlassTouchTest {
    @Test fun theSameTouchObjectExposesNewFramesWithoutBeingRecreated() {
        val fraction = mutableStateOf(0f)
        val glow = mutableStateOf(0f)
        val point = mutableStateOf<Offset?>(null)
        val touch = GlassTouch(MutableInteractionSource(), fraction, 0.96f, glow, point)

        assertEquals(1f, touch.press, 0.0001f)
        assertNull(touch.point)
        fraction.value = 1f
        glow.value = 0.75f
        point.value = Offset(10f, 20f)
        assertEquals(0.96f, touch.press, 0.0001f)
        assertEquals(0.75f, touch.glow, 0.0001f)
        assertEquals(Offset(10f, 20f), touch.point)

        // Preserve the existing underdamped spring rather than clamping its overshoot.
        fraction.value = -0.1f
        assertEquals(1.004f, touch.press, 0.0001f)
        point.value = null
        assertNull(touch.point)
    }
}
