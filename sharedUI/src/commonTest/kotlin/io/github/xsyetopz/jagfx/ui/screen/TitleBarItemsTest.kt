package io.github.xsyetopz.jagfx.ui.screen

import kotlin.test.Test
import kotlin.test.assertEquals

class TitleBarItemsTest {
    @Test
    fun aNativeMenuLeavesOnlyTheFileName() {
        assertEquals(TitleBarItems(fileMenu = false, history = false), titleBarItems(true))
    }

    @Test
    fun withoutANativeMenuTheTitleBarKeepsTheMenuAndHistory() {
        assertEquals(TitleBarItems(fileMenu = true, history = true), titleBarItems(false))
    }
}
