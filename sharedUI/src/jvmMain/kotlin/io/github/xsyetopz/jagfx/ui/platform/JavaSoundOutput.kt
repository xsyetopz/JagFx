package io.github.xsyetopz.jagfx.ui.platform

import io.github.xsyetopz.jagfx.ui.editor.AudioOutput
import io.github.xsyetopz.jagfx.ui.editor.AudioOutputException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.SourceDataLine

private const val BYTES_PER_FRAME = 2
private const val CHUNKS_PER_SECOND = 100
private const val BUFFER_CHUNKS = 10
private const val DRAIN_POLL_MS = 10L
private const val DRAIN_LIMIT_MS = 1000L * BUFFER_CHUNKS / CHUNKS_PER_SECOND + 500L

/**
 * Plays through a Java Sound [SourceDataLine] from [openLine], on [dispatcher].
 *
 * Writes 10 ms chunks into a 100 ms line buffer, so a cancelled collector stops within about
 * 100 ms. Emits progress after each chunk and while the line drains.
 */
class JavaSoundOutput(
    private val openLine: (AudioFormat) -> SourceDataLine = AudioSystem::getSourceDataLine,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AudioOutput {
    override fun play(
        pcm: ShortArray,
        sampleRate: Int,
    ): Flow<Float> =
        flow {
            if (pcm.isEmpty()) return@flow
            val format = AudioFormat(sampleRate.toFloat(), Short.SIZE_BITS, 1, true, false)
            val chunkFrames = (sampleRate / CHUNKS_PER_SECOND).coerceAtLeast(1)
            val line = open(format, chunkFrames * BUFFER_CHUNKS * BYTES_PER_FRAME)
            try {
                line.start()
                val chunk = ByteArray(chunkFrames * BYTES_PER_FRAME)
                var frame = 0
                while (frame < pcm.size) {
                    val count = minOf(chunkFrames, pcm.size - frame)
                    for (i in 0 until count) {
                        val sample = pcm[frame + i].toInt()
                        chunk[i * BYTES_PER_FRAME] = sample.toByte()
                        chunk[i * BYTES_PER_FRAME + 1] = (sample shr Byte.SIZE_BITS).toByte()
                    }
                    line.write(chunk, 0, count * BYTES_PER_FRAME)
                    frame += count
                    emit(progress(line, pcm.size))
                }
                // drain() can hang on some Linux mixers; poll for the buffered time plus a margin.
                var waitedMs = 0L
                while (line.longFramePosition < pcm.size && waitedMs < DRAIN_LIMIT_MS) {
                    delay(DRAIN_POLL_MS)
                    waitedMs += DRAIN_POLL_MS
                    emit(progress(line, pcm.size))
                }
                emit(1f)
            } finally {
                line.stop()
                line.flush()
                line.close()
            }
        }.flowOn(dispatcher)

    private fun open(
        format: AudioFormat,
        bufferBytes: Int,
    ): SourceDataLine =
        try {
            openLine(format).apply { open(format, bufferBytes) }
        } catch (e: LineUnavailableException) {
            throw AudioOutputException(e.message ?: "Audio output is unavailable", e)
        } catch (e: IllegalArgumentException) {
            throw AudioOutputException(e.message ?: "Audio format is not supported", e)
        } catch (e: SecurityException) {
            throw AudioOutputException(e.message ?: "Audio output is not permitted", e)
        }
}

private fun progress(
    line: SourceDataLine,
    frames: Int,
): Float = (line.longFramePosition.toFloat() / frames).coerceIn(0f, 1f)
