package io.github.xsyetopz.jagfx.ui.screen

import io.github.xsyetopz.jagfx.core.model.Waveform
import io.github.xsyetopz.jagfx.ui.editor.RequestIntent
import io.github.xsyetopz.jagfx.ui.editor.ValueUnit
import io.github.xsyetopz.jagfx.ui.editor.ViewIntent
import io.github.xsyetopz.jagfx.ui.i18n.Strings
import io.github.xsyetopz.jagfx.ui.i18n.UiLocale
import io.github.xsyetopz.jagfx.ui.widgets.formatDecimal
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StripModelTest {
    private val raws = listOf(0, 1, 255, 256, 1000, 32768, 65535)

    @Test
    fun toRawInvertsFromRawInEveryUnit() {
        for (unit in ValueUnit.entries) {
            for (raw in raws) {
                assertEquals(
                    raw,
                    unit.toRaw(unit.fromRaw(raw.toDouble())).roundToInt(),
                    "$unit $raw",
                )
            }
        }
    }

    @Test
    fun averageIsMeanOfRawValuesInUnit() {
        for (unit in ValueUnit.entries) {
            assertEquals(unit.fromRaw(500.0), averageIn(unit, 200, 800))
        }
    }

    @Test
    fun averageOfNegativeAndPositiveBoundsDoesNotOverflow() {
        assertEquals(0.0, averageIn(ValueUnit.Hz, Int.MIN_VALUE + 1, Int.MAX_VALUE))
    }

    @Test
    fun everyPlayActionSetsOptionsBeforeItStartsPlaying() {
        for (action in PlayAction.entries) {
            val intents = action.intents()
            assertEquals(ViewIntent.SetPlaySingleVoice(action.singleTone), intents.first())
            assertEquals(ViewIntent.SetLooping(action.looping), intents[1])
            assertEquals(RequestIntent.TogglePlay, intents.last())
        }
    }

    @Test
    fun playActionsDifferInTheirOptions() {
        val options = PlayAction.entries.map { it.singleTone to it.looping }
        assertEquals(options.size, options.toSet().size)
    }

    @Test
    fun everySelectableWaveformHasName() {
        val strings = Strings(UiLocale.En)
        val names = Waveform.selectable.map { waveformName(strings, it) }
        assertTrue(names.all { it.isNotEmpty() })
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun formatDecimalPadsRoundsAndSigns() {
        assertEquals("1.50", formatDecimal(1.5, 2))
        assertEquals("0.13", formatDecimal(0.126, 2))
        assertEquals("-2.25", formatDecimal(-2.25, 2))
        assertEquals("3", formatDecimal(2.6, 0))
    }

    @Test
    fun formatDecimalDropsSignOfValueThatRoundsToZero() {
        assertEquals("0.00", formatDecimal(-0.001, 2))
    }
}
