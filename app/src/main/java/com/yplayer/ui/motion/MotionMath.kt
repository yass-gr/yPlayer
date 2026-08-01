package com.yplayer.ui.motion

const val DECELERATION_RATE = 0.998f
const val RUBBER_CONSTANT = 0.55f

fun rubberband(overshoot: Float, dimension: Float, constant: Float = RUBBER_CONSTANT): Float {
    val d = dimension.coerceAtLeast(1e-4f)
    val o = overshoot.coerceAtLeast(0f)
    return (o * d * constant) / (d + constant * o)
}

fun projectMomentum(velocityPerSecond: Float, decelerationRate: Float = DECELERATION_RATE): Float =
    (velocityPerSecond / 1000f) * decelerationRate / (1f - decelerationRate)

fun chooseAnchor(progress: Float, projected: Float): Float =
    if (projected >= 0.5f) 1f else 0f
