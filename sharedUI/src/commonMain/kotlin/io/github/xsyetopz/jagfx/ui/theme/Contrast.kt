package io.github.xsyetopz.jagfx.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

private const val LINEAR_THRESHOLD = 0.03928f
private const val LINEAR_DIVISOR = 12.92f
private const val GAMMA_OFFSET = 0.055f
private const val GAMMA_SCALE = 1.055f
private const val GAMMA = 2.4
private const val RED_WEIGHT = 0.2126f
private const val GREEN_WEIGHT = 0.7152f
private const val BLUE_WEIGHT = 0.0722f
private const val FLARE = 0.05f

private fun linear(channel: Float): Float =
    if (channel <= LINEAR_THRESHOLD) {
        channel / LINEAR_DIVISOR
    } else {
        ((channel + GAMMA_OFFSET) / GAMMA_SCALE).toDouble().pow(GAMMA).toFloat()
    }

/** WCAG relative luminance of opaque [color]. */
internal fun relativeLuminance(color: Color): Float =
    RED_WEIGHT * linear(color.red) + GREEN_WEIGHT * linear(color.green) +
        BLUE_WEIGHT * linear(color.blue)

/** WCAG contrast ratio of [a] and [b], from 1 (equal) to 21 (black on white); order does not matter. */
internal fun contrastRatio(
    a: Color,
    b: Color,
): Float {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    return (maxOf(la, lb) + FLARE) / (minOf(la, lb) + FLARE)
}
