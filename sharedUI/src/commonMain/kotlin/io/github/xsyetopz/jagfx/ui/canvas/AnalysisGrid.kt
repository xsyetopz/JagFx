package io.github.xsyetopz.jagfx.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import kotlin.math.floor
import kotlin.math.max

/** Distance in dp between two bright horizontal rules, the original's 22 px; a faint rule sits halfway. */
private const val HORIZONTAL_STEP = 22.0

/** Least distance in dp between two vertical lines; denser semitones are thinned out. */
private const val MIN_VERTICAL_SPACING = 3.0

/** Offset that centers a 1px line on a pixel. */
private const val HALF_PIXEL = 0.5

/** Semitone strides that divide an octave, so thinned lines still meet every C. */
private val VerticalStrides =
    (1..SemitoneScale.SEMITONES_PER_OCTAVE).filter { SemitoneScale.SEMITONES_PER_OCTAVE % it == 0 }

private val VerticalLine = Color.White.copy(alpha = 0.05f)
private val MajorRule = Color.White.copy(alpha = 0.13f)
private val MinorRule = Color.White.copy(alpha = 0.06f)

/**
 * Draws the grid that the game's sound editor shares between the pole-zero map and the frequency
 * response: an even faint line per semitone of the frequency axis, and horizontal rules that
 * alternate between bright and faint.
 * It has no labels and no octave marks.
 *
 * A semitone `f` sits at `width * max(1, zoom) * normX(f) - scroll`; [scroll] is in pixels of
 * this draw scope.
 */
internal fun DrawScope.drawAnalysisGrid(
    zoom: Int,
    scroll: Float = 0f,
) {
    if (size.width < 1f || size.height < 1f) return
    clipRect {
        drawSemitoneLines(size.width * max(1, zoom), scroll)
        drawRules(0.0, HORIZONTAL_STEP, MajorRule)
        drawRules(HORIZONTAL_STEP / 2.0, HORIZONTAL_STEP, MinorRule)
    }
}

/** One line per semitone across [effectiveWidth] pixels, shifted left by [scroll]. */
private fun DrawScope.drawSemitoneLines(
    effectiveWidth: Float,
    scroll: Float,
) {
    val first = SemitoneScale.firstVisibleSemitone
    val last = SemitoneScale.lastVisibleSemitone
    val spacing = effectiveWidth / density / (last - first)
    val stride =
        VerticalStrides.firstOrNull { spacing * it >= MIN_VERTICAL_SPACING }
            ?: VerticalStrides.last()
    for (semitone in first..last) {
        val frequency = SemitoneScale.semitoneToFrequency(semitone)
        val visible =
            semitone.mod(stride) == 0 &&
                frequency >= FilterFrequencyScale.MIN_FREQUENCY_HZ &&
                frequency <= FilterFrequencyScale.MAX_FREQUENCY_HZ
        if (visible) {
            val x = effectiveWidth * FilterFrequencyScale.frequencyToNormalizedX(frequency) - scroll
            val snapped = snapToPx(x / density.toDouble())
            drawLine(VerticalLine, Offset(snapped, 0f), Offset(snapped, size.height), density)
        }
    }
}

/** Horizontal rules every [step] dp from [start] dp. */
private fun DrawScope.drawRules(
    start: Double,
    step: Double,
    color: Color,
) {
    val height = size.height / density
    var y = start
    while (y <= height) {
        val snapped = snapToPx(y)
        drawLine(color, Offset(0f, snapped), Offset(size.width, snapped), density)
        y += step
    }
}

/** [value] in dp snapped to the dp grid, returned in pixels. */
private fun DrawScope.snapToPx(value: Double): Float =
    ((floor(value) + HALF_PIXEL) * density).toFloat()
