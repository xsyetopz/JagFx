package io.github.xsyetopz.jagfx.ui.canvas

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DownsamplerTest {
    private val ramp = floatArrayOf(0f, 1f, -2f, 3f, -4f, 5f, -6f, 7f)

    @Test
    fun reducesEachColumnToItsMinimumAndMaximum() {
        val result = Downsampler.minMax(ramp, columns = 4, zoom = 1, scroll = 0f)
        assertContentEquals(floatArrayOf(0f, 1f, -2f, 3f, -4f, 5f, -6f, 7f), result)
        val halves = Downsampler.minMax(ramp, columns = 2, zoom = 1, scroll = 0f)
        assertContentEquals(floatArrayOf(-2f, 3f, -6f, 7f), halves)
    }

    @Test
    fun columnsWithoutSamplesAreNaN() {
        // Two samples over four columns: step 0.5, so column 0 covers [0, 0) and column 1 [0, 1).
        val result =
            Downsampler.minMax(
                floatArrayOf(0.25f, -0.5f),
                columns = 4,
                zoom = 1,
                scroll = 0f,
            )
        assertTrue(result[0].isNaN() && result[1].isNaN())
        assertEquals(0.25f, result[2])
        assertTrue(result[4].isNaN() && result[5].isNaN())
        assertEquals(-0.5f, result[7])
        assertTrue(Downsampler.minMax(FloatArray(0), 3, 1, 0f).all { it.isNaN() })
    }

    @Test
    fun zoomAndScrollSelectTheVisibleSamples() {
        // Zoom 2 spreads 8 samples over 8 virtual columns; scroll 4 shows the second half.
        val result = Downsampler.minMax(ramp, columns = 4, zoom = 2, scroll = 4f)
        assertContentEquals(floatArrayOf(-4f, -4f, 5f, 5f, -6f, -6f, 7f, 7f), result)
        val fractional = Downsampler.minMax(ramp, columns = 2, zoom = 2, scroll = 0.5f)
        // step 2: column 0 covers samples 1..2, column 1 covers samples 3..4.
        assertContentEquals(floatArrayOf(-2f, 1f, -4f, 3f), fractional)
    }

    @Test
    fun columnsPastTheEndAreNaN() {
        val result = Downsampler.minMax(ramp, columns = 4, zoom = 2, scroll = 6f)
        assertContentEquals(floatArrayOf(-6f, -6f, 7f, 7f), result.copyOfRange(0, 4))
        assertTrue(result.copyOfRange(4, 8).all { it.isNaN() })
    }
}
