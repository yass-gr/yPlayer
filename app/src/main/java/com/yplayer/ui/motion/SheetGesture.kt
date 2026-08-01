package com.yplayer.ui.motion

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker

fun Modifier.sheetDrag(
    state: SheetState,
    dragRangePx: Float,
    onTap: (() -> Unit)? = null,
): Modifier = pointerInput(state, dragRangePx) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = true)
        state.onDragStart()
        val tracker = VelocityTracker()
        var moved = false
        try {
            drag(down.id) { change ->
                change.consume()
                moved = true
                val dragAmount = change.positionChange()
                tracker.addPosition(change.uptimeMillis, change.position)
                state.applyDrag(-dragAmount.y / dragRangePx)
            }
        } finally {
            val velocity = tracker.calculateVelocity().y
            if (moved) {
                state.onDragEnd(-velocity / dragRangePx)
            } else {
                state.cancelDrag()
                onTap?.invoke()
            }
        }
    }
}
