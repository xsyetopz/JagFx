package io.github.xsyetopz.jagfx.ui.editor

import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.core.model.Patch
import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.core.model.Voice
import io.github.xsyetopz.jagfx.core.model.Waveform
import io.github.xsyetopz.jagfx.core.vorbis.JagVorbis

/**
 * Everything that can change an [EditorState]. [reduce] applies an intent.
 *
 * Edit intents address the selected voice. Integer arguments outside their model range are
 * clamped; list indices outside the list leave the state unchanged.
 */
internal sealed interface EditorIntent

/** Edits of the envelope shown for [target] in the selected voice. */
internal sealed interface EnvelopeIntent : EditorIntent {
    val target: SignalChainSlot

    /**
     * Sets the waveform. For the first envelope of an LFO or gate, [Waveform.Off] removes the
     * pair and another waveform creates a missing pair.
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

    /** Inserts [segment] before [index]; an index past the end appends. */
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

    /** Replaces all segments, as delivered by a canvas. Extra segments past the limit are dropped. */
    data class SetSegments(
        override val target: SignalChainSlot,
        val segments: List<Segment>,
    ) : EnvelopeIntent
}

/** Scalar fields of the selected voice, in milliseconds or percent. */
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

/** Partials of the selected voice. [index] is the position in the partial list. */
internal sealed interface PartialIntent : EditorIntent {
    /** Removes partial [index] when it exists, else appends a 100 % partial. */
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

/** Filter of the selected voice. Channel `0` is feedforward, `1` is feedback. */
internal sealed interface FilterIntent : EditorIntent {
    /** Replaces the filter, for example with an imported one; `null` removes it. */
    data class SetFilter(
        val filter: Filter?,
    ) : FilterIntent

    /**
     * Turns the filter on or off. Off keeps the filter so that on brings it back; on without a
     * kept filter creates one with a single zero feedforward pole.
     */
    data class SetEnabled(
        val enabled: Boolean,
    ) : FilterIntent

    /** Adds zero poles or truncates. Creates a filter when needed; no poles removes it. */
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
    /** Selects [slot]. In copy mode with a copied voice, pastes it into another slot first. */
    data class SelectVoice(
        val slot: Int,
    ) : SlotIntent

    /** Puts [voice] into [slot], for example an imported tone. */
    data class SetVoice(
        val slot: Int,
        val voice: Voice,
    ) : SlotIntent

    /** Enables [slot] with a default voice, or empties it. */
    data class SetVoiceEnabled(
        val slot: Int,
        val enabled: Boolean,
    ) : SlotIntent

    /** Copies the selected voice and enters copy mode. */
    data object CopyVoice : SlotIntent

    /** Empties the selected slot. */
    data object ResetVoice : SlotIntent

    data class SelectTarget(
        val target: SignalChainSlot,
    ) : SlotIntent

    /** Moves the selected target by [offset] in [SignalChainSlot.envelopeChain], wrapping. */
    data class SelectTargetByOffset(
        val offset: Int,
    ) : SlotIntent
}

/** Loop region of the patch. */
internal sealed interface LoopIntent : EditorIntent {
    data class SetLoopBegin(
        val ms: Int,
    ) : LoopIntent

    data class SetLoopEnd(
        val ms: Int,
    ) : LoopIntent
}

/**
 * Header-only edits of the open sample; without an open sample they leave the state unchanged.
 * Arguments outside their range are clamped; see [reduce].
 */
internal sealed interface SampleIntent : EditorIntent {
    /** Trims the end of the sample to [count] samples, at most the count at load time. */
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

/** View and playback options. They never change the patch. */
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
}

/** Undo, redo, and gesture grouping. */
internal sealed interface HistoryIntent : EditorIntent {
    data object Undo : HistoryIntent

    data object Redo : HistoryIntent

    /** Starts a gesture: edits until the matching [EndGesture] form one undo step. */
    data object BeginGesture : HistoryIntent

    data object EndGesture : HistoryIntent
}

/** Outcomes of file I/O and playback, reported by the ViewModel. */
internal sealed interface ResultIntent : EditorIntent {
    /** A file opened. [path] is `null` for a patch without a file. */
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

    /** A tone or filter was written to a file named [name]. */
    data class Exported(
        val name: String,
    ) : ResultIntent

    /** A sample opened; [pcm] is its decoded audio. [path] is `null` for a sample without a file. */
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

    /** A preview render finished with 16-bit [pcm] samples. */
    class PreviewRendered(
        val pcm: ShortArray,
    ) : ResultIntent
}

/**
 * Requests with side effects. The ViewModel performs them and reports a [ResultIntent];
 * [reduce] leaves the state unchanged.
 */
internal sealed interface RequestIntent : EditorIntent {
    data object Open : RequestIntent

    data object Save : RequestIntent

    data object SaveAs : RequestIntent

    /** Reads a `.tone` file into the selected slot. */
    data object ImportTone : RequestIntent

    /** Writes the selected voice to a `.tone` file. */
    data object ExportTone : RequestIntent

    /** Reads a `.filter` file into the filter of the selected voice. */
    data object ImportFilter : RequestIntent

    /** Writes the filter of the selected voice to a `.filter` file. */
    data object ExportFilter : RequestIntent

    /** Reads a `.jagvorbis` file into the sample document. */
    data object OpenSample : RequestIntent

    data object SaveSample : RequestIntent

    data object SaveSampleAs : RequestIntent

    /** Stops playback and closes the sample; the patch is shown again. */
    data object CloseSample : RequestIntent

    /** Stops playback if playing, then starts it again. Plays the sample while one is open. */
    data object TogglePlay : RequestIntent

    data object Stop : RequestIntent
}
