package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.xsyetopz.jagfx.ui.editor.EditorState
import io.github.xsyetopz.jagfx.ui.editor.EditorViewModel
import io.github.xsyetopz.jagfx.ui.editor.PanelSide
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme

/** The editor: collects [viewModel] state and shows the sample view or the synth editor. */
@Composable
internal fun EditorScreen(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var view by remember { mutableStateOf(ScreenViewState()) }
    val actions = remember(viewModel) { EditorActions(viewModel::dispatch) { view = it(view) } }
    val sample = state.sample
    if (sample != null) {
        SampleContent(state, sample, view, actions, modifier)
    } else {
        EditorContent(state, view, actions, modifier)
    }
}

/**
 * Stateless editor content for [state] and [view]: the title bar, the voice strip on the side of
 * [EditorState.panelSide] with the rack, the bottom bar, and the status line.
 */
@Composable
internal fun EditorContent(
    state: EditorState,
    view: ScreenViewState,
    actions: EditorActions,
    modifier: Modifier = Modifier,
) {
    val dimens = JagFxTheme.dimens
    Column(modifier.fillMaxSize().background(JagFxTheme.colors.root)) {
        TitleBar(state, actions, Modifier.fillMaxWidth())
        Row(Modifier.weight(1f).fillMaxWidth()) {
            val strip: @Composable () -> Unit = {
                VoiceStrip(
                    state,
                    actions,
                    Modifier.fillMaxHeight(),
                )
            }
            if (state.panelSide == PanelSide.Left) strip()
            Rack(state, view, actions, Modifier.weight(1f).fillMaxHeight().padding(dimens.spaceSm))
            if (state.panelSide == PanelSide.Right) strip()
        }
        BottomBar(state, view, actions, Modifier.fillMaxWidth())
        StatusBar(state.statusHint, Modifier.fillMaxWidth())
    }
}
