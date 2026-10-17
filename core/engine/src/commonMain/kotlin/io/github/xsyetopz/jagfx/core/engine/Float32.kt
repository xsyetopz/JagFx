package io.github.xsyetopz.jagfx.core.engine

/**
 * Rounds [x] to the nearest `f32` value, as a [Double].
 *
 * Kotlin/JS represents [Float] as a JS number, so `Float` arithmetic does not round to `f32` there.
 * The filter (spec §8.9) does its `fl32` arithmetic in [Double] and rounds after each operation.
 * For `+`, `-`, `*` and `/` of two `f32` values, this gives the exact `f32` result.
 */
internal expect fun fround(x: Double): Double
