package com.novasa.modulardragview.extension

import kotlin.math.max
import kotlin.math.min

fun Float.clamped(min: Float, max: Float) = max(min, min(max, this))
fun Float.normalized(min: Float, max: Float): Float = if (min == 0f) this / max else (this - min) / (max - min)
fun Float.denormalized(min: Float, max: Float): Float = if (min == 0f) this * max else this * (max - min) + min
fun Float.normalizedClamped(min: Float, max: Float): Float = clamped(min, max).normalized(min, max)
fun Float.denormalizedClamped(min: Float, max: Float): Float = denormalized(min, max).clamped(min, max)
