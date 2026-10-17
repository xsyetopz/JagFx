package io.github.xsyetopz.jagfx.core.engine

import io.github.xsyetopz.jagfx.core.model.Echo
import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.core.model.Loop
import io.github.xsyetopz.jagfx.core.model.Milliseconds
import io.github.xsyetopz.jagfx.core.model.Partial
import io.github.xsyetopz.jagfx.core.model.Patch
import io.github.xsyetopz.jagfx.core.model.Percent
import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.core.model.Voice
import io.github.xsyetopz.jagfx.core.model.Waveform
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RendererTest {
    private fun voice(
        duration: Int,
        offset: Int = 0,
    ) = Voice(
        frequencyEnvelope = Envelope(Waveform.Square, 1000, 1000, emptyList()),
        amplitudeEnvelope =
            Envelope(
                Waveform.Off,
                0,
                0,
                listOf(Segment(0, 30000), Segment(65535, 30000)),
            ),
        pitchLfo = null,
        amplitudeLfo = null,
        gate = null,
        partials = listOf(Partial(Percent(100), 0, Milliseconds(0))),
        echo = Echo.None,
        duration = Milliseconds(duration),
        offset = Milliseconds(offset),
        filter = null,
    )

    private fun patch(
        vararg voices: Voice?,
        loop: Loop = Loop.None,
    ) = Patch(List(10) { voices.getOrNull(it) }, loop)

    @Test
    fun emptyPatchRendersNoSamples() {
        assertEquals(0, Renderer.render(Patch.Empty).size)
    }

    @Test
    fun lengthIsTheLatestVoiceEnd() {
        assertEquals(
            samplesOf(150),
            Renderer.render(patch(voice(100), voice(50, offset = 100))).size,
        )
    }

    @Test
    fun shortVoiceIsSilentButCountsForLength() {
        val samples = Renderer.render(patch(voice(5, offset = 20)))
        assertEquals(samplesOf(25), samples.size)
        assertTrue(samples.all { it == 0.toShort() })
    }

    @Test
    fun loopRepeatsTheLoopRegion() {
        val loop = Loop(Milliseconds(20), Milliseconds(60))
        val once = Renderer.render(patch(voice(100), loop = loop))
        val thrice = Renderer.render(patch(voice(100), loop = loop), loopCount = 3)
        val ls = samplesOf(20)
        val le = samplesOf(60)
        val region = once.copyOfRange(ls, le)
        val expected = once.copyOfRange(0, le) + region + region + once.copyOfRange(le, once.size)
        assertContentEquals(expected, thrice)
    }

    @Test
    fun loopPastTheEndDoesNotRepeat() {
        val loop = Loop(Milliseconds(20), Milliseconds(200))
        assertEquals(
            samplesOf(100),
            Renderer.render(patch(voice(100), loop = loop), loopCount = 3).size,
        )
    }

    @Test
    fun slotSelectsOneVoice() {
        val p = patch(voice(100), voice(200))
        assertContentEquals(Renderer.render(patch(voice(100))), Renderer.render(p, slot = 0))
        assertEquals(samplesOf(200), Renderer.render(p, slot = 1).size)
        assertEquals(0, Renderer.render(p, slot = 5).size)
    }

    @Test
    fun invalidArgumentsAreRejected() {
        assertFailsWith<IllegalArgumentException> { Renderer.render(Patch.Empty, loopCount = -1) }
        assertFailsWith<IllegalArgumentException> { Renderer.render(Patch.Empty, slot = 10) }
    }

    @Test
    fun pcm8WavHasAnEightBitHeaderAndOffsetSamples() {
        val wav = Wav.encode(shortArrayOf(-32768, 0, 32767, 256), PcmEncoding.Pcm8)
        assertEquals(48, wav.size)
        assertEquals(40, wav[4].toInt())
        assertEquals(22050, (wav[28].toInt() and 0xFF) or ((wav[29].toInt() and 0xFF) shl 8))
        assertEquals(1, wav[32].toInt())
        assertEquals(8, wav[34].toInt())
        assertContentEquals(byteArrayOf(0, -128, -1, -127), wav.copyOfRange(44, 48))
    }
}
