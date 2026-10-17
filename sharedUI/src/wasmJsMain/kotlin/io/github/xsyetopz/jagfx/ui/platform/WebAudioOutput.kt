@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.xsyetopz.jagfx.ui.platform

import io.github.xsyetopz.jagfx.ui.editor.AudioOutput
import io.github.xsyetopz.jagfx.ui.editor.AudioOutputException
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.khronos.webgl.Float32Array
import org.khronos.webgl.toFloat32Array
import kotlin.js.Promise

private const val FRAME_MS = 16L
private const val SAMPLE_SCALE = 32768f

/**
 * Plays through one shared Web Audio `AudioContext`, created on the first play so it starts
 * inside a user gesture. Emits progress from the context clock about once per frame.
 */
class WebAudioOutput : AudioOutput {
    private var context: AudioContext? = null

    override fun play(
        pcm: ShortArray,
        sampleRate: Int,
    ): Flow<Float> =
        flow {
            if (pcm.isEmpty()) return@flow
            val (audio, source) = start(pcm, sampleRate)
            var ended = false
            source.onended = { ended = true }
            val startedAt = audio.currentTime
            val duration = pcm.size.toDouble() / sampleRate
            try {
                while (!ended) {
                    emit(((audio.currentTime - startedAt) / duration).toFloat().coerceIn(0f, 1f))
                    delay(FRAME_MS)
                }
                emit(1f)
            } finally {
                source.onended = null
                if (!ended) source.stop()
                source.disconnect()
            }
        }

    private suspend fun start(
        pcm: ShortArray,
        sampleRate: Int,
    ): Pair<AudioContext, AudioBufferSourceNode> =
        try {
            val audio = context ?: AudioContext().also { context = it }
            if (audio.state == "suspended") audio.resume().await<JsAny?>()
            val buffer = audio.createBuffer(1, pcm.size, sampleRate.toFloat())
            buffer.copyToChannel(
                FloatArray(pcm.size) { pcm[it] / SAMPLE_SCALE }.toFloat32Array(),
                0,
            )
            val source = audio.createBufferSource()
            source.buffer = buffer
            source.connect(audio.destination)
            source.start()
            audio to source
        } catch (e: JsException) {
            throw AudioOutputException(e.message ?: "Audio output is unavailable", e)
        }
}

internal external class AudioContext : JsAny {
    val currentTime: Double
    val destination: AudioNode
    val state: String

    fun createBuffer(
        numberOfChannels: Int,
        length: Int,
        sampleRate: Float,
    ): AudioBuffer

    fun createBufferSource(): AudioBufferSourceNode

    fun resume(): Promise<JsAny?>
}

internal open external class AudioNode : JsAny {
    fun connect(destination: AudioNode): AudioNode

    fun disconnect()
}

internal external class AudioBuffer : JsAny {
    fun copyToChannel(
        source: Float32Array,
        channelNumber: Int,
    )
}

internal external class AudioBufferSourceNode : AudioNode {
    var buffer: AudioBuffer?
    var onended: (() -> Unit)?

    fun start()

    fun stop()
}
