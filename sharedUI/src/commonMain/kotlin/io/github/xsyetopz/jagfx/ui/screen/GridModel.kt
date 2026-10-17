package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.ui.graphics.Color
import io.github.xsyetopz.jagfx.ui.canvas.ENVELOPE_POSITION_SPAN
import io.github.xsyetopz.jagfx.ui.canvas.FilterResponse
import io.github.xsyetopz.jagfx.ui.canvas.Viewport
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.theme.SynthPalette

/**
 * The envelope cells of the rack, as in the Scala JagFX: one column each for pitch, volume, and
 * the filter sweep, with the modulation or gate pair of that column under it.
 */
internal val RackRows: List<List<SignalChainSlot>> =
    listOf(
        listOf(SignalChainSlot.Pitch, SignalChainSlot.Volume, SignalChainSlot.Filter),
        listOf(SignalChainSlot.VibratoRate, SignalChainSlot.TremoloRate, SignalChainSlot.GapOff),
        listOf(SignalChainSlot.VibratoDepth, SignalChainSlot.TremoloDepth, SignalChainSlot.GapOn),
    )

/** The filter column right of the rack: the pole-zero map above the frequency response. */
internal val FilterColumn: List<SignalChainSlot> =
    listOf(SignalChainSlot.PoleZero, SignalChainSlot.Bode)

/** Every cell of the rack in reading order; the output spans the row under the envelopes. */
internal val RackCells: List<SignalChainSlot> =
    RackRows.flatten() + SignalChainSlot.Output + FilterColumn

/**
 * The first envelope of the LFO or gate pair that [slot] belongs to, whose waveform creates and
 * removes the pair; `null` for a slot outside a pair.
 */
internal fun pairFirstOf(slot: SignalChainSlot): SignalChainSlot? =
    when (slot) {
        SignalChainSlot.VibratoRate, SignalChainSlot.VibratoDepth -> SignalChainSlot.VibratoRate
        SignalChainSlot.TremoloRate, SignalChainSlot.TremoloDepth -> SignalChainSlot.TremoloRate
        SignalChainSlot.GapOff, SignalChainSlot.GapOn -> SignalChainSlot.GapOff
        else -> null
    }

/** The plot color of [slot]: envelopes orange, the output green, and the filter plots blue. */
internal fun slotColor(slot: SignalChainSlot): Color =
    when (slot) {
        SignalChainSlot.Output -> SynthPalette.Output
        SignalChainSlot.PoleZero, SignalChainSlot.Bode -> SynthPalette.Filter
        else -> SynthPalette.Envelope
    }

/** Resonance in dB of a raw pole magnitude, as the filter computes it. */
internal fun magnitudeToDb(magnitude: Int): Double = magnitude * FilterResponse.MAGNITUDE_DB_SCALE

/** Time in ms of an envelope [position], in 1/65536 of the voice, in a voice [durationMs] long. */
internal fun positionToMs(
    position: Float,
    durationMs: Int,
): Float = position / ENVELOPE_POSITION_SPAN * durationMs

/**
 * The part of the time axis, as fractions of the voice, that a plot [plotWidth] px wide shows at
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
