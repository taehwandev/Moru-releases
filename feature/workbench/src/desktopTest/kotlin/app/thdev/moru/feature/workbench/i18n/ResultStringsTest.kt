package app.thdev.moru.feature.workbench.i18n

import app.thdev.moru.core.designsystem.theme.UiLanguage
import app.thdev.moru.feature.workbench.result.ResultTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The Result pane text in both languages. */
class ResultStringsTest {
    private val english = resultStrings(UiLanguage.English)
    private val korean = resultStrings(UiLanguage.Korean)
    private val hangul = Regex("[\\uAC00-\\uD7A3]")

    private fun ResultStrings.allText(): List<String> = ResultTab.entries.map { tab(it) } + listOf(
        noChanges, reviseLabel, revisePlaceholder, reviseButton, revised, reviseNoAgent,
    )

    @Test
    fun noStringInEitherLanguageIsBlank() {
        assertTrue(english.allText().all { it.isNotBlank() })
        assertTrue(korean.allText().all { it.isNotBlank() })
    }

    @Test
    fun everyTabHasItsOwnName() {
        listOf(english, korean).forEach { table ->
            val names = ResultTab.entries.map { table.tab(it) }
            assertEquals(names.size, names.toSet().size)
        }
    }
}
