package io.github.xsyetopz.jagfx.ui.canvas

/** Column reduction of the output waveform: the min and max sample of each pixel column. */
internal object Downsampler {
    /**
     * Minimum and maximum of [samples] per pixel column, interleaved as `[min0, max0, min1, ...]`.
     *
     * The samples are spread over `columns * zoom` columns and the view starts [scroll] columns
     * in. Column `px` covers the samples from `(int)((px + scroll) * step)` until
     * `(int)((px + 1 + scroll) * step)`, with `step = samples.size / (columns * zoom)`. A column
     * without samples, before the start, or past the end holds `NaN` twice.
     */
    fun minMax(
        samples: FloatArray,
        columns: Int,
        zoom: Int,
        scroll: Float,
    ): FloatArray {
        val result = FloatArray(columns.coerceAtLeast(0) * 2) { Float.NaN }
        val count = samples.size
        if (columns <= 0 || count == 0) return result
        val step = count.toDouble() / (columns.toDouble() * zoom.coerceAtLeast(1))
        for (px in 0 until columns) {
            val start = ((px + scroll) * step).toInt()
            if (start >= count) break
            val end = ((px + 1 + scroll) * step).toInt().coerceAtMost(count)
            if (start in 0 until end) reduceColumn(samples, start, end, result, px * 2)
        }
        return result
    }

    private fun reduceColumn(
        samples: FloatArray,
        start: Int,
        end: Int,
        result: FloatArray,
        index: Int,
    ) {
        var min = samples[start]
        var max = min
        for (i in start + 1 until end) {
            val sample = samples[i]
            if (sample < min) min = sample
            if (sample > max) max = sample
        }
        result[index] = min
        result[index + 1] = max
    }
}
