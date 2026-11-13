package app.thdev.moru.feature.workbench.i18n

import app.thdev.moru.core.designsystem.theme.UiLanguage
import app.thdev.moru.feature.workbench.TaskDisplayState
import app.thdev.moru.feature.workbench.TaskGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Both languages, side by side: nothing is blank, Korean reads as Korean, and the numbers survive. */
class WorkbenchStringsTest {
    private val english = workbenchStrings(UiLanguage.English)
    private val korean = workbenchStrings(UiLanguage.Korean)
    private val hangul = Regex("[\\uAC00-\\uD7A3]")

    /** Every plain and parameterised string of a table, fed distinct sample arguments. */
    private fun WorkbenchStrings.allText(): List<String> =
        listOf(
            workspaceTitle, openProjectButton, reorderSidebarButton, finishReorderSidebarButton,
            closedProjects(2), reopenProject("P"),
            newTaskLabel, newTaskIn("P"),
            emptyTitle, emptyBody, noTasksYet, collapseProject("P"), expandProject("P"), projectDescription("P"),
            folderUnavailable, noFolderLinked,
            projectMenu("P"),
            codexThemeChanged("T", "s1"), codexThemeRestartDetails(true), codexThemeRestartDetails(false),
            themeRestartBusy, themeRestartInProgress, themeRestartFailed, restartForTheme,
            tasksNeedYou(2, "P"), taskDescription("T"), taskSessions(2, 1, 1), terminalLabel,
            sessionRunning(3), sessionNeedsAttention(2), sessionResponsesReady(1), sessionWaiting(4), sessionEnded(5),
            waiting(2), working(3), noActiveTasks,
        ) + TaskDisplayState.entries.map(::statusLabel) + TaskGroup.entries.map(::sectionLabel)

    @Test
    fun noStringInEitherLanguageIsBlank() {
        assertTrue(english.allText().all { text -> text.isNotBlank() })
        assertTrue(korean.allText().all { text -> text.isNotBlank() })
    }

    @Test
    fun koreanTextIsKoreanAndEnglishTextHasNoKoreanInIt() {
        assertFalse(english.allText().any { text -> hangul.containsMatchIn(text) })
        assertTrue(korean.statusLabel(TaskDisplayState.NeedsApproval).contains(hangul))
        assertTrue(korean.sectionLabel(TaskGroup.NeedsAttention).contains(hangul))
        assertTrue(korean.emptyBody.contains(hangul))
        assertTrue(korean.taskSessions(2, 1, 1).contains(hangul))
        assertTrue(korean.restartForTheme.contains(hangul))
    }

    @Test
    fun everyTaskStateAndGroupHasADistinctLabelInBothLanguages() {
        listOf(english, korean).forEach { table ->
            assertEquals(TaskDisplayState.entries.size, TaskDisplayState.entries.map(table::statusLabel).toSet().size)
            assertEquals(TaskGroup.entries.size, TaskGroup.entries.map(table::sectionLabel).toSet().size)
        }
    }

    @Test
    fun parametersAreCarriedIntoTheTextInBothLanguages() {
        listOf(english, korean).forEach { table ->
            assertTrue(table.reopenProject("moru-api").contains("moru-api"))
            assertTrue(table.closedProjects(5).contains("5"))
        }
    }

}
