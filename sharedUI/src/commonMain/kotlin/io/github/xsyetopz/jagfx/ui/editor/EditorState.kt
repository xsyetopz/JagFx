package io.github.xsyetopz.jagfx.ui.editor

import io.github.xsyetopz.jagfx.core.format.PatchInvariant
import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.core.model.Loop
import io.github.xsyetopz.jagfx.core.model.Milliseconds
import io.github.xsyetopz.jagfx.core.model.Patch
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import io.github.xsyetopz.jagfx.core.model.Voice
import io.github.xsyetopz.jagfx.core.vorbis.JagVorbis

/** Why the reducer kept the old patch for an edit. */
internal enum class RejectReason {
    /** The selected voice slot is empty. */
    DisabledSlot,

    /** The pitch envelope must keep an oscillator waveform (invariant I1). */
    PitchWaveformOff,

    /** The edit addresses an LFO, gate, or filter modulation envelope that does not exist. */
    MissingEnvelope,

    /** The edit addresses a filter that does not exist. */
    MissingFilter,

    /** The edited patch has no binary encoding; [StatusHint.EditRejected.invariant] says why. */
    NoBinaryEncoding,
}

/** One-line message for the status bar. */
internal sealed interface StatusHint {
    data class Loaded(
        val name: String,
    ) : StatusHint

    data class OpenFailed(
        val name: String,
        val message: String,
    ) : StatusHint

    data class Saved(
        val name: String,
    ) : StatusHint

    data class Exported(
        val name: String,
    ) : StatusHint

    data class SaveFailed(
        val path: String,
        val message: String,
    ) : StatusHint

    data class PlaybackFailed(
        val message: String,
    ) : StatusHint

    /** [invariant] is set when [reason] is [RejectReason.NoBinaryEncoding]. */
    data class EditRejected(
        val reason: RejectReason,
        val invariant: PatchInvariant? = null,
    ) : StatusHint
}

/**
 * Rendered output waveform for the output canvas, as samples in `-1.0..1.0`.
 *
 * Not a data class: two previews are equal only when they are the same render.
 */
internal class Preview private constructor(
    val samples: FloatArray,
) {
    companion object {
        private const val PCM_SCALE = 32768f

        val Empty: Preview = Preview(FloatArray(0))

        /** Scales 16-bit [pcm] samples to `-1.0..1.0`. */
        fun fromPcm(pcm: ShortArray): Preview =
            Preview(FloatArray(pcm.size) { pcm[it] / PCM_SCALE })
    }
}

/**
 * An open `.jagvorbis` sample, a document apart from the patch.
 *
 * @property sample The sample with its edited header fields.
 * @property savedSample The sample as last loaded or saved; [isDirty] compares against it.
 * @property preview The decoded audio of the sample as loaded, `loadedSampleCount` samples.
 * @property loadedSampleCount The sample count at load time: the end trim cannot exceed it.
 */
internal data class SampleDocument(
    val name: String,
    val path: String?,
    val sample: JagVorbis,
    val savedSample: JagVorbis,
    val preview: Preview,
    val loadedSampleCount: Int,
) {
    val isDirty: Boolean get() = sample != savedSample
}

/**
 * Complete editor state. [reduce] is the only function that makes a new state from an intent.
 *
 * @property savedPatch The patch as last loaded or saved; [isDirty] compares against it.
 * @property playbackPosition Playback progress in `0.0..1.0`.
 * @property loopCount Loop repetitions in `0..MAX_LOOP_COUNT`; `0` loops until stopped.
 * @property bypassedFilters Per voice slot, the filter that was turned off, for turning it on.
 * @property sample The open sample, or `null`. The patch stays intact while a sample is open.
 */
internal data class EditorState(
    val patch: Patch = Patch.Empty,
    val savedPatch: Patch = Patch.Empty,
    val history: History = History(),
    val selectedVoice: Int = 0,
    val selectedTarget: SignalChainSlot = SignalChainSlot.Pitch,
    val fileName: String = DEFAULT_FILE_NAME,
    val filePath: String? = null,
    val isPlaying: Boolean = false,
    val playbackPosition: Float = 0f,
    val isLooping: Boolean = false,
    val loopCount: Int = 0,
    val playSingleVoice: Boolean = false,
    val trueWave: Boolean = true,
    val copiedVoice: Voice? = null,
    val isCopyMode: Boolean = false,
    val bypassedFilters: List<Filter?> = List(SynthLimits.VOICE_SLOTS) { null },
    val statusHint: StatusHint? = null,
    val preview: Preview = Preview.Empty,
    val sample: SampleDocument? = null,
) {
    val isDirty: Boolean get() = patch != savedPatch

    /** The voice in [selectedVoice], or `null` when the slot is empty. */
    val voice: Voice? get() = patch.voices[selectedVoice]

    val canUndo: Boolean get() = history.canUndo

    val canRedo: Boolean get() = history.canRedo

    /** Loop passes for playback: `1` without looping, [INFINITE_LOOP_PASSES] for an endless loop. */
    val effectiveLoopCount: Int
        get() =
            when {
                !isLooping -> 1
                loopCount == 0 -> INFINITE_LOOP_PASSES
                else -> loopCount
            }

    /**
     * Loop region for playback. When looping and the patch loop is empty, the loop spans the
     * whole patch: `0` to the latest voice end (`duration + offset`).
     */
    val playbackLoop: Loop
        get() =
            if (isLooping && patch.loop.begin.value >= patch.loop.end.value) {
                val end =
                    patch.voices.maxOfOrNull {
                        (it?.duration?.value ?: 0) +
                            (it?.offset?.value ?: 0)
                    }
                Loop(Milliseconds(0), Milliseconds((end ?: 0).coerceAtMost(SynthLimits.U16_MAX)))
            } else {
                patch.loop
            }

    /** Voice slot to render alone, or `null` to render the whole patch. */
    val playbackSlot: Int? get() = if (playSingleVoice) selectedVoice else null

    /** The patch to play: [patch] with [playbackLoop]. */
    fun playbackPatch(): Patch = patch.copy(loop = playbackLoop)

    /**
     * Window title: `JagFx: name` or `JagFx: path`, with ` *` when dirty. A file path under
     * [home] shows `~` for the home prefix. An open [sample] takes the place of the patch.
     */
    fun windowTitle(home: String?): String {
        val path = sample?.path ?: filePath.takeIf { sample == null }
        val name = sample?.name ?: fileName
        val shown = path?.let { abbreviateHome(it, home) } ?: name
        return "JagFx: $shown${if (sample?.isDirty ?: isDirty) " *" else ""}"
    }

    companion object {
        const val DEFAULT_FILE_NAME: String = "untitled"
        const val MAX_LOOP_COUNT: Int = 999
        const val INFINITE_LOOP_PASSES: Int = 50
    }
}

private fun abbreviateHome(
    path: String,
    home: String?,
): String {
    val prefix = home?.trimEnd('/', '\\')
    val underHome =
        !prefix.isNullOrEmpty() &&
            path.startsWith(prefix) &&
            (
                path.length == prefix.length || path[prefix.length] == '/' ||
                    path[prefix.length] == '\\'
            )
    return if (underHome) "~" + path.substring(prefix.length) else path
}

/** File name of [path] without directory and extension. */
internal fun fileNameOf(path: String): String {
    val base = path.substringAfterLast('/').substringAfterLast('\\')
    val dot = base.lastIndexOf('.')
    return if (dot > 0) base.substring(0, dot) else base
}
