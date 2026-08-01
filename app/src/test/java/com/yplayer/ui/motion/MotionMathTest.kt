package com.yplayer.ui.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionMathTest {

    @Test
    fun rubberband_zeroOvershootIsZero() {
        assertEquals(0f, rubberband(0f, 1f), 0.0001f)
    }

    @Test
    fun rubberband_compressesOvershootBelowDimension() {
        assertTrue(rubberband(100f, 1f) < 1f)
        assertTrue(rubberband(100f, 1f) > 0f)
    }

    @Test
    fun projectMomentum_zeroVelocityIsZero() {
        assertEquals(0f, projectMomentum(0f), 0.0001f)
    }

    @Test
    fun projectMomentum_projectsPositiveVelocity() {
        assertTrue(projectMomentum(1000f) > 100f)
    }

    @Test
    fun chooseAnchor_picksNearestAnchor() {
        assertEquals(1f, chooseAnchor(0.1f, 0.9f), 0.0001f)
        assertEquals(0f, chooseAnchor(0.8f, 0.2f), 0.0001f)
    }
}
