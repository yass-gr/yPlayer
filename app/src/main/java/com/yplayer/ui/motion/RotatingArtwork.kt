package com.yplayer.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos

@Composable
fun rememberArtworkAngle(isPlaying: Boolean, degreesPerSecond: () -> Float): Float {
    val angle = remember { Animatable(0f) }
    val playing by rememberUpdatedState(isPlaying)
    val speed by rememberUpdatedState(degreesPerSecond)

    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        var lastNanos = -1L
        while (true) {
            val now = withFrameNanos { it }
            if (lastNanos > 0) {
                val dt = (now - lastNanos) / 1_000_000_000f
                angle.snapTo((angle.value + dt * speed()) % 360f)
            }
            lastNanos = now
        }
    }
    return angle.value
}
