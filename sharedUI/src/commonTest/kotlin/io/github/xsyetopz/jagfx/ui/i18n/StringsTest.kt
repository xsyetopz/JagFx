package io.github.xsyetopz.jagfx.ui.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StringsTest {
    @Test
    fun everyLocaleHasExactlyTheEnglishKeySet() {
        assertEquals(111, stringsEn.size)
        assertEquals(UiString.entries.toSet(), stringsEn.keys)
        for (locale in UiLocale.entries) {
            assertEquals(stringsEn.keys, locale.table.keys, locale.tag)
            assertTrue(locale.table.values.none { it.isEmpty() }, locale.tag)
        }
    }

    @Test
    fun resolvesExactAndLanguageMatches() {
        assertEquals(UiLocale.PtBr, resolveLocale("pt-BR"))
        assertEquals(UiLocale.PtBr, resolveLocale("pt_br"))
        assertEquals(UiLocale.PtBr, resolveLocale("pt-PT"))
        assertEquals(UiLocale.De, resolveLocale("de-AT"))
        assertEquals(UiLocale.Fr, resolveLocale("fr"))
        assertEquals(UiLocale.En, resolveLocale("en-US"))
        assertEquals(UiLocale.En, resolveLocale("ja"))
        assertEquals(UiLocale.En, resolveLocale(""))
    }

    @Test
    fun formatSubstitutesPlaceholders() {
        val strings = Strings(UiLocale.En)
        assertEquals("Loaded a.synth", strings.format(UiString.StatusLoadedFile, "a.synth"))
        assertEquals(
            "Could not open a.synth: boom",
            strings.format(UiString.StatusCouldNotOpenFile, "a.synth", "boom"),
        )
    }

    @Test
    fun missingKeyFallsBackToEnglish() {
        val partial = UiLocale.De.table - UiString.HeaderLoopStart
        assertEquals(null, partial[UiString.HeaderLoopStart])
        assertEquals(
            stringsEn[UiString.HeaderLoopStart],
            Strings(UiLocale.En)[UiString.HeaderLoopStart],
        )
    }
}
