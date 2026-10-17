package io.github.xsyetopz.jagfx.core.vorbis

/**
 * Rounds [x] to the nearest `f32` value, as a [Double].
 *
 * The game client decodes in `f32`, and Kotlin/JS has no `f32` arithmetic.
 * The decoder computes in [Double] and rounds after each operation.
 * For `+`, `-`, `*` and `/` of two `f32` values, this gives the exact `f32` result.
 */
internal expect fun fround(x: Double): Double
