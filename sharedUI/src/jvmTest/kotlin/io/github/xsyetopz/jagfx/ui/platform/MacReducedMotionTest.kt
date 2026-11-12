package io.github.xsyetopz.jagfx.ui.platform

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MacReducedMotionTest {
    @Test
    fun aDefaultsValueOfOneMeansReducedMotion() {
        assertTrue(parseDefaultsBoolean("1\n"))
    }

    @Test
    fun zeroAndErrorTextMeanFullMotion() {
        assertFalse(parseDefaultsBoolean("0\n"))
        assertFalse(parseDefaultsBoolean("Error: Could not find key"))
        assertFalse(parseDefaultsBoolean(""))
    }

    @Test
    fun otherSystemsReadNoSetting() {
        assertFalse(readMacReducedMotion("Linux"))
        assertFalse(readMacReducedMotion("Windows 11"))
    }
}
