package app.thdev.moru.feature.settings.i18n

import app.thdev.moru.core.designsystem.theme.UiLanguage
import app.thdev.moru.core.model.AppLanguage
import app.thdev.moru.core.model.AppTheme
import app.thdev.moru.core.model.TaskSortOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The Settings text in both languages. */
class SettingsStringsTest {
    private val english = settingsStrings(UiLanguage.English)
    private val korean = settingsStrings(UiLanguage.Korean)
    private val hangul = Regex("[\\uAC00-\\uD7A3]")

    private fun SettingsStrings.allText(): List<String> =
        listOf(
            title, subtitle, automaticSaveHint, savingMessage, savedMessage, saveFailed("x"), unknownError,
            retryButton, invalidNumbersHint, numberRange(10, 28), fontSizeTitle,
            updatesTitle, appUpdatesTitle,
            updatesDescription, currentVersion("26.41.3"), versionUnavailable,
            automaticUpdates, updatesUnavailable, checkForUpdates, restartToUpdate,
            generalTitle, generalDescription, rememberStateTitle, rememberStateDescription,
            autoCollapseSidebarTitle, autoCollapseSidebarDescription,
            taskSortOrderTitle, taskSortOrderDescription, appearanceTitle,
            appearanceDescription, appleStyleTitle, appleStyleDescription,
            themeTitle, themeDescription, languageTitle, languageDescription, fontTitle,
            fontDescription, fontPlaceholder, uiFontTitle, uiFontSizeTitle, uiFontDescription,
            searchFonts, noFontsFound, fontPreview, resetFont, decreaseFontSize("IDE"), increaseFontSize("IDE"),
            terminalTitle, terminalDescription, shellTitle, shellDescription,
            scrollbackTitle, scrollbackDescription, agentClisTitle, agentClisDescription, agentClisLooking,
            noAgentCliTitle, noAgentCliDescription, cliUpdatedOutsideMoru, cliUpdate, cliUpdating, cliUpdated("2.1.300"),
            cliAlreadyLatest("2.1.300"), cliUpdateFailed(null, "claude update"),
        ) + AppTheme.entries.map { themeLabel(it) } + AppLanguage.entries.map { languageLabel(it) } +
            TaskSortOrder.entries.map { taskSortOrderLabel(it) }

    @Test
    fun noStringInEitherLanguageIsBlank() {
        assertTrue(english.allText().all { it.isNotBlank() })
        assertTrue(korean.allText().all { it.isNotBlank() })
    }

    @Test
    fun englishTextHasNoKoreanExceptTheKoreanLanguageName() {
        val offenders = english.allText().filter { hangul.containsMatchIn(it) }
        assertEquals(listOf("한국어"), offenders)
        val translatedKoreanItems = listOf(
            korean.updatesTitle,
            korean.appUpdatesTitle,
            korean.updatesDescription,
            korean.currentVersion("26.41.3"),
            korean.versionUnavailable,
            korean.automaticUpdates,
            korean.updatesUnavailable,
            korean.checkForUpdates,
            korean.restartToUpdate,
            korean.appearanceDescription,
            korean.appleStyleTitle,
            korean.appleStyleDescription,
            korean.fontDescription,
            korean.uiFontTitle,
            korean.uiFontSizeTitle,
            korean.uiFontDescription,
            korean.searchFonts,
            korean.noFontsFound,
            korean.fontPreview,
            korean.resetFont,
            korean.decreaseFontSize("IDE"),
            korean.increaseFontSize("IDE"),
        )
        assertEquals(22, translatedKoreanItems.size)
        translatedKoreanItems.forEach { text ->
            assertTrue(hangul.containsMatchIn(text), "Expected Hangul in '$text'")
        }
    }

    @Test
    fun languageNamesAreShownInTheirOwnLanguageWhateverIsSelected() {
        listOf(english, korean).forEach { table ->
            assertEquals("English", table.languageLabel(AppLanguage.English))
            assertEquals("한국어", table.languageLabel(AppLanguage.Korean))
        }
    }

    @Test
    fun themeAndLanguageOptionsStayDistinguishable() {
        listOf(english, korean).forEach { table ->
            val themes = AppTheme.entries.map { table.themeLabel(it) }
            val languages = AppLanguage.entries.map { table.languageLabel(it) }
            assertEquals(themes.size, themes.toSet().size)
            assertEquals(languages.size, languages.toSet().size)
        }
    }

    @Test
    fun taskOrderLabelsMatchTheAvailableModesInBothLanguages() {
        assertEquals(listOf("Oldest first", "Newest first", "Manual"), TaskSortOrder.entries.map { english.taskSortOrderLabel(it) })
        assertEquals(listOf("오래된 작업부터", "최신 작업부터", "수동"), TaskSortOrder.entries.map { korean.taskSortOrderLabel(it) })
    }

}
