package io.github.xsyetopz.jagfx.ui.canvas

import kotlin.test.Test
import kotlin.test.assertEquals

class PlotLabelsTest {
    @Test
    fun valueLabelInTallPlotIsCentredOnItsTick() {
        assertEquals(40f, valueLabelTop(y = 50f, labelHeight = 20f, height = 200f))
    }

    @Test
    fun valueLabelInPlotShorterThanTwoLabelsStaysAtTop() {
        assertEquals(0f, valueLabelTop(y = 10f, labelHeight = 20f, height = 12f))
    }
}
