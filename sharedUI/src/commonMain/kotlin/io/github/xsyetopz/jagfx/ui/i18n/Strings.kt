package io.github.xsyetopz.jagfx.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.intl.Locale

internal enum class UiLocale(
    val tag: String,
    val table: Map<UiString, String>,
) {
    En("en", stringsEn),
    Fr("fr", stringsFr),
    De("de", stringsDe),
    Es("es", stringsEs),
    It("it", stringsIt),
    PtBr("pt-BR", stringsPtBr),
}

private val placeholder = Regex("""\{(\d+)}""")

private fun languageOf(tag: String): String = tag.substringBefore('-')

/** Exact tag match, then language match, then English. */
internal fun resolveLocale(languageTag: String): UiLocale {
    val requested = languageTag.replace('_', '-')
    val exact = UiLocale.entries.firstOrNull { it.tag.equals(requested, ignoreCase = true) }
    return exact
        ?: UiLocale.entries.firstOrNull {
            languageOf(it.tag).equals(languageOf(requested), ignoreCase = true)
        }
        ?: UiLocale.En
}

internal class Strings(
    val locale: UiLocale,
) {
    operator fun get(key: UiString): String = locale.table[key] ?: stringsEn.getValue(key)

    fun format(
        key: UiString,
        vararg args: Any?,
    ): String =
        placeholder.replace(get(key)) { match ->
            val index = match.groupValues[1].toInt()
            if (index < args.size) args[index].toString() else match.value
        }
}

internal val LocalStrings: ProvidableCompositionLocal<Strings> =
    compositionLocalOf { Strings(UiLocale.En) }

@Composable
internal fun rememberStrings(): Strings {
    val tag = Locale.current.toLanguageTag()
    return remember(tag) { Strings(resolveLocale(tag)) }
}
