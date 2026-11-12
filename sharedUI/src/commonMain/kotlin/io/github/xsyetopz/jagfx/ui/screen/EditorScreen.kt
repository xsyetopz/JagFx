package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.xsyetopz.jagfx.ui.editor.EditorState
import io.github.xsyetopz.jagfx.ui.editor.EditorViewModel
import io.github.xsyetopz.jagfx.ui.editor.PanelSide
import io.github.xsyetopz.jagfx.ui.editor.RequestIntent
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
    } else if (!state.isFileOpen) {
        NoFileScreen(state, actions, modifier)
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
    val rowDirection =
        if (state.panelSide ==
            PanelSide.Left
        ) {
            LayoutDirection.Ltr
        } else {
            LayoutDirection.Rtl
        }
    Column(
        modifier
            .fillMaxSize()
            .background(JagFxTheme.colors.root)
            .onKeyEvent { event ->
                val stop =
                    event.type == KeyEventType.KeyDown && stopsPlayback(event.key, state.isPlaying)
                if (stop) actions.dispatch(RequestIntent.Stop)
                stop
            },
    ) {
        TitleBar(state, actions, Modifier.fillMaxWidth())
        // The strip is composed first so Tab reaches it before the rack; a right-to-left row
        // still draws it on the right, and each child restores left-to-right layout.
        CompositionLocalProvider(LocalLayoutDirection provides rowDirection) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    VoiceStrip(state, actions, Modifier.fillMaxHeight())
                    Rack(
                        state,
                        view,
                        actions,
                        Modifier.weight(1f).fillMaxHeight().padding(dimens.spaceSm),
                    )
                }
            }
        }
        BottomBar(state, actions, Modifier.fillMaxWidth())
        StatusBar(state.statusHint, Modifier.fillMaxWidth())
    }
}
