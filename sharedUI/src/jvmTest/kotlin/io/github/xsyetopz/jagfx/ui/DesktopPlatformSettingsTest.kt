package io.github.xsyetopz.jagfx.ui

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import io.github.xsyetopz.jagfx.ui.platform.DesktopPlatformSettings
import io.github.xsyetopz.jagfx.ui.platform.WINDOWS_ACCESSIBILITY_KEY
import io.github.xsyetopz.jagfx.ui.platform.runCommand
import java.util.concurrent.CountDownLatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopPlatformSettingsTest {
    private fun settings(
        os: String,
        answers: Map<String, String?>,
    ) = DesktopPlatformSettings(os, {
        false
    }, { it() }) { cmd -> answers[cmd.drop(2).joinToString(" ")] }

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

    private val kdeAnimation = "kdeglobals --group KDE --key AnimationDurationFactor"
    private val kdeFont = "kdeglobals --group General --key font"

    @Test
    fun kdeInstantAnimationsAreReducedMotion() {
        assertTrue(linux(mapOf(kdeAnimation to "0")).reducedMotion)
        assertFalse(linux(mapOf(kdeAnimation to "0.5")).reducedMotion)
    }

    @Test
    fun kdeFontSizeIsTextScale() {
        assertEquals(
            120,
            linux(mapOf(kdeFont to "Noto Sans,12,-1,5,400,0,0,0,0,0,0,0,0,0,0,1")).textScalePercent,
        )
        assertEquals(
            105,
            linux(mapOf(kdeFont to "Noto Sans,10.5,-1,5,50,0,0,0,0,0")).textScalePercent,
        )
        assertEquals(100, linux(mapOf(kdeFont to "Noto Sans")).textScalePercent)
    }

    @Test
    fun linuxTakesLargerOfGnomeAndKde() {
        val s =
            linux(
                mapOf(
                    "org.gnome.desktop.interface text-scaling-factor" to "1.5",
                    kdeFont to "Noto Sans,12",
                ),
            )
        assertEquals(150, s.textScalePercent)
    }

    @Test
    fun windowsUsesToolkitFlag() {
        val s = DesktopPlatformSettings("Windows 11", { true }, { it() }) { null }
        assertTrue(s.highContrast)
        assertFalse(s.reducedMotion)
        assertEquals(100, s.textScalePercent)
    }

    private val textScaleQuery =
        listOf("reg", "query", WINDOWS_ACCESSIBILITY_KEY, "/v", "TextScaleFactor")

    private fun windowsScale(output: String?) =
        DesktopPlatformSettings("Windows 11", { false }, { it() }) { cmd ->
            output.takeIf { cmd == textScaleQuery }
        }.textScalePercent

    @Test
    fun windowsTextScaleReadsRegistryPercent() {
        val output =
            "\r\nHKEY_CURRENT_USER\\Software\\Microsoft\\Accessibility\r\n" +
                "    TextScaleFactor    REG_DWORD    0x7d\r\n"
        assertEquals(125, windowsScale(output))
    }

    @Test
    fun windowsTextScaleClampsAndDefaults() {
        assertEquals(200, windowsScale("    TextScaleFactor    REG_DWORD    0xe1"))
        assertEquals(100, windowsScale(null))
        assertEquals(100, windowsScale("    TextScaleFactor    REG_SZ    big"))
    }

    @Test
    fun throwingRunnerIsDefault() {
        val s = DesktopPlatformSettings("Linux", { false }, { it() }) { error("boom") }
        assertFalse(s.highContrast)
        assertEquals(100, s.textScalePercent)
    }

    @Test
    fun gettersDoNotWaitForProbe() {
        val release = CountDownLatch(1)
        var started: (() -> Unit)? = null
        val s =
            DesktopPlatformSettings("Linux", { false }, { started = it }) {
                release.await()
                "true"
            }
        assertFalse(s.highContrast)
        assertEquals(100, s.textScalePercent)
        release.countDown()
        started?.invoke()
        assertTrue(s.highContrast)
    }

    @Test
    fun finishedProbeRecomposesReader() {
        var finish: (() -> Unit)? = null
        val s = DesktopPlatformSettings("Linux", { false }, { finish = it }) { "1.5" }
        val seen = mutableListOf<Int>()
        ImageComposeScene(100, 100, Density(1f)) {
            seen += s.textScalePercent
        }.use { scene ->
            scene.render()
            finish?.invoke()
            scene.render()
        }
        assertEquals(listOf(100, 150), seen)
    }

    @Test
    fun commandFailureAndTimeoutGiveNull() {
        assertNull(runCommand(listOf("sleep", "5"), timeoutMs = 200))
        assertNull(runCommand(listOf("jagfx-no-such-command")))
        assertNull(runCommand(listOf("false")))
    }
}
