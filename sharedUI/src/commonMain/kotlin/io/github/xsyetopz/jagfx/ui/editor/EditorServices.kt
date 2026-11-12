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
     * Plays [pcm] when collected and emits progress in `0.0..1.0`. Flow completes when
     * playback ends. Cancelling collector stops playback. Flow throws
     * [AudioOutputException] when platform cannot play.
     */
    fun play(
        pcm: ShortArray,
        sampleRate: Int,
    ): Flow<Float>
}

/** File user picked to open. [path] is `null` where platform has no file paths. */
class OpenedFile(
    val name: String,
    val path: String?,
    val bytes: ByteArray,
)

/**
 * File user picked to save to. [name] is shown to user. [path] is `null` where save
 * cannot be written to again, such as browser download; next save asks again.
 */
class SavedFile(
    val name: String,
    val path: String?,
)

/** File operation failed. [message] is shown to user. */
class PatchFileException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * File types of editor: whole patch, one voice, one filter, or sample. [extension] has
 * no leading dot. [description] names type in dialogs; `.tone` and `.filter` are JagFx's own
 * layouts, not confirmed against original tool.
 */
enum class PatchFileKind(
    val extension: String,
    val description: String,
) {
    /** Patch of ten voice slots and loop. */
    Synth("synth", "synth file"),

    /** One voice, encoded as voice slot of `.synth` file. */
    Tone("tone", "JagFx tone file"),

    /** One filter, encoded as filter block of voice. */
    Filter("filter", "JagFx filter file"),

    /** Recorded sample, document apart from patch. */
    Sample("jagvorbis", "jagvorbis sample"),
}

/**
 * File access for editor. Each platform supplies one. Methods throw [PatchFileException]
 * when operation fails, and return `null` when user cancels picker.
 */
interface PatchFiles {
    /** Asks user for file of [kind] and reads it. */
    suspend fun pickAndRead(kind: PatchFileKind): OpenedFile?

    suspend fun read(path: String): ByteArray

    suspend fun write(
        path: String,
        bytes: ByteArray,
    )

    /** Asks user where to save file of [kind], suggesting [suggestedName], and writes. */
    suspend fun pickAndWrite(
        suggestedName: String,
        bytes: ByteArray,
        kind: PatchFileKind,
    ): SavedFile?
}

/** What editor keeps across restarts: side panel edge, text size, and theme. */
@Serializable
data class EditorPreferences(
    val panelSide: PanelSide = PanelSide.Right,
    val textScalePercent: Int = 100,
    val theme: ThemePreference = ThemePreference.System,
)

/**
 * Keeps [EditorPreferences] between runs. Each platform supplies one. [load] returns defaults
 * when nothing is stored or stored data is unreadable. [save] never throws; failed write
 * loses setting but not session.
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
 * Read-only settings of platform (EN 301 549 clause 11.7). Each platform reports what it
 * exposes and leaves rest at defaults.
 *
 * @property highContrast User asked platform for high contrast.
 * @property reducedMotion User asked platform for less motion.
 * @property textScalePercent Platform text size in percent, `100` when it is not exposed.
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

/** Theme to draw: stored [System] becomes [ThemePreference.HighContrast] on high-contrast platform. */
fun resolveTheme(
    stored: ThemePreference,
    platformHighContrast: Boolean,
): ThemePreference =
    if (stored == ThemePreference.System && platformHighContrast) {
        ThemePreference.HighContrast
    } else {
        stored
    }

/** Text size to draw: in-app size times platform size, within `100..200`. */
fun resolveTextScale(
    storedPercent: Int,
    platformPercent: Int,
): Int =
    (
        storedPercent.toLong() * platformPercent.coerceAtLeast(EditorState.MIN_TEXT_SCALE_PERCENT) /
            EditorState.MIN_TEXT_SCALE_PERCENT
    ).toInt()
        .coerceIn(EditorState.MIN_TEXT_SCALE_PERCENT, EditorState.MAX_TEXT_SCALE_PERCENT)
