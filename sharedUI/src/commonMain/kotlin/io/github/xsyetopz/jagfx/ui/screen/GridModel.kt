package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.ui.graphics.Color
import io.github.xsyetopz.jagfx.ui.canvas.ENVELOPE_POSITION_SPAN
import io.github.xsyetopz.jagfx.ui.canvas.FilterResponse
import io.github.xsyetopz.jagfx.ui.canvas.Viewport
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.theme.SynthPalette

/**
 * Rows of rack, as in original: Freq, Amp, and Output, each with its base cell and
 * its modulation or gate pair.
 */
internal val RackRows: List<List<SignalChainSlot>> =
    listOf(
        listOf(SignalChainSlot.Pitch, SignalChainSlot.VibratoRate, SignalChainSlot.VibratoDepth),
        listOf(SignalChainSlot.Volume, SignalChainSlot.TremoloRate, SignalChainSlot.TremoloDepth),
        listOf(SignalChainSlot.Output, SignalChainSlot.GapOff, SignalChainSlot.GapOn),
    )

/**
 * Filter column right of rack: graph of poles and zeros, transition curve, and
 * frequency response.
 */
internal val FilterColumn: List<SignalChainSlot> =
    listOf(SignalChainSlot.PoleZero, SignalChainSlot.Filter, SignalChainSlot.Bode)

/** Every cell of rack in reading order, each slot once: rows, then filter column. */
internal val RackCells: List<SignalChainSlot> = RackRows.flatten() + FilterColumn

/**
 * First envelope of LFO or gate pair that [slot] belongs to, whose waveform creates and
 * removes pair; `null` for slot outside pair.
 */
internal fun pairFirstOf(slot: SignalChainSlot): SignalChainSlot? =
    when (slot) {
        SignalChainSlot.VibratoRate, SignalChainSlot.VibratoDepth -> SignalChainSlot.VibratoRate
        SignalChainSlot.TremoloRate, SignalChainSlot.TremoloDepth -> SignalChainSlot.TremoloRate
        SignalChainSlot.GapOff, SignalChainSlot.GapOn -> SignalChainSlot.GapOff
        else -> null
    }

/** Plot color of [slot]: envelopes orange, output green, and filter plots blue. */
internal fun slotColor(
    slot: SignalChainSlot,
    palette: SynthPalette,
): Color =
    when (slot) {
        SignalChainSlot.Output -> palette.output
        SignalChainSlot.PoleZero, SignalChainSlot.Bode -> palette.filter
        else -> palette.envelope
    }

/** Resonance in dB of raw pole magnitude, as filter computes it. */
internal fun magnitudeToDb(magnitude: Int): Double = magnitude * FilterResponse.MAGNITUDE_DB_SCALE

/** Time in ms of envelope [position], in 1/65536 of voice, in voice [durationMs] long. */
internal fun positionToMs(
    position: Float,
    durationMs: Int,
): Float = position / ENVELOPE_POSITION_SPAN * durationMs

/**
 * Part of time axis, as fractions of voice, that plot [plotWidth] px wide shows at
 * [viewport].
 */
internal fun visibleWindow(
    viewport: Viewport,
    plotWidth: Float,
): ClosedFloatingPointRange<Float> {
    val total = plotWidth * viewport.zoom.coerceAtLeast(1)
    if (total <= 0f) return 0f..1f
    val start = (viewport.scrollOffset / total).coerceIn(0f, 1f)
    return start..((viewport.scrollOffset + plotWidth) / total).coerceIn(start, 1f)
}
