package io.github.xsyetopz.jagfx.ui.screen

import io.github.xsyetopz.jagfx.core.model.Waveform
import io.github.xsyetopz.jagfx.ui.editor.EditorIntent
import io.github.xsyetopz.jagfx.ui.editor.RequestIntent
import io.github.xsyetopz.jagfx.ui.editor.ValueUnit
import io.github.xsyetopz.jagfx.ui.editor.ViewIntent
import io.github.xsyetopz.jagfx.ui.i18n.Strings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import kotlin.math.roundToInt

private const val PERCENT_PER_RAW = 655.35
private const val GATE_UNITS_PER_STEP = 256.0
private const val SAMPLES_PER_MS = 22.05

/** The raw value of [value] in this unit; the inverse of [fromRaw]. */
internal fun ValueUnit.toRaw(value: Double): Int =
    when (this) {
        ValueUnit.Hz -> value
        ValueUnit.Percent -> value * PERCENT_PER_RAW
        ValueUnit.Ms -> value * GATE_UNITS_PER_STEP * SAMPLES_PER_MS
    }.roundToInt()

/** The mean of the raw values [min] and [max] in [unit]. */
internal fun averageIn(
    unit: ValueUnit,
    min: Int,
    max: Int,
): Double = unit.fromRaw((min.toDouble() + max) / 2)

/** The waveform names, in [Waveform.selectable] order. */
private val WaveformNames =
    listOf(
        UiString.WaveOff,
        UiString.WaveSquare,
        UiString.WaveSine,
        UiString.WaveSawtooth,
        UiString.WaveNoise,
    )

/** The name of [waveform] in [strings]. */
internal fun waveformName(
    strings: Strings,
    waveform: Waveform,
): String = strings[WaveformNames[Waveform.selectable.indexOf(waveform)]]

/** The playback buttons of the voice strip. */
internal enum class PlayAction(
    val singleTone: Boolean,
    val looping: Boolean,
) {
    Play(singleTone = true, looping = false),
    PlayAll(singleTone = false, looping = false),
    Loop(singleTone = true, looping = true),
    LoopAll(singleTone = false, looping = true),
    ;

    /** The intents that set the playback options and then start playing. */
    fun intents(): List<EditorIntent> =
        listOf(
            ViewIntent.SetPlaySingleVoice(singleTone),
            ViewIntent.SetLooping(looping),
            RequestIntent.TogglePlay,
        )
}
