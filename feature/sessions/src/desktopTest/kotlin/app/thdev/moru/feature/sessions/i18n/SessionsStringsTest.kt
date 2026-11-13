package app.thdev.moru.feature.sessions.i18n

import app.thdev.moru.core.designsystem.theme.UiLanguage
import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.SessionActivity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The Sessions panel text in both languages. */
class SessionsStringsTest {
    private val english = sessionsStrings(UiLanguage.English)
    private val korean = sessionsStrings(UiLanguage.Korean)
    private val hangul = Regex("[\\uAC00-\\uD7A3]")

    private fun SessionsStrings.allText(): List<String> =
        listOf(
            title, summary(3, 1, 2), empty, unattachedGroup, scopeAll, scopeThisProject, exitCode(2), runningProgram,
            terminal, webPreview, openPreview("localhost:5173"), rowDescription("A", "T", "S"), permissionRequestLabel,
            boardNeedsAttention, boardRunning, boardCompleted, boardReady, boardNothingNeedsAttention,
            boardNothingRunning, boardNothingCompleted, boardNothingReady,
        ) + SessionActivity.entries.map { activityLabel(it) } + SessionActivity.entries.mapNotNull { activityHint(it) }

    @Test
    fun noStringInEitherLanguageIsBlank() {
        assertTrue(english.allText().all { it.isNotBlank() })
        assertTrue(korean.allText().all { it.isNotBlank() })
    }

    @Test
    fun koreanTextIsKoreanAndEnglishTextHasNoKoreanInIt() {
        assertFalse(english.allText().any { hangul.containsMatchIn(it) })
        assertTrue(korean.title.contains(hangul))
        assertTrue(korean.activityLabel(SessionActivity.Quiet).contains(hangul))
    }

    @Test
    fun thePreviewLinkNamesTheAddressItOpens() {
        listOf(english, korean).forEach { table -> assertTrue(table.openPreview("localhost:5173").contains("localhost:5173")) }
    }

    @Test
    fun everyActivityHasItsOwnLabelAndOnlyGuessedStatesHaveAHint() {
        listOf(english, korean).forEach { table ->
            val labels = SessionActivity.entries.map { table.activityLabel(it) }
            assertEquals(labels.size, labels.toSet().size)
            assertNotNull(table.activityHint(SessionActivity.Quiet))
            assertNotNull(table.activityHint(SessionActivity.Exited))
            assertNull(table.activityHint(SessionActivity.Working))
            assertNull(table.activityHint(SessionActivity.Failed))
        }
    }

    @Test
    fun aQuestionAndAPermissionRequestHaveDifferentVisibleLabels() {
        listOf(english, korean).forEach { table ->
            assertTrue(table.activityLabel(SessionActivity.Asking) != table.permissionRequestLabel)
        }
    }

    @Test
    fun finishedResponseHeadingsStayDistinctFromTaskCompletionAndWaiting() {
        assertEquals("Response ready", english.boardCompleted)
        assertEquals("응답 완료", korean.boardCompleted)
        assertEquals(english.boardCompleted, english.activityLabel(SessionActivity.Quiet))
        assertEquals(korean.boardCompleted, korean.activityLabel(SessionActivity.Quiet))
        assertEquals("Waiting", english.boardReady)
        assertEquals("대기 중", korean.boardReady)
    }

    @Test
    fun programNamesAreShownAsTheyAreAndTheOtherRuntimesAreDescribed() {
        listOf(english, korean).forEach { table ->
            assertEquals("aider", table.agentName(AgentRuntime.Named("aider")))
            assertEquals(table.terminal, table.agentName(AgentRuntime.Terminal))
            assertEquals(table.webPreview, table.agentName(AgentRuntime.WebPreview))
        }
    }
}
