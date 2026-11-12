package io.github.xsyetopz.jagfx.ui.widgets

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TipTest {
    private fun ComposeUiTest.anchor() = onNodeWithTag("anchor", useUnmergedTree = true)

    private fun ComposeUiTest.show() =
        setContent {
            Tip("Hint") { Box(Modifier.size(40.dp).testTag("anchor").focusable()) }
        }

    @Test
    fun hoveringTheAnchorShowsTheTooltipAndMovingOntoItKeepsItOpen() =
        runComposeUiTest {
            show()
            anchor().performMouseInput { moveTo(center) }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Hint").fetchSemanticsNodes().isNotEmpty()
            }
            anchor().performMouseInput { moveTo(bottomRight) }
            onNodeWithText("Hint").performMouseInput { moveTo(center) }
            mainClock.advanceTimeBy(2_000)
            onNodeWithText("Hint").assertIsDisplayed()
        }

    @Test
    fun keyboardFocusShowsTheTooltipAndEscapeClosesIt() =
        runComposeUiTest {
            show()
            anchor().requestFocus()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Hint").fetchSemanticsNodes().isNotEmpty()
            }
            anchor().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            onNodeWithText("Hint").assertDoesNotExist()
        }
}
