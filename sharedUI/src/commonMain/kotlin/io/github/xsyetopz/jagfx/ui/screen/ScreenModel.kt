package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.runtime.Immutable
import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.core.model.Partial
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import io.github.xsyetopz.jagfx.core.model.Voice
import io.github.xsyetopz.jagfx.core.model.Waveform
import io.github.xsyetopz.jagfx.ui.canvas.PoleRef
import io.github.xsyetopz.jagfx.ui.canvas.TimeView
import io.github.xsyetopz.jagfx.ui.editor.RejectReason
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.editor.StatusHint
import io.github.xsyetopz.jagfx.ui.i18n.Strings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import kotlin.math.abs

/** `PitchOffset` units per semitone. */
private const val DETUNE_UNITS_PER_SEMITONE = 10

/** `true` for slots that draw voice timeline: envelope slots and output. */
internal val SignalChainSlot.isTimeDomain: Boolean
    get() = isEnvelope || this == SignalChainSlot.Output

/**
 * Zoom, scroll, selection, and snap toggle of one canvas. These never change patch.
 *
 * @property linked `true` when time-domain cell follows [ScreenViewState.timeView]; otherwise
 *     it follows [timeView].
 * @property timeView Own time view of time-domain cell that is not [linked].
 * @property panelOpen `true` while controls panel of cell is shown.
 * @property axisValues `true` to draw axis labels of enlarged cell.
 * @property gridLines `true` to draw grid lines of enlarged cell.
 * @property pointReadouts `true` to label every point of enlarged cell with its time and value.
 */
@Immutable
internal data class CanvasView(
    val zoom: Int = 1,
    val scroll: Float = 0f,
    val linked: Boolean = true,
    val timeView: TimeView = TimeView(),
    val selectedIndex: Int = -1,
    val snap: Boolean = false,
    val panelOpen: Boolean = false,
    val axisValues: Boolean = true,
    val gridLines: Boolean = true,
    val pointReadouts: Boolean = false,
)

/**
 * View state of screen that [EditorState] does not hold.
 *
 * @property openCell Cell that fills rack on its own, or `null` for whole rack.
 * @property timeView Time view that every linked time-domain cell shares.
 */
@Immutable
internal data class ScreenViewState(
    val canvases: Map<SignalChainSlot, CanvasView> = emptyMap(),
    val selectedPole: PoleRef? = null,
    val openCell: SignalChainSlot? = null,
    val timeView: TimeView = TimeView(),
) {
    fun canvas(slot: SignalChainSlot): CanvasView = canvases[slot] ?: CanvasView()

    fun withCanvas(
        slot: SignalChainSlot,
        view: CanvasView,
    ): ScreenViewState = copy(canvases = canvases + (slot to view))

    /** `true` when [slot] follows shared [timeView]; only time-domain slots link. */
    fun isLinked(slot: SignalChainSlot): Boolean = slot.isTimeDomain && canvas(slot).linked

    /** Time view [slot] shows: shared one when linked, otherwise its own. */
    fun timeViewOf(slot: SignalChainSlot): TimeView =
        if (isLinked(slot)) timeView else canvas(slot).timeView

    /** This state with [view] written to shared time view or to own one of [slot]. */
    fun withTimeView(
        slot: SignalChainSlot,
        view: TimeView,
    ): ScreenViewState =
        if (isLinked(slot)) {
            copy(timeView = view)
        } else {
            withCanvas(slot, canvas(slot).copy(timeView = view))
        }
}

/**
 * Envelope that canvas of [slot] shows for [voice], or `null` when it does not exist.
 * Filter modulation segments are shown as envelope with waveform [Waveform.Off].
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
 * One harmonic row. [index] is position in partial list; [partial] is `null` for
 * inactive row.
 */
internal data class PartialRow(
    val index: Int,
    val partial: Partial?,
)

/** [SynthLimits.MAX_PARTIALS] harmonic rows of [voice]. */
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

/** Status bar text of [hint], or empty string without hint. */
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

        StatusHint.NewPatch -> {
            strings[UiString.StatusNewPatch]
        }

        StatusHint.Edited -> {
            strings[UiString.StatusEdited]
        }

        StatusHint.Undone -> {
            strings[UiString.StatusUndone]
        }

        StatusHint.Redone -> {
            strings[UiString.StatusRedone]
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

/**
 * Text screen reader reads for plot of [slot]: its title, waveform, point count, and
 * lowest and highest target level in percent of full range. Only title for plot
 * without [envelope].
 */
internal fun plotSummary(
    slot: SignalChainSlot,
    envelope: Envelope?,
    strings: Strings,
): String {
    val title = strings[titleOf(slot)]
    val levels = envelope?.segments?.map { it.targetLevel * PERCENT / SynthLimits.U16_MAX }
    return if (envelope == null || levels.isNullOrEmpty()) {
        title
    } else {
        strings.format(
            UiString.PlotSummary,
            title,
            waveformName(strings, envelope.waveform),
            levels.size,
            levels.min(),
            levels.max(),
        )
    }
}

private const val PERCENT = 100
