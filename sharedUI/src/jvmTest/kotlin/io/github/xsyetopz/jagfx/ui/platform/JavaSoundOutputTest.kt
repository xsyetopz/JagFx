package io.github.xsyetopz.jagfx.ui.platform

import io.github.xsyetopz.jagfx.ui.editor.AudioOutputException
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.lang.reflect.Proxy
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.SourceDataLine
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JavaSoundOutputTest {
    @Test
    fun writesLittleEndianSamplesAndEndsAtFullProgress() =
        runTest {
            val line = FakeLine()
            val pcm = ShortArray(1000) { (it * 37 - 18000).toShort() }

            val progress = JavaSoundOutput({ line.proxy }).play(pcm, 22050).toList()

            assertContentEquals(littleEndian(pcm), line.written.toByteArray())
            assertEquals(1f, progress.last())
            assertTrue(progress.zipWithNext().all { (a, b) -> a <= b })
            assertTrue(line.closed)
        }

    @Test
    fun cancelledCollectorStopsAndClosesLine() =
        runTest {
            val line = FakeLine()
            val pcm = ShortArray(22050)

            JavaSoundOutput({ line.proxy }).play(pcm, 22050).take(1).toList()

            assertTrue(line.written.size() < pcm.size * 2)
            assertTrue(line.stopped)
            assertTrue(line.closed)
        }

    @Test
    fun unavailableLineThrowsAudioOutputException() =
        runTest {
            val output = JavaSoundOutput({ throw LineUnavailableException("busy") })

            val error =
                assertFailsWith<AudioOutputException> { output.play(ShortArray(1), 22050).toList() }
            assertEquals("busy", error.message)
        }

    @Test
    fun emptyPcmOpensNoLine() =
        runTest {
            val output = JavaSoundOutput({ error("opened") })

            assertEquals(emptyList(), output.play(ShortArray(0), 22050).toList())
        }
}

private fun littleEndian(pcm: ShortArray): ByteArray =
    ByteArray(pcm.size * 2) { i ->
        val sample = pcm[i / 2].toInt()
        (if (i % 2 == 0) sample else sample shr 8).toByte()
    }

/** A line that plays each write at once, so its frame position is the frames written. */
private class FakeLine {
    val written = ByteArrayOutputStream()
    var stopped = false
    var closed = false

    val proxy: SourceDataLine =
        Proxy.newProxyInstance(
            SourceDataLine::class.java.classLoader,
            arrayOf(SourceDataLine::class.java),
        ) { _, method, args ->
            when (method.name) {
                "write" -> {
                    val count = args[2] as Int
                    written.write(args[0] as ByteArray, args[1] as Int, count)
                    count
                }

                "getLongFramePosition" -> {
                    written.size() / 2L
                }

                "stop" -> {
                    stopped = true
                    null
                }

                "close" -> {
                    closed = true
                    null
                }

                "open", "start", "flush" -> {
                    null
                }

                else -> {
                    error("unexpected call ${method.name}")
                }
            }
        } as SourceDataLine
}
