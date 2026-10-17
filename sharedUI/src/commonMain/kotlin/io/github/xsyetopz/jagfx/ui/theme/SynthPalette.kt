package io.github.xsyetopz.jagfx.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colors of the plots, which stay dark in both appearances like the screen of a scope.
 * As in the Scala JagFX, every envelope draws orange, the output green, and the filter plots
 * the accent blue; the other hues come from the Wong color-blind palette.
 */
internal class SynthPalette private constructor() {
    companion object {
        /** Plot background. */
        val Scope: Color = Color(0xFF050505)

        /** Grid lines at labelled steps. */
        val Grid: Color = Color(0xFF2A2A2A)

        /** Grid lines between labelled steps. */
        val GridFaint: Color = Color(0xFF181818)

        /** The centre or zero line. */
        val MidLine: Color = Color(0xFF3A3A3A)

        /** Axis labels and readouts drawn on the plot. */
        val ScopeText: Color = Color(0xFF888888)

        /** Hover crosshair and the selected breakpoint ring. */
        val Marker: Color = Color(0xFFF0F0F0)

        /** Fill behind a selected breakpoint or pole. */
        val Selection: Color = Color(0x40FFFFFF)

        /** Loop start and end markers. */
        val Loop: Color = Color(0xFFE69F00)

        /** Playback position. */
        val Playhead: Color = Color(0xFFF0E442)

        /** Every envelope line. */
        val Envelope: Color = Color(0xFFEE7733)

        /** Rendered output. */
        val Output: Color = Color(0xFF009E73)

        /** Frequency response. */
        val Filter: Color = Color(0xFF33BBEE)

        /** Filter zeros, the feed-forward channel. */
        val Zero: Color = Color(0xFF33BBEE)

        /** Filter poles, the feedback channel. */
        val Pole: Color = Color(0xFFCC79A7)

        /** Disabled or missing data on a plot. */
        val Inactive: Color = Color(0xFF555555)
    }
}
