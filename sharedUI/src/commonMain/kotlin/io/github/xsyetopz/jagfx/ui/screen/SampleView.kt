package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.xsyetopz.jagfx.ui.canvas.OutputWaveCanvas
import io.github.xsyetopz.jagfx.ui.editor.EditorState
import io.github.xsyetopz.jagfx.ui.editor.RequestIntent
import io.github.xsyetopz.jagfx.ui.editor.SampleDocument
import io.github.xsyetopz.jagfx.ui.editor.SampleIntent
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.editor.ViewIntent
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.widgets.Icons
import io.github.xsyetopz.jagfx.ui.widgets.JagButton
import io.github.xsyetopz.jagfx.ui.widgets.JagField
import io.github.xsyetopz.jagfx.ui.widgets.JagIconButton
import io.github.xsyetopz.jagfx.ui.widgets.JagLabel
import io.github.xsyetopz.jagfx.ui.widgets.JagText

private val FieldWidth = 80.dp
private val CountWidth = 36.dp
private const val MAX_SAMPLE_VALUE = Int.MAX_VALUE
private const val MS_PER_SECOND = 1000L
private const val MS_PER_TENTH = 100L

/** Milliseconds of [count] samples at [sampleRate], rounded down; `0` without a rate. */
internal fun sampleDurationMs(
    count: Int,
    sampleRate: Int,
): Long = if (sampleRate > 0) count * MS_PER_SECOND / sampleRate else 0L

/** Seconds with one decimal, for example `1.5 s`. */
internal fun formatSeconds(ms: Long): String =
    "${ms / MS_PER_SECOND}.${ms % MS_PER_SECOND / MS_PER_TENTH} s"

/** The sample view: file and transport controls, the waveform with loop markers, and the fields. */
@Composable
internal fun SampleContent(
    state: EditorState,
    document: SampleDocument,
    view: ScreenViewState,
    actions: EditorActions,
    modifier: Modifier = Modifier,
) {
    val dimens = JagFxTheme.dimens
    Column(
        modifier
            .fillMaxSize()
            .background(JagFxTheme.colors.root),
    ) {
        SampleToolbar(
            state,
            document,
            actions,
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(dimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceLg),
        ) {
            SampleWave(state, document, view, actions, Modifier.weight(1f).fillMaxWidth())
            SampleFields(document, actions)
        }
        StatusBar(state.statusHint, Modifier.fillMaxWidth())
    }
}

@Composable
private fun SampleToolbar(
    state: EditorState,
    document: SampleDocument,
    actions: EditorActions,
    modifier: Modifier,
) {
    val strings = LocalStrings.current
    val dimens = JagFxTheme.dimens
    Row(
        modifier
            .height(dimens.headerHeight)
            .background(JagFxTheme.colors.header)
            .padding(horizontal = dimens.spaceXl),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JagButton(strings[UiString.SampleClose], { actions.dispatch(RequestIntent.CloseSample) })
        JagText(document.name + if (document.isDirty) " *" else "")
        JagButton(
            strings[UiString.ToolbarOpenSample],
            { actions.dispatch(RequestIntent.OpenSample) },
        )
        JagIconButton(Icons.Save, strings[UiString.TooltipSaveFile], {
            actions.dispatch(RequestIntent.SaveSample)
        })
        JagButton(strings[UiString.ToolbarSaveAs], { actions.dispatch(RequestIntent.SaveSampleAs) })
        JagIconButton(Icons.Play, strings[UiString.TooltipPlay], {
            actions.dispatch(RequestIntent.TogglePlay)
        })
        JagIconButton(
            Icons.Stop,
            strings[UiString.TooltipStop],
            { actions.dispatch(RequestIntent.Stop) },
        )
        JagIconButton(
            Icons.Loop,
            strings[UiString.TooltipLoopPlayback],
            { actions.dispatch(ViewIntent.SetLooping(!state.isLooping)) },
            active = state.isLooping,
        )
        JagLabel(strings[UiString.HeaderLoopCount])
        JagField(
            state.loopCount,
            { actions.dispatch(ViewIntent.SetLoopCount(it)) },
            0..EditorState.MAX_LOOP_COUNT,
            Modifier.width(CountWidth),
        )
    }
}

@Composable
private fun SampleWave(
    state: EditorState,
    document: SampleDocument,
    view: ScreenViewState,
    actions: EditorActions,
    modifier: Modifier,
) {
    val sample = document.sample
    val slot = SignalChainSlot.Output
    val samples =
        remember(document.preview, sample.sampleCount) {
            document.preview.samples.copyOf(
                minOf(sample.sampleCount, document.preview.samples.size),
            )
        }
    val count = sample.sampleCount.coerceAtLeast(1).toFloat()
    val canvasView = view.canvas(slot)
    val looped = state.isLooping && state.effectiveLoopCount > 1
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(JagFxTheme.dimens.spaceMd),
    ) {
        JagText(document.name, style = JagFxTheme.typography.heading)
        OutputWaveCanvas(
            samples = samples,
            playbackPosition = if (looped) 0f else state.playbackPosition,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            panZoom = actions.panZoom(slot, canvasView),
            loopMarkers = listOf(sample.loopBegin / count, sample.loopEnd / count),
        )
    }
}

@Composable
private fun SampleFields(
    document: SampleDocument,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    val sample = document.sample
    val dimens = JagFxTheme.dimens
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXl * 2),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceLg),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Labeled(strings[UiString.SampleCountLabel]) {
            JagField(
                sample.sampleCount,
                { actions.dispatch(SampleIntent.SetSampleCount(it)) },
                0..document.loadedSampleCount,
                Modifier.width(FieldWidth),
            )
        }
        Labeled(strings[UiString.HeaderLoopStart]) {
            JagField(
                sample.loopBegin,
                { actions.dispatch(SampleIntent.SetLoopBegin(it)) },
                0..MAX_SAMPLE_VALUE,
                Modifier.width(FieldWidth),
            )
        }
        Labeled(strings[UiString.HeaderLoopEnd]) {
            JagField(
                sample.loopEnd,
                { actions.dispatch(SampleIntent.SetLoopEnd(it)) },
                0..MAX_SAMPLE_VALUE,
                Modifier.width(FieldWidth),
            )
        }
        JagButton(
            strings[UiString.SamplePingPong],
            { actions.dispatch(SampleIntent.SetPingPong(!sample.pingPong)) },
            active = sample.pingPong,
        )
        ReadOnly(strings[UiString.SampleRateLabel], "${sample.sampleRate} Hz")
        ReadOnly(
            strings[UiString.SampleDurationLabel],
            formatSeconds(sampleDurationMs(sample.sampleCount, sample.sampleRate)),
        )
        ReadOnly(strings[UiString.SamplePacketsLabel], sample.packetCount.toString())
    }
}

@Composable
private fun Labeled(
    label: String,
    content: @Composable () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(JagFxTheme.dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JagLabel(label)
        content()
    }
}

@Composable
private fun ReadOnly(
    label: String,
    value: String,
) {
    Labeled(label) { JagText(value) }
}
