package io.github.xsyetopz.jagfx.ui

import io.github.xsyetopz.jagfx.ui.platform.DesktopPlatformSettings
import io.github.xsyetopz.jagfx.ui.platform.runCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopPlatformSettingsTest {
    private fun settings(
        os: String,
        answers: Map<String, String?>,
    ) = DesktopPlatformSettings(os, { false }) { cmd -> answers[cmd.drop(2).joinToString(" ")] }

    private fun mac(answers: Map<String, String?>) = settings("Mac OS X", answers)

    private fun linux(answers: Map<String, String?>) = settings("Linux", answers)

    private fun scale(text: String?) =
        linux(
            mapOf("org.gnome.desktop.interface text-scaling-factor" to text),
        ).textScalePercent

    @Test
    fun macOneIsTrue() {
        val s =
            mac(
                mapOf(
                    "com.apple.universalaccess increaseContrast" to "1",
                    "com.apple.universalaccess reduceMotion" to "1",
                ),
            )
        assertTrue(s.highContrast)
        assertTrue(s.reducedMotion)
    }

    @Test
    fun macNewDomainIsTrue() {
        val s = mac(mapOf("com.apple.Accessibility ReduceMotionEnabled" to "1"))
        assertTrue(s.reducedMotion)
        assertFalse(s.highContrast)
    }

    @Test
    fun macFailureIsFalse() {
        val s = mac(emptyMap())
        assertFalse(s.highContrast)
        assertFalse(s.reducedMotion)
        assertEquals(100, s.textScalePercent)
    }

    @Test
    fun macZeroIsFalse() {
        assertFalse(mac(mapOf("com.apple.universalaccess reduceMotion" to "0")).reducedMotion)
    }

    @Test
    fun gnomeScaleRoundsToPercent() {
        assertEquals(125, scale("1.25"))
        assertEquals(110, scale("1.1\n"))
        assertEquals(150, scale("double 1.5"))
    }

    @Test
    fun gnomeScaleGarbageIsDefault() {
        assertEquals(100, scale("banana"))
        assertEquals(100, scale(""))
        assertEquals(100, scale(null))
        assertEquals(100, scale("NaN"))
    }

    @Test
    fun gnomeScaleClamps() {
        assertEquals(200, scale("3.0"))
        assertEquals(200, scale("2.5"))
        assertEquals(100, scale("0.5"))
    }

    @Test
    fun gnomeFlags() {
        val s =
            linux(
                mapOf(
                    "org.gnome.desktop.a11y.interface high-contrast" to "true",
                    "org.gnome.desktop.interface enable-animations" to "false",
                ),
            )
        assertTrue(s.highContrast)
        assertTrue(s.reducedMotion)
        val d = linux(emptyMap())
        assertFalse(d.highContrast)
        assertFalse(d.reducedMotion)
    }

    @Test
    fun windowsUsesToolkitFlag() {
        var called = false
        val s =
            DesktopPlatformSettings("Windows 11", { true }) {
                called = true
                "1"
            }
        assertTrue(s.highContrast)
        assertFalse(s.reducedMotion)
        assertEquals(100, s.textScalePercent)
        assertFalse(called)
    }

    @Test
    fun throwingRunnerIsDefault() {
        val s = DesktopPlatformSettings("Linux", { false }) { error("boom") }
        assertFalse(s.highContrast)
        assertEquals(100, s.textScalePercent)
    }

    @Test
    fun commandFailureAndTimeoutGiveNull() {
        assertNull(runCommand(listOf("sleep", "5"), timeoutMs = 200))
        assertNull(runCommand(listOf("jagfx-no-such-command")))
        assertNull(runCommand(listOf("false")))
    }
}
