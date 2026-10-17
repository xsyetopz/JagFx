package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.runtime.Immutable
import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.core.model.Partial
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import io.github.xsyetopz.jagfx.core.model.Voice
import io.github.xsyetopz.jagfx.core.model.Waveform
import io.github.xsyetopz.jagfx.ui.canvas.PoleRef
import io.github.xsyetopz.jagfx.ui.editor.RejectReason
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.editor.StatusHint
import io.github.xsyetopz.jagfx.ui.i18n.Strings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import kotlin.math.abs

/** `PitchOffset` units per semitone. */
private const val DETUNE_UNITS_PER_SEMITONE = 10

/** Zoom, scroll, selection, and snap toggle of one canvas. These never change the patch. */
@Immutable
internal data class CanvasView(
    val zoom: Int = 1,
    val scroll: Float = 0f,
    val selectedIndex: Int = -1,
    val snap: Boolean = false,
)

/**
 * View state of the screen that [EditorState] does not hold.
 *
 * @property openCell The cell that fills the rack on its own, or `null` for the whole rack.
 */
@Immutable
internal data class ScreenViewState(
    val canvases: Map<SignalChainSlot, CanvasView> = emptyMap(),
    val selectedPole: PoleRef? = null,
    val openCell: SignalChainSlot? = null,
) {
    fun canvas(slot: SignalChainSlot): CanvasView = canvases[slot] ?: CanvasView()

    fun withCanvas(
        slot: SignalChainSlot,
        view: CanvasView,
    ): ScreenViewState = copy(canvases = canvases + (slot to view))
}

/**
 * The envelope that the canvas of [slot] shows for [voice], or `null` when it does not exist.
 * The filter modulation segments are shown as an envelope with waveform [Waveform.Off].
 */
internal fun envelopeOf(
    voice: Voice?,
    slot: SignalChainSlot,
): Envelope? =
    when (slot) {
        SignalChainSlot.Pitch -> {
            voice?.frequencyEnvelope
        }

        SignalChainSlot.VibratoRate -> {
            voice?.pitchLfo?.rate
        }

        SignalChainSlot.VibratoDepth -> {
            voice?.pitchLfo?.depth
        }

        SignalChainSlot.Volume -> {
            voice?.amplitudeEnvelope
        }

        SignalChainSlot.TremoloRate -> {
            voice?.amplitudeLfo?.rate
        }

        SignalChainSlot.TremoloDepth -> {
            voice?.amplitudeLfo?.depth
        }

        SignalChainSlot.GapOff -> {
            voice?.gate?.gapOff
        }

        SignalChainSlot.GapOn -> {
            voice?.gate?.gapOn
        }

        SignalChainSlot.Filter -> {
            voice?.filter?.modulationEnvelope?.let {
                Envelope(Waveform.Off, 0, 0, it)
            }
        }

        SignalChainSlot.PoleZero, SignalChainSlot.Output, SignalChainSlot.Bode -> {
            null
        }
    }

/**
 * One harmonic row. [index] is the position in the partial list; [partial] is `null` for an
 * inactive row.
 */
internal data class PartialRow(
    val index: Int,
    val partial: Partial?,
)

/** The [SynthLimits.MAX_PARTIALS] harmonic rows of [voice]. */
internal fun partialRows(voice: Voice?): List<PartialRow> =
    List(SynthLimits.MAX_PARTIALS) { index -> PartialRow(index, voice?.partials?.getOrNull(index)) }

/** [units] of `PitchOffset` as signed semitones with one decimal, as `+2.5st`. */
internal fun semitones(units: Int): String {
    val sign = if (units < 0) "-" else "+"
    val whole = abs(units) / DETUNE_UNITS_PER_SEMITONE
    val tenth = abs(units) % DETUNE_UNITS_PER_SEMITONE
    return "$sign$whole.${tenth}st"
}

/** Panel title key of [slot]. */
internal fun titleOf(slot: SignalChainSlot): UiString =
    when (slot) {
        SignalChainSlot.Pitch -> UiString.SlotPitch
        SignalChainSlot.VibratoRate -> UiString.SlotVibratoRate
        SignalChainSlot.VibratoDepth -> UiString.SlotVibratoDepth
        SignalChainSlot.Volume -> UiString.SlotVolume
        SignalChainSlot.TremoloRate -> UiString.SlotTremoloRate
        SignalChainSlot.TremoloDepth -> UiString.SlotTremoloDepth
        SignalChainSlot.GapOff -> UiString.SlotGapOff
        SignalChainSlot.GapOn -> UiString.SlotGapOn
        SignalChainSlot.Filter -> UiString.SlotFilter
        SignalChainSlot.PoleZero -> UiString.SlotPoleZero
        SignalChainSlot.Output -> UiString.SlotOutput
        SignalChainSlot.Bode -> UiString.SlotBode
    }

private fun rejectKey(reason: RejectReason): UiString =
    when (reason) {
        RejectReason.DisabledSlot -> UiString.StatusRejectedDisabledSlot
        RejectReason.PitchWaveformOff -> UiString.StatusRejectedPitchWaveformOff
        RejectReason.MissingEnvelope -> UiString.StatusRejectedMissingEnvelope
        RejectReason.MissingFilter -> UiString.StatusRejectedMissingFilter
        RejectReason.NoBinaryEncoding -> UiString.StatusRejectedNoBinaryEncoding
    }

/** Status bar text of [hint], or an empty string without a hint. */
internal fun statusText(
    hint: StatusHint?,
    strings: Strings,
): String =
    when (hint) {
        null -> {
            ""
        }

        is StatusHint.Loaded -> {
            strings.format(UiString.StatusLoadedFile, hint.name)
        }

        is StatusHint.OpenFailed -> {
            strings.format(UiString.StatusCouldNotOpenFile, hint.name, hint.message)
        }

        is StatusHint.Saved -> {
            strings.format(UiString.StatusSaved, hint.name)
        }

        is StatusHint.Exported -> {
            strings.format(UiString.StatusExported, hint.name)
        }

        is StatusHint.SaveFailed -> {
            strings.format(UiString.StatusCouldNotSaveFile, hint.path, hint.message)
        }

        is StatusHint.PlaybackFailed -> {
            strings.format(UiString.StatusCouldNotPlay, hint.message)
        }

        is StatusHint.EditRejected -> {
            strings.format(
                rejectKey(hint.reason),
                hint.invariant?.name ?: "",
            )
        }
    }
