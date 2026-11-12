package io.github.xsyetopz.jagfx.ui.editor

import io.github.xsyetopz.jagfx.core.engine.Renderer
import io.github.xsyetopz.jagfx.core.model.Patch
import io.github.xsyetopz.jagfx.core.vorbis.JagVorbis
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** Renders [patch] to 16-bit PCM at [Renderer.SAMPLE_RATE]; see [Renderer.render]. */
internal fun interface PatchRenderer {
    fun render(
        patch: Patch,
        loopCount: Int,
        slot: Int?,
    ): ShortArray

    companion object {
        val Default: PatchRenderer = PatchRenderer(Renderer::render)
    }
}

/** Decodes [sample] to mono 16-bit PCM of `sampleCount` samples; see [JagVorbis.decodePcm]. */
internal fun interface SampleDecoder {
    fun decode(sample: JagVorbis): ShortArray

    companion object {
        val Default: SampleDecoder = SampleDecoder(JagVorbis::decodePcm)
    }
}

/** Playback failed, for example because no audio device is available. [message] is shown. */
class AudioOutputException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/** Plays mono 16-bit PCM. Each platform supplies one. */
fun interface AudioOutput {
    /**
     * Plays [pcm] when collected and emits progress in `0.0..1.0`. The flow completes when
     * playback ends. Cancelling the collector stops playback. The flow throws
     * [AudioOutputException] when the platform cannot play.
     */
    fun play(
        pcm: ShortArray,
        sampleRate: Int,
    ): Flow<Float>
}

/** A file the user picked to open. [path] is `null` where the platform has no file paths. */
class OpenedFile(
    val name: String,
    val path: String?,
    val bytes: ByteArray,
)

/**
 * A file the user picked to save to. [name] is shown to the user. [path] is `null` where a save
 * cannot be written to again, such as a browser download; the next save asks again.
 */
class SavedFile(
    val name: String,
    val path: String?,
)

/** A file operation failed. [message] is shown to the user. */
class PatchFileException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * The file types of the editor: a whole patch, one voice, one filter, or a sample. [extension] has
 * no leading dot. [description] names the type in dialogs; `.tone` and `.filter` are JagFx's own
 * layouts, not confirmed against the original tool.
 */
enum class PatchFileKind(
    val extension: String,
    val description: String,
) {
    /** A patch of ten voice slots and a loop. */
    Synth("synth", "synth file"),

    /** One voice, encoded as a voice slot of a `.synth` file. */
    Tone("tone", "JagFx tone file"),

    /** One filter, encoded as the filter block of a voice. */
    Filter("filter", "JagFx filter file"),

    /** A recorded sample, a document apart from the patch. */
    Sample("jagvorbis", "jagvorbis sample"),
}

/**
 * File access for the editor. Each platform supplies one. Methods throw [PatchFileException]
 * when an operation fails, and return `null` when the user cancels a picker.
 */
interface PatchFiles {
    /** Asks the user for a file of [kind] and reads it. */
    suspend fun pickAndRead(kind: PatchFileKind): OpenedFile?

    suspend fun read(path: String): ByteArray

    suspend fun write(
        path: String,
        bytes: ByteArray,
    )

    /** Asks the user where to save a file of [kind], suggesting [suggestedName], and writes. */
    suspend fun pickAndWrite(
        suggestedName: String,
        bytes: ByteArray,
        kind: PatchFileKind,
    ): SavedFile?
}

/** What the editor keeps across restarts: the side panel edge, the text size, and the theme. */
@Serializable
data class EditorPreferences(
    val panelSide: PanelSide = PanelSide.Right,
    val textScalePercent: Int = 100,
    val theme: ThemePreference = ThemePreference.System,
)

/**
 * Keeps [EditorPreferences] between runs. Each platform supplies one. [load] returns the defaults
 * when nothing is stored or the stored data is unreadable. [save] never throws; a failed write
 * loses the setting but not the session.
 */
interface Preferences {
    fun load(): EditorPreferences

    fun save(preferences: EditorPreferences)

    companion object {
        /** Stores nothing. */
        val None: Preferences =
            object : Preferences {
                override fun load(): EditorPreferences = EditorPreferences()

                override fun save(preferences: EditorPreferences) = Unit
            }
    }
}

/**
 * Read-only settings of the platform (EN 301 549 clause 11.7). Each platform reports what it
 * exposes and leaves the rest at the defaults.
 *
 * @property highContrast The user asked the platform for high contrast.
 * @property reducedMotion The user asked the platform for less motion.
 * @property textScalePercent The platform text size in percent, `100` when it is not exposed.
 */
interface PlatformSettings {
    val highContrast: Boolean
    val reducedMotion: Boolean
    val textScalePercent: Int

    companion object {
        /** Reports no platform preferences. */
        val None: PlatformSettings =
            object : PlatformSettings {
                override val highContrast: Boolean = false
                override val reducedMotion: Boolean = false
                override val textScalePercent: Int = 100
            }
    }
}

/** The theme to draw: a stored [System] becomes [ThemePreference.HighContrast] on a high-contrast platform. */
fun resolveTheme(
    stored: ThemePreference,
    platformHighContrast: Boolean,
): ThemePreference =
    if (stored == ThemePreference.System && platformHighContrast) {
        ThemePreference.HighContrast
    } else {
        stored
    }

/** The text size to draw: the in-app size times the platform size, within `100..200`. */
fun resolveTextScale(
    storedPercent: Int,
    platformPercent: Int,
): Int =
    (
        storedPercent.toLong() * platformPercent.coerceAtLeast(EditorState.MIN_TEXT_SCALE_PERCENT) /
            EditorState.MIN_TEXT_SCALE_PERCENT
    ).toInt()
        .coerceIn(EditorState.MIN_TEXT_SCALE_PERCENT, EditorState.MAX_TEXT_SCALE_PERCENT)
