package com.yplayer.ui.motion

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

object MotionSpecs {
    val Settle = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    val Momentum = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessMediumLow,
    )

    const val MOMENTUM_THRESHOLD = 0.15f
}
