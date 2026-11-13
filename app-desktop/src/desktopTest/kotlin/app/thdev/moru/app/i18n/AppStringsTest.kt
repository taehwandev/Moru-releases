package app.thdev.moru.app.i18n

import app.thdev.moru.app.ProjectTab
import app.thdev.moru.app.MoruDestination
import app.thdev.moru.app.contextDetail
import app.thdev.moru.app.contextTitle
import app.thdev.moru.core.designsystem.theme.UiLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Both languages of the app shell, side by side. */
class AppStringsTest {
    private val english = appStrings(UiLanguage.English)
    private val korean = appStrings(UiLanguage.Korean)
    private val hangul = Regex("[\\uAC00-\\uD7A3]")

    private fun AppStrings.allText(): List<String> =
        listOf(
            selectProject, newTask, noProjectSelected, readyForTask("P"), branchDetail("b", "p"), folderDetail("p"),
            welcomeEmptyTitle, welcomeEmptyBody, welcomeProjectBody, openProject, terminalHint,
            unavailableTitle, unavailableBody("P"), preparingTask, taskCouldNotStart, noAgentLaunched, retry,
            switchWhilePreparing, taskBrief, hideTaskBrief, dismiss, loadingProjectFiles,
            unknownError, couldNotChooseFolder("m"), couldNotOpenProject("m"),
            couldNotStart("T", "m"),
            quitTitle, quitEndsTerminals(1), quitEndsTerminals(3), quitKeepTerminals, quitCancel, quitConfirm,
            startupFailedTitle, startupFailed("r", "l"), startupNeedsNewerMoru("l"), startupReportIssue, startupClose,
            crashTitle, crashBody("r", "l"), crashLastTimeTitle, crashLastTime("r", "l"),
        ) + ProjectTab.entries.map(::tabLabel) + MoruDestination.entries.map(::destinationLabel)

    @Test
    fun noStringInEitherLanguageIsBlank() {
        assertTrue(english.allText().all { it.isNotBlank() })
        assertTrue(korean.allText().all { it.isNotBlank() })
    }

    @Test
    fun koreanTextIsKoreanAndEnglishTextHasNoKoreanInIt() {
        assertFalse(english.allText().any { hangul.containsMatchIn(it) })
        assertTrue(korean.tabLabel(ProjectTab.Code).contains(hangul))
        assertTrue(korean.destinationLabel(MoruDestination.Settings).contains(hangul))
        assertTrue(korean.noAgentLaunched.contains(hangul))
    }

    @Test
    fun everyTabAndDestinationIsDistinctInBothLanguages() {
        listOf(english, korean).forEach { table ->
            assertEquals(ProjectTab.entries.size, ProjectTab.entries.map(table::tabLabel).toSet().size)
            assertEquals(MoruDestination.entries.size, MoruDestination.entries.map(table::destinationLabel).toSet().size)
        }
    }

    @Test
    fun theEnglishTabAndDestinationNamesAreTheOnesThePreviousScreensShowed() {
        assertEquals(listOf("Work", "Code", "Git"), ProjectTab.entries.map(english::tabLabel))
        assertEquals(listOf("Workbench", "Knowledge", "Notifications", "Settings"), MoruDestination.entries.map(english::destinationLabel))
    }

    @Test
    fun parametersAndFailureMessagesCarryTheirDetailInBothLanguages() {
        listOf(english, korean).forEach { table ->
            assertTrue(table.unavailableBody("moru-api").contains("moru-api"))
            assertTrue(table.couldNotStart("Fix it", "disk full").let { it.contains("Fix it") && it.contains("disk full") })
            assertTrue(table.couldNotChooseFolder("denied").contains("denied"))
            assertTrue(table.couldNotOpenProject("missing").contains("missing"))
        }
    }

    @Test
    fun theContextLineFollowsTheLanguageItIsGiven() {
        assertEquals("No project selected", contextTitle(null, null, english))
        assertEquals("선택된 프로젝트 없음", contextTitle(null, null, korean))
        assertEquals("Moru › 작업 준비됨", contextTitle("Moru", null, korean))
        assertEquals("브랜치 main · …/git/Moru", contextDetail("main", "/Users/me/git/Moru", korean))
        assertEquals("프로젝트 폴더 · …/git/Moru", contextDetail(null, "/Users/me/git/Moru", korean))
    }
}
