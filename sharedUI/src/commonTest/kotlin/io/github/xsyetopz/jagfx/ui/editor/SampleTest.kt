package io.github.xsyetopz.jagfx.ui.editor

import io.github.xsyetopz.jagfx.core.vorbis.JagVorbis
import io.github.xsyetopz.jagfx.core.vorbis.VorbisStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal fun jagVorbis(
    sampleCount: Int = 10,
    loopBegin: Int = 2,
    loopEnd: Int = 6,
    pingPong: Boolean = false,
) = JagVorbis(
    22050,
    sampleCount,
    loopBegin,
    loopEnd,
    pingPong,
    VorbisStream(byteArrayOf(1, 2, 3), listOf(byteArrayOf(4), byteArrayOf(5, 6))),
)

internal fun openState(sample: JagVorbis = jagVorbis()): EditorState =
    EditorState().after(
        ResultIntent.SampleLoaded("a", "/a.jagvorbis", sample, ShortArray(sample.sampleCount)),
    )

class SampleReducerTest {
    @Test
    fun sampleCountClampsToLoadedCountAndPullsLoopIn() {
        val state = openState()

        val trimmed = state.after(SampleIntent.SetSampleCount(4)).sample!!.sample
        val grown = state.after(SampleIntent.SetSampleCount(4), SampleIntent.SetSampleCount(99))
        val negative = state.after(SampleIntent.SetSampleCount(-5)).sample!!.sample

        assertEquals(4, trimmed.sampleCount)
        assertEquals(2, trimmed.loopBegin)
        assertEquals(4, trimmed.loopEnd)
        assertEquals(10, grown.sample!!.sample.sampleCount)
        assertEquals(0, negative.sampleCount)
        assertEquals(0, negative.loopBegin)
        assertEquals(0, negative.loopEnd)
    }

    @Test
    fun loopBeginClampsToZeroAndLoopEnd() {
        val state = openState()

        val high = state.after(SampleIntent.SetLoopBegin(99)).sample!!.sample
        val low = state.after(SampleIntent.SetLoopBegin(-3)).sample!!.sample

        assertEquals(6, high.loopBegin)
        assertEquals(0, low.loopBegin)
    }

    @Test
    fun loopEndClampsToLoopBeginAndSampleCount() {
        val state = openState()

        val high = state.after(SampleIntent.SetLoopEnd(99)).sample!!.sample
        val low = state.after(SampleIntent.SetLoopEnd(0)).sample!!.sample

        assertEquals(10, high.loopEnd)
        assertEquals(2, low.loopEnd)
    }

    @Test
    fun pingPongToggleAndDirtyTracking() {
        val state = openState()

        val toggled = state.after(SampleIntent.SetPingPong(true))
        val restored = toggled.after(SampleIntent.SetPingPong(false))

        assertTrue(toggled.sample!!.sample.pingPong)
        assertTrue(toggled.sample.isDirty)
        assertEquals(false, restored.sample!!.isDirty)
    }

    @Test
    fun windowTitleShowsSampleWhileOpen() {
        val state = openState().after(SampleIntent.SetPingPong(true))

        assertEquals("JagFx: /a.jagvorbis *", state.windowTitle(home = null))
    }

    @Test
    fun editsKeepPatchAndIgnoredWithoutSample() {
        val patch = EditorState().after(SlotIntent.SetVoiceEnabled(0, true))

        val untouched = patch.after(SampleIntent.SetSampleCount(1))
        val opened = patch.after(ResultIntent.SampleLoaded("a", null, jagVorbis(), ShortArray(10)))
        val closed = opened.after(RequestIntent.CloseSample)

        assertEquals(patch, untouched)
        assertEquals(patch.patch, opened.patch)
        assertNull(closed.sample)
        assertEquals(patch.patch, closed.patch)
    }
}

class SamplePlaybackTest {
    private val pcm = ShortArray(10) { it.toShort() }

    private fun play(
        sample: JagVorbis,
        passes: Int,
    ) = samplePlaybackPcm(pcm, sample, passes).map { it.toInt() }

    @Test
    fun forwardPlaysSampleCountSamples() {
        assertEquals((0..7).toList(), play(jagVorbis(sampleCount = 8), passes = 1))
    }

    @Test
    fun loopedRepeatsLoopRegionBetweenHeadAndTail() {
        val result = play(jagVorbis(), passes = 3)

        assertEquals(
            listOf(0, 1, 2, 3, 4, 5) + listOf(2, 3, 4, 5) + listOf(2, 3, 4, 5) + listOf(6, 7, 8, 9),
            result,
        )
    }

    @Test
    fun pingPongReversesEveryOtherRepetition() {
        val result = play(jagVorbis(pingPong = true), passes = 3)

        assertEquals(
            listOf(0, 1, 2, 3, 4, 5) + listOf(5, 4, 3, 2) + listOf(2, 3, 4, 5) + listOf(6, 7, 8, 9),
            result,
        )
    }

    @Test
    fun pingPongEndingReversedRunsBackToFirstSampleAndStops() {
        val result = play(jagVorbis(pingPong = true), passes = 2)

        assertEquals(listOf(0, 1, 2, 3, 4, 5) + listOf(5, 4, 3, 2, 1, 0), result)
    }

    @Test
    fun emptyLoopRegionPlaysForwardOnce() {
        assertEquals((0..9).toList(), play(jagVorbis(loopBegin = 4, loopEnd = 4), passes = 5))
    }
}
