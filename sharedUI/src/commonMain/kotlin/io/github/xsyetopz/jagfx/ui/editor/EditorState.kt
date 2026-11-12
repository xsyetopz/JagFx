package io.github.xsyetopz.jagfx.ui.editor

import io.github.xsyetopz.jagfx.core.format.PatchInvariant
import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.core.model.Loop
import io.github.xsyetopz.jagfx.core.model.Milliseconds
import io.github.xsyetopz.jagfx.core.model.Patch
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import io.github.xsyetopz.jagfx.core.model.Voice
import io.github.xsyetopz.jagfx.core.vorbis.JagVorbis

/** Why reducer kept old patch for edit. */
internal enum class RejectReason {
    /** Selected voice slot is empty. */
    DisabledSlot,

    /** Pitch envelope must keep oscillator waveform (invariant I1). */
    PitchWaveformOff,

    /** Edit addresses LFO, gate, or filter modulation envelope that does not exist. */
    MissingEnvelope,

    /** Edit addresses filter that does not exist. */
    MissingFilter,

    /** Edited patch has no binary encoding; [StatusHint.EditRejected.invariant] says why. */
    NoBinaryEncoding,
}

/** One-line message for status bar. */
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

    /** New patch with tone 1 on replaced document. */
    data object NewPatch : StatusHint

    /** Accepted edit; undo reverts it. */
    data object Edited : StatusHint

    data object Undone : StatusHint

    data object Redone : StatusHint

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
 * Rendered output waveform for output canvas, as samples in `-1.0..1.0`.
 *
 * Not data class: two previews are equal only when they are same render.
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
 * Open `.jagvorbis` sample, document apart from patch.
 *
 * @property sample Sample with its edited header fields.
 * @property savedSample Sample as last loaded or saved; [isDirty] compares against it.
 * @property preview Decoded audio of sample as loaded, `loadedSampleCount` samples.
 * @property loadedSampleCount Sample count at load time: end trim cannot exceed it.
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

/** Window edge properties panel sits on, so either hand can keep it near mouse. */
enum class PanelSide { Left, Right }

/** Which parts of rack editor shows. */
internal enum class RackMode {
    /** Three envelope rows. */
    Main,

    /** Filter column at full size. */
    Filter,

    /** Rows and filter column side by side. */
    Both,
}

/** Color theme user picked. [System] follows platform, for example high contrast. */
enum class ThemePreference { System, Classic, Light, HighContrast }

/**
 * Complete editor state. [reduce] is only function that makes new state from intent.
 *
 * @property savedPatch Patch as last loaded or saved; [isDirty] compares against it.
 * @property playbackPosition Playback progress in `0.0..1.0`.
 * @property loopCount Loop repetitions in `0..MAX_LOOP_COUNT`; `0` loops until stopped.
 * @property bypassedFilters Per voice slot, filter that was turned off, for turning it on.
 * @property sample Open sample, or `null`. Patch stays intact while sample is open.
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
    val panelSide: PanelSide = PanelSide.Right,
    val textScalePercent: Int = MIN_TEXT_SCALE_PERCENT,
    val theme: ThemePreference = ThemePreference.System,
    val mode: RackMode = RackMode.Main,
) {
    val isDirty: Boolean get() = patch != savedPatch

    /** Voice in [selectedVoice], or `null` when slot is empty. */
    val voice: Voice? get() = patch.voices[selectedVoice]

    val canUndo: Boolean get() = history.canUndo

    val canRedo: Boolean get() = history.canRedo

    /** Loop passes for playback: `1` without looping, [INFINITE_LOOP_PASSES] for endless loop. */
    val effectiveLoopCount: Int
        get() =
            when {
                !isLooping -> 1
                loopCount == 0 -> INFINITE_LOOP_PASSES
                else -> loopCount
            }

    /**
     * Loop region for playback. When looping and patch loop is empty, loop spans
     * whole patch: `0` to latest voice end (`duration + offset`).
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

    /** Voice slot to render alone, or `null` to render whole patch. */
    val playbackSlot: Int? get() = if (playSingleVoice) selectedVoice else null

    /** Patch to play: [patch] with [playbackLoop]. */
    fun playbackPatch(): Patch = patch.copy(loop = playbackLoop)

    /**
     * Window title: `JagFx: name` or `JagFx: path`, with ` *` when dirty. File path under
     * [home] shows `~` for home prefix. Open [sample] takes place of patch.
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

        /** In-app text size range, in percent of normal size. */
        const val MIN_TEXT_SCALE_PERCENT: Int = 100
        const val MAX_TEXT_SCALE_PERCENT: Int = 200

        /** Text sizes that menus offer, in percent. */
        val TEXT_SCALE_STEPS: List<Int> =
            (MIN_TEXT_SCALE_PERCENT..MAX_TEXT_SCALE_PERCENT step TEXT_SCALE_STEP).toList()
        private const val TEXT_SCALE_STEP: Int = 25
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
