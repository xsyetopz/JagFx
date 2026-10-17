package io.github.xsyetopz.jagfx.ui.screen

import io.github.xsyetopz.jagfx.core.model.SynthLimits
import kotlin.test.Test
import kotlin.test.assertEquals

class LaneModelTest {
    @Test
    fun pressNearTheEndResizesAndElsewhereMoves() {
        assertEquals(LaneDrag.Move, laneDragAt(x = 50f, barStart = 0f, barEnd = 100f, edge = 6f))
        assertEquals(LaneDrag.Resize, laneDragAt(x = 94f, barStart = 0f, barEnd = 100f, edge = 6f))
        assertEquals(LaneDrag.Resize, laneDragAt(x = 5f, barStart = 0f, barEnd = 10f, edge = 6f))
    }

    @Test
    fun dragsKeepTimesInTheFormatRange() {
        assertEquals(
            110 to 50,
            draggedTimes(LaneDrag.Move, offset = 100, duration = 50, deltaMs = 9.6f),
        )
        assertEquals(
            0 to 50,
            draggedTimes(LaneDrag.Move, offset = 100, duration = 50, deltaMs = -500f),
        )
        assertEquals(
            100 to 0,
            draggedTimes(LaneDrag.Resize, offset = 100, duration = 50, deltaMs = -80f),
        )
        assertEquals(
            100 to SynthLimits.U16_MAX,
            draggedTimes(LaneDrag.Resize, offset = 100, duration = 50, deltaMs = 1e6f),
        )
    }

    @Test
    fun rulerStepIsTheSmallestRoundStepWithRoomForLabels() {
        assertEquals(1, rulerStep(spanMs = 10, widthPx = 1000f, minGapPx = 50f))
        assertEquals(50, rulerStep(spanMs = 1000, widthPx = 1000f, minGapPx = 50f))
        assertEquals(100, rulerStep(spanMs = 1000, widthPx = 500f, minGapPx = 50f))
        assertEquals(200, rulerStep(spanMs = 2000, widthPx = 600f, minGapPx = 50f))
        assertEquals(1000, rulerStep(spanMs = 1000, widthPx = 0f, minGapPx = 50f))
    }
}
