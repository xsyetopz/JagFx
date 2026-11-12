package io.github.xsyetopz.jagfx.ui.editor

import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.core.model.Patch
import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.core.model.Voice
import io.github.xsyetopz.jagfx.core.model.Waveform
import io.github.xsyetopz.jagfx.core.vorbis.JagVorbis

/**
 * Everything that can change [EditorState]. [reduce] applies intent.
 *
 * Edit intents address selected voice. Integer arguments outside their model range are
 * clamped; list indices outside list leave state unchanged.
 */
internal sealed interface EditorIntent

/** Edits of envelope shown for [target] in selected voice. */
internal sealed interface EnvelopeIntent : EditorIntent {
    val target: SignalChainSlot

    /**
     * Sets waveform. For first envelope of LFO or gate, [Waveform.Off] removes
     * pair and another waveform creates missing pair.
     */
    data class SetWaveform(
        override val target: SignalChainSlot,
        val waveform: Waveform,
    ) : EnvelopeIntent

    data class SetStartValue(
        override val target: SignalChainSlot,
        val value: Int,
    ) : EnvelopeIntent

    data class SetEndValue(
        override val target: SignalChainSlot,
        val value: Int,
    ) : EnvelopeIntent

    /** Inserts [segment] before [index]; index past end appends. */
    data class InsertSegment(
        override val target: SignalChainSlot,
        val index: Int,
        val segment: Segment,
    ) : EnvelopeIntent

    data class SetSegment(
        override val target: SignalChainSlot,
        val index: Int,
        val segment: Segment,
    ) : EnvelopeIntent

    data class RemoveSegment(
        override val target: SignalChainSlot,
        val index: Int,
    ) : EnvelopeIntent

    /** Replaces all segments, as delivered by canvas. Extra segments past limit are dropped. */
    data class SetSegments(
        override val target: SignalChainSlot,
        val segments: List<Segment>,
    ) : EnvelopeIntent
}

/** Scalar fields of selected voice, in milliseconds or percent. */
internal sealed interface VoiceIntent : EditorIntent {
    data class SetDuration(
        val ms: Int,
    ) : VoiceIntent

    data class SetOffset(
        val ms: Int,
    ) : VoiceIntent

    data class SetEchoDelay(
        val ms: Int,
    ) : VoiceIntent

    data class SetEchoFeedback(
        val percent: Int,
    ) : VoiceIntent
}

/** Partials of selected voice. [index] is position in partial list. */
internal sealed interface PartialIntent : EditorIntent {
    /** Removes partial [index] when it exists, else appends 100 % partial. */
    data class TogglePartial(
        val index: Int,
    ) : PartialIntent

    data class SetAmplitude(
        val index: Int,
        val percent: Int,
    ) : PartialIntent

    data class SetPitchOffset(
        val index: Int,
        val decicents: Int,
    ) : PartialIntent

    data class SetDelay(
        val index: Int,
        val ms: Int,
    ) : PartialIntent
}

/** Filter of selected voice. Channel `0` is feedforward, `1` is feedback. */
internal sealed interface FilterIntent : EditorIntent {
    /** Replaces filter, for example with imported one; `null` removes it. */
    data class SetFilter(
        val filter: Filter?,
    ) : FilterIntent

    /**
     * Turns filter on or off. Off keeps filter so that on brings it back; on without
     * kept filter creates one with single zero feedforward pole.
     */
    data class SetEnabled(
        val enabled: Boolean,
    ) : FilterIntent

    /** Adds zero poles or truncates. Creates filter when needed; no poles removes it. */
    data class SetPoleCount(
        val channel: Int,
        val count: Int,
    ) : FilterIntent

    /** Sets unity gain [index] (`0` or `1`). */
    data class SetUnityGain(
        val index: Int,
        val value: Int,
    ) : FilterIntent

    /** Sets pole [pole] of [channel] at [phaseIndex] (`0` = start, `1` = end). */
    data class SetCoefficient(
        val channel: Int,
        val pole: Int,
        val phaseIndex: Int,
        val phase: Int,
        val magnitude: Int,
    ) : FilterIntent
}

/** Voice slot selection, enabling, copy and paste. */
internal sealed interface SlotIntent : EditorIntent {
    /** Selects [slot]. In copy mode with copied voice, pastes it into another slot first. */
    data class SelectVoice(
        val slot: Int,
    ) : SlotIntent

    /** Puts [voice] into [slot], for example imported tone. */
    data class SetVoice(
        val slot: Int,
        val voice: Voice,
    ) : SlotIntent

    /** Enables [slot] with default voice, or empties it. */
    data class SetVoiceEnabled(
        val slot: Int,
        val enabled: Boolean,
    ) : SlotIntent

    /** Copies selected voice and enters copy mode. */
    data object CopyVoice : SlotIntent

    /** Empties selected slot. */
    data object ResetVoice : SlotIntent

    data class SelectTarget(
        val target: SignalChainSlot,
    ) : SlotIntent

    /** Moves selected target by [offset] in [SignalChainSlot.envelopeChain], wrapping. */
    data class SelectTargetByOffset(
        val offset: Int,
    ) : SlotIntent
}

/** Loop region of patch. */
internal sealed interface LoopIntent : EditorIntent {
    data class SetLoopBegin(
        val ms: Int,
    ) : LoopIntent

    data class SetLoopEnd(
        val ms: Int,
    ) : LoopIntent
}

/**
 * Header-only edits of open sample; without open sample they leave state unchanged.
 * Arguments outside their range are clamped; see [reduce].
 */
internal sealed interface SampleIntent : EditorIntent {
    /** Trims end of sample to [count] samples, at most count at load time. */
    data class SetSampleCount(
        val count: Int,
    ) : SampleIntent

    data class SetLoopBegin(
        val sample: Int,
    ) : SampleIntent

    data class SetLoopEnd(
        val sample: Int,
    ) : SampleIntent

    data class SetPingPong(
        val enabled: Boolean,
    ) : SampleIntent
}

/** View and playback options. They never change patch. */
internal sealed interface ViewIntent : EditorIntent {
    data class SetTrueWave(
        val enabled: Boolean,
    ) : ViewIntent

    data class SetPlaySingleVoice(
        val enabled: Boolean,
    ) : ViewIntent

    data class SetLooping(
        val enabled: Boolean,
    ) : ViewIntent

    data class SetLoopCount(
        val count: Int,
    ) : ViewIntent

    data class SetPanelSide(
        val side: PanelSide,
    ) : ViewIntent

    /** Sets in-app text size; reducer clamps [percent] to `100..200`. */
    data class SetTextScale(
        val percent: Int,
    ) : ViewIntent

    data class SetTheme(
        val theme: ThemePreference,
    ) : ViewIntent

    /** Shows Main rack, Filter column, or Both; patch and history stay as they are. */
    data class SetMode(
        val mode: RackMode,
    ) : ViewIntent
}

/** Undo, redo, and gesture grouping. */
internal sealed interface HistoryIntent : EditorIntent {
    data object Undo : HistoryIntent

    data object Redo : HistoryIntent

    /** Starts gesture: edits until matching [EndGesture] form one undo step. */
    data object BeginGesture : HistoryIntent

    data object EndGesture : HistoryIntent
}

/** Outcomes of file I/O and playback, reported by ViewModel. */
internal sealed interface ResultIntent : EditorIntent {
    /** File opened. [path] is `null` for patch without file. */
    data class Loaded(
        val name: String,
        val path: String?,
        val patch: Patch,
    ) : ResultIntent

    data class LoadFailed(
        val name: String,
        val message: String,
    ) : ResultIntent

    /** [patch] was written to [path]. */
    data class Saved(
        val name: String,
        val path: String?,
        val patch: Patch,
    ) : ResultIntent

    data class SaveFailed(
        val path: String,
        val message: String,
    ) : ResultIntent

    /** Tone or filter was written to file named [name]. */
    data class Exported(
        val name: String,
    ) : ResultIntent

    /** Sample opened; [pcm] is its decoded audio. [path] is `null` for sample without file. */
    class SampleLoaded(
        val name: String,
        val path: String?,
        val sample: JagVorbis,
        val pcm: ShortArray,
    ) : ResultIntent

    /** [sample] was written to [path]. */
    data class SampleSaved(
        val name: String,
        val path: String?,
        val sample: JagVorbis,
    ) : ResultIntent

    data object PlaybackStarted : ResultIntent

    data object PlaybackStopped : ResultIntent

    data class PlaybackFailed(
        val message: String,
    ) : ResultIntent

    /** Playback progress in `0.0..1.0`. */
    data class PlaybackPosition(
        val fraction: Float,
    ) : ResultIntent

    /** Preview render finished with 16-bit [pcm] samples. */
    class PreviewRendered(
        val pcm: ShortArray,
    ) : ResultIntent
}

/** Whole-document changes that need no side effect. */
internal sealed interface DocumentIntent : EditorIntent {
    /** Replaces patch with new one that has tone 1 on, and opens editor. */
    data object NewPatch : DocumentIntent
}

/**
 * Requests with side effects. ViewModel performs them and reports [ResultIntent];
 * [reduce] leaves state unchanged.
 */
internal sealed interface RequestIntent : EditorIntent {
    data object Open : RequestIntent

    /** Opens `.synth` file at [path] without picker. */
    data class OpenPath(
        val path: String,
    ) : RequestIntent

    data object Save : RequestIntent

    data object SaveAs : RequestIntent

    /** Reads `.tone` file into selected slot. */
    data object ImportTone : RequestIntent

    /** Writes selected voice to `.tone` file. */
    data object ExportTone : RequestIntent

    /** Reads `.filter` file into filter of selected voice. */
    data object ImportFilter : RequestIntent

    /** Writes filter of selected voice to `.filter` file. */
    data object ExportFilter : RequestIntent

    /** Reads `.jagvorbis` file into sample document. */
    data object OpenSample : RequestIntent

    data object SaveSample : RequestIntent

    data object SaveSampleAs : RequestIntent

    /** Stops playback and closes sample; patch is shown again. */
    data object CloseSample : RequestIntent

    /** Stops playback if playing, then starts it again. Plays sample while one is open. */
    data object TogglePlay : RequestIntent

    data object Stop : RequestIntent
}
