package io.github.xsyetopz.jagfx.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Colors of the plots, one set per [JagFxAppearance]; read them as [JagFxTheme.palette].
 * Plots keep a dark scope in every appearance.
 * Every line, marker, and trace has at least 3:1 contrast against [scope], and 7:1 in
 * [HighContrast]; `ContrastTest` checks it.
 */
@Immutable
internal data class SynthPalette(
    /** Plot background. */
    val scope: Color,
    /** Grid lines at labelled steps. */
    val grid: Color,
    /** Grid lines between labelled steps. */
    val gridFaint: Color,
    /** The centre or zero line. */
    val midLine: Color,
    /** Axis labels and readouts drawn on the plot. */
    val scopeText: Color,
    /** Hover crosshair, breakpoint markers, and the selected breakpoint ring. */
    val marker: Color,
    /** Fill behind a selected breakpoint or pole. */
    val selection: Color,
    /** Loop start and end markers. */
    val loop: Color,
    /** Playback position. */
    val playhead: Color,
    /** Every envelope line. */
    val envelope: Color,
    /** Rendered output. */
    val output: Color,
    /** Frequency response. */
    val filter: Color,
    /** Filter zeros, the feed-forward channel. */
    val zero: Color,
    /** Filter poles, the feedback channel. */
    val pole: Color,
    /** Disabled or missing data on a plot. */
    val inactive: Color,
) {
    companion object {
        /**
         * The 2025 look: black plots, dark grey-green grid, grey midline, pale yellow-green
         * envelopes and response, pale green output, and poles and zeros in pink and cyan.
         */
        val Classic: SynthPalette =
            SynthPalette(
                scope = Color(0xFF000000),
                grid = Color(0xFF1E3024),
                gridFaint = Color(0xFF111B15),
                midLine = Color(0xFF7A7A7A),
                scopeText = Color(0xFFA8A8A8),
                marker = Color(0xFFF0F0F0),
                selection = Color(0x40FFFFFF),
                loop = Color(0xFFE69F00),
                playhead = Color(0xFFF0E442),
                envelope = Color(0xFFD2E08A),
                output = Color(0xFFA6E8A0),
                filter = Color(0xFFD2E08A),
                zero = Color(0xFF5CC8F0),
                pole = Color(0xFFF08CC0),
                inactive = Color(0xFF6A6A6A),
            )

        /** The Scala JagFX hues on a near-black scope, for the light chrome. */
        val Light: SynthPalette =
            SynthPalette(
                scope = Color(0xFF050505),
                grid = Color(0xFF2A2A2A),
                gridFaint = Color(0xFF181818),
                midLine = Color(0xFF777777),
                scopeText = Color(0xFFA0A0A0),
                marker = Color(0xFFF0F0F0),
                selection = Color(0x40FFFFFF),
                loop = Color(0xFFE69F00),
                playhead = Color(0xFFF0E442),
                envelope = Color(0xFFEE7733),
                output = Color(0xFF2ED1A0),
                filter = Color(0xFF33BBEE),
                zero = Color(0xFF33BBEE),
                pole = Color(0xFFCC79A7),
                inactive = Color(0xFF6A6A6A),
            )

        /** Pure black with white, yellow, green, cyan, and pink lines, every pair at 7:1 or more. */
        val HighContrast: SynthPalette =
            SynthPalette(
                scope = Color(0xFF000000),
                grid = Color(0xFF4A4A4A),
                gridFaint = Color(0xFF2A2A2A),
                midLine = Color(0xFFA0A0A0),
                scopeText = Color(0xFFFFFFFF),
                marker = Color(0xFFFFFFFF),
                selection = Color(0x60FFFF00),
                loop = Color(0xFFFFA500),
                playhead = Color(0xFFFFFFFF),
                envelope = Color(0xFFFFFF00),
                output = Color(0xFF00FF66),
                filter = Color(0xFFFFFF00),
                zero = Color(0xFF00FFFF),
                pole = Color(0xFFFF80FF),
                inactive = Color(0xFF9A9A9A),
            )
    }
}
