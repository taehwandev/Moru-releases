package app.thdev.moru.feature.git.i18n

import app.thdev.moru.core.designsystem.theme.UiLanguage
import app.thdev.moru.core.ui.i18n.CoreUiStrings
import app.thdev.moru.core.ui.i18n.coreUiStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The History destination's text, and the shared diff and time text it shows, in both languages. */
class HistoryStringsTest {
    private val english = historyStrings(UiLanguage.English)
    private val korean = historyStrings(UiLanguage.Korean)
    private val englishShared = coreUiStrings(UiLanguage.English)
    private val koreanShared = coreUiStrings(UiLanguage.Korean)
    private val hangul = Regex("[\\uAC00-\\uD7A3]")

    private fun HistoryStrings.allText(): List<String> =
        listOf(
            title, refresh, loading, noCommits, commitCount(3, false), commitCount(3, true), diffNotConnected,
            diffLoading, noFileChanges, authorLine("A", "a@b", true, 2), authorLine("A", "a@b", false, 1),
            changesTitle, changesSubtitle, changesLoading, noChanges, resizeGitDetail, resetPaneSize,
        )

    private fun CoreUiStrings.allText(): List<String> =
        listOf(
            binaryFile, renamedOnly, noLineChanges, filesSummary(2, 3, 4, false), filesSummary(2, 3, 4, true),
            agoJustNow, agoMinutes(1), agoHours(1), agoDays(1), agoWeeks(1), agoMonths(1), agoYears(1),
        )

    @Test
    fun noStringInEitherLanguageIsBlank() {
        assertTrue(english.allText().all { it.isNotBlank() })
        assertTrue(korean.allText().all { it.isNotBlank() })
        assertTrue(englishShared.allText().all { it.isNotBlank() })
        assertTrue(koreanShared.allText().all { it.isNotBlank() })
    }

    @Test
    fun koreanTextIsKoreanAndEnglishTextHasNoKoreanInIt() {
        assertFalse(english.allText().any { hangul.containsMatchIn(it) })
        assertFalse(englishShared.allText().any { hangul.containsMatchIn(it) })
        assertTrue(korean.title.contains(hangul))
        assertTrue(koreanShared.agoDays(2).contains(hangul))
    }

    @Test
    fun englishCountsAreSingularForOne() {
        assertEquals("1 commit", english.commitCount(1, false))
        assertEquals("2 commits · recent history only", english.commitCount(2, true))
        assertTrue(english.authorLine("A", "a@b", false, 1).endsWith("1 parent"))
        assertTrue(english.authorLine("A", "a@b", false, 2).endsWith("2 parents"))
    }

    @Test
    fun authorAndEmailAreCarriedThrough() {
        listOf(english, korean).forEach { assertTrue(it.authorLine("Ann", "ann@x.dev", false, 1).contains("Ann <ann@x.dev>")) }
    }
}
