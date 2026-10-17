package io.github.xsyetopz.jagfx.ui.editor

import io.github.xsyetopz.jagfx.core.engine.Renderer
import io.github.xsyetopz.jagfx.core.model.Patch
import io.github.xsyetopz.jagfx.core.vorbis.JagVorbis
import kotlinx.coroutines.flow.Flow

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
 * no leading dot.
 */
enum class PatchFileKind(
    val extension: String,
) {
    /** A patch of ten voice slots and a loop. */
    Synth("synth"),

    /** One voice, encoded as a voice slot of a `.synth` file. */
    Tone("tone"),

    /** One filter, encoded as the filter block of a voice. */
    Filter("filter"),

    /** A recorded sample, a document apart from the patch. */
    Sample("jagvorbis"),
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
