package io.github.xsyetopz.jagfx.ui.canvas

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimeViewOpsTest {
    private val plotWidth = 400f

    @Test
    fun scrollOffsetConvertsToAFractionOfTheVoice() {
        val view = TimeView(zoom = 4).withScrollOffset(offset = plotWidth, plotWidth = plotWidth)

        assertEquals(plotWidth / (plotWidth * 4), view.scroll)
        assertEquals(plotWidth, view.toViewport(plotWidth).scrollOffset)
    }

    @Test
    fun scrollOffsetIsLimitedToTheEndOfTheVoice() {
        val view =
            TimeView(
                zoom = 2,
            ).withScrollOffset(offset = plotWidth * 10, plotWidth = plotWidth)

        assertEquals(1f - view.span, view.scroll)
    }

    @Test
    fun anEmptyPlotLeavesTheViewAlone() {
        val view = TimeView(zoom = 2, scroll = 0.25f)

        assertEquals(view, view.withScrollOffset(offset = 50f, plotWidth = 0f))
    }

    @Test
    fun zoomingAroundTheCenterKeepsTheCenterTime() {
        val view = TimeView(zoom = 2, scroll = 0.25f)
        val center = view.timeAt(0.5f)

        assertEquals(center, view.zoomedAroundCenter(4).timeAt(0.5f))
    }

    @Test
    fun zoomingInWithTheWheelKeepsTheTimeUnderThePointer() {
        val view = TimeView(zoom = 2, scroll = 0.25f)
        val relative = 0.3f
        val zoomed = view.zoomedStepAt(delta = 1f, relative = relative)

        assertEquals(view.timeAt(relative), zoomed.timeAt(relative), 1e-4f)
    }

    @Test
    fun zoomingNeverScrollsPastTheEndsOfTheVoice() {
        for (delta in listOf(-1f, 1f)) {
            for (relative in listOf(0f, 1f)) {
                val zoomed = TimeView(zoom = 2, scroll = 0.5f).zoomedStepAt(delta, relative)

                assertTrue(zoomed.scroll in 0f..(1f - zoomed.span))
            }
        }
    }
}
