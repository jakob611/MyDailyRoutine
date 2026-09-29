package com.example.mydailyroutine.core.designsystem

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import com.example.mydailyroutine.core.designsystem.motion.effectSpec
import com.example.mydailyroutine.core.designsystem.motion.glassMorphSpec
import com.example.mydailyroutine.core.designsystem.motion.glassTouchSpec
import com.example.mydailyroutine.core.designsystem.motion.spatialSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionSpecTest {
    @Test fun allSharedSpecsSnapWhenMotionIsReduced() {
        listOf(spatialSpec<Float>(true), effectSpec<Float>(true),
            glassTouchSpec<Float>(true), glassMorphSpec<Float>(true)).forEach {
            assertTrue(it is SnapSpec<*>)
        }
    }

    @Test fun normalMotionKeepsItsOriginalSpringAndEffectVocabulary() {
        assertTrue(spatialSpec<Float>(false) is SpringSpec<*>)
        assertTrue(glassTouchSpec<Float>(false) is SpringSpec<*>)
        assertTrue(glassMorphSpec<Float>(false) is SpringSpec<*>)
        assertEquals(120, (effectSpec<Float>(false, 120) as TweenSpec<Float>).durationMillis)
    }
}
