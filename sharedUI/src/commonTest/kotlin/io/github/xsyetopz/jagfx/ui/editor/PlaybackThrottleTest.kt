package io.github.xsyetopz.jagfx.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlaybackThrottleTest {
    @Test
    fun intervalFollowsAudioClockAndIsOneHertzWhenMotionIsReduced() {
        assertEquals(0L, playbackIntervalMs(reducedMotion = false))
        assertEquals(1000L, playbackIntervalMs(reducedMotion = true))
    }

    @Test
    fun zeroIntervalPassesEveryPosition() {
        val throttle = PlaybackThrottle(playbackIntervalMs(reducedMotion = false))
        val passed = (0 until 100).count { throttle.offer(it / 100f, it * 10L) != null }
        assertEquals(100, passed)
        assertNull(throttle.flush())
    }

    @Test
    fun firstPositionPassesAndLaterOnesWaitForInterval() {
        val throttle = PlaybackThrottle(250)
        assertEquals(0.0f, throttle.offer(0.0f, 0))
        assertNull(throttle.offer(0.1f, 16))
        assertNull(throttle.offer(0.2f, 249))
        assertEquals(0.3f, throttle.offer(0.3f, 250))
        assertNull(throttle.offer(0.4f, 499))
        assertEquals(0.5f, throttle.offer(0.5f, 500))
    }

    @Test
    fun sixtyHertzStreamForOneSecondPassesAboutFourPositions() {
        val throttle = PlaybackThrottle(250)
        val passed = (0 until 60).count { throttle.offer(it / 60f, it * 1000L / 60) != null }
        assertEquals(4, passed)
    }

    @Test
    fun oneHertzPassesOnePositionPerSecond() {
        val throttle = PlaybackThrottle(playbackIntervalMs(reducedMotion = true))
        val passed = (0 until 180).count { throttle.offer(it / 180f, it * 1000L / 60) != null }
        assertEquals(3, passed)
    }

    @Test
    fun flushReturnsHeldBackFinalPositionOnce() {
        val throttle = PlaybackThrottle(250)
        throttle.offer(0.0f, 0)
        throttle.offer(0.9f, 100)
        assertEquals(0.9f, throttle.flush())
        assertNull(throttle.flush())
    }

    @Test
    fun flushReturnsNothingWhenLastPositionWasPassed() {
        val throttle = PlaybackThrottle(250)
        throttle.offer(0.5f, 0)
        assertNull(throttle.flush())
    }

    @Test
    fun timestampThatGoesBackwardsDoesNotStallStream() {
        val throttle = PlaybackThrottle(250)
        throttle.offer(0.1f, 1000)
        assertEquals(0.2f, throttle.offer(0.2f, 0))
    }
}
