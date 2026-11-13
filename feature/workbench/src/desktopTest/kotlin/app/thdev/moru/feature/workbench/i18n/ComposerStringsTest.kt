package app.thdev.moru.feature.workbench.i18n

import app.thdev.moru.core.designsystem.theme.UiLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The composer text in both languages. */
class ComposerStringsTest {
    private val english = composerStrings(UiLanguage.English)
    private val korean = composerStrings(UiLanguage.Korean)
    private val hangul = Regex("[\\uAC00-\\uD7A3]")

    private fun ComposerStrings.allText(): List<String> =
        listOf(headingTitle, headingBody, preparingTask, cancel)

    @Test
    fun noStringInEitherLanguageIsBlank() {
        assertTrue(english.allText().all { it.isNotBlank() })
        assertTrue(korean.allText().all { it.isNotBlank() })
    }

    @Test
    fun koreanTextIsKoreanAndEnglishTextHasNoKoreanInIt() {
        assertFalse(english.allText().any { hangul.containsMatchIn(it) })
        assertTrue(korean.headingTitle.contains(hangul))
        assertTrue(korean.headingBody.contains(hangul))
    }

    @Test
    fun theEnglishTextIsWhatTheComposerShowsNow() {
        assertEquals("Preparing task…", english.preparingTask)
    }
}
