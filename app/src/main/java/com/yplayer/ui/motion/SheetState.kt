package com.yplayer.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

@Stable
class SheetState(
    val density: Density,
    private val scope: CoroutineScope,
    private val reducedMotion: Boolean = false,
) {
    val progress = Animatable(0f)

    var isDragging by mutableStateOf(false)
        private set

    fun applyDrag(deltaProgress: Float) {
        val raw = progress.value + deltaProgress
        val banded = when {
            raw < 0f -> -rubberband(-raw, 1f)
            raw > 1f -> 1f + rubberband(raw - 1f, 1f)
            else -> raw
        }
        scope.launch { progress.snapTo(banded) }
    }

    fun onDragStart() {
        isDragging = true
        scope.launch { progress.stop() }
    }

    fun onDragEnd(velocityPerSecond: Float) {
        isDragging = false
        val current = progress.value
        val projected = current + projectMomentum(velocityPerSecond)
        val target = chooseAnchor(current, projected)
        val spec = if (abs(velocityPerSecond) > MotionSpecs.MOMENTUM_THRESHOLD) {
            MotionSpecs.Momentum
        } else {
            MotionSpecs.Settle
        }
        scope.launch {
            if (reducedMotion) progress.snapTo(target)
            else progress.animateTo(target, spec, initialVelocity = velocityPerSecond)
        }
    }

    fun cancelDrag() {
        isDragging = false
    }

    fun expand() = settleTo(1f)

    fun collapse() = settleTo(0f)

    private fun settleTo(target: Float) {
        scope.launch {
            if (reducedMotion) progress.snapTo(target)
            else progress.animateTo(target, MotionSpecs.Settle, initialVelocity = progress.velocity)
        }
    }
}

@Composable
fun rememberSheetState(reducedMotion: Boolean = false): SheetState {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    return remember { SheetState(density, scope, reducedMotion) }
}
