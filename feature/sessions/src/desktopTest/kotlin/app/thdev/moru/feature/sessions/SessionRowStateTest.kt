package app.thdev.moru.feature.sessions

import app.thdev.moru.core.designsystem.component.MoruBadgeType
import app.thdev.moru.core.designsystem.component.MoruStatusGlyphKind
import app.thdev.moru.core.designsystem.theme.UiLanguage
import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.feature.sessions.i18n.sessionsStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Session labels distinguish the next decision and an observed response from a completed Task. */
class SessionRowStateTest {
    private val korean = sessionsStrings(UiLanguage.Korean)

    private fun row(activity: SessionActivity, agent: AgentRuntime) = AgentSessionRow(
        sessionId = "s", title = "t", agent = agent, activity = activity, lastOutputAtMillis = 1_000, nowMillis = 2_000,
        exitCode = null, previewUrl = null, selected = false,
    )

    @Test
    fun anAgentThatStoppedPrintingHasAResponseReadyRatherThanACompletedTask() {
        val state = sessionRowState(row(SessionActivity.Quiet, AgentRuntime.Named("Claude")), korean)

        assertEquals("응답 완료", state.label)
        assertEquals(MoruStatusGlyphKind.Done, state.glyph)
        assertEquals(MoruBadgeType.SUCCESS, state.badgeType)
    }

    @Test
    fun aQuietPlainTerminalIsStillRunningItsProgram() {
        val state = sessionRowState(row(SessionActivity.Quiet, AgentRuntime.Terminal), korean)

        assertEquals("실행 중", state.label)
        assertEquals(MoruStatusGlyphKind.Working, state.glyph)
        assertNull(sessionRowHint(row(SessionActivity.Quiet, AgentRuntime.Terminal), korean))
        assertNotNull(sessionRowHint(row(SessionActivity.Quiet, AgentRuntime.Named("Claude")), korean))
    }

    @Test
    fun theOtherStatesUseTheirOwnWords() {
        val claude = AgentRuntime.Named("Claude")
        assertEquals("작업 중", sessionRowState(row(SessionActivity.Working, claude), korean).label)
        assertEquals("답변 필요", sessionRowState(row(SessionActivity.Asking, claude), korean).label)
        assertEquals("셸 대기", sessionRowState(row(SessionActivity.Idle, AgentRuntime.Terminal), korean).label)
        assertEquals("실패", sessionRowState(row(SessionActivity.Failed, claude), korean).label)
        assertEquals(MoruStatusGlyphKind.Idle, sessionRowState(row(SessionActivity.Exited, claude), korean).glyph)
    }

    @Test
    fun aLivePermissionRequestHasItsOwnLabelEvenBeforeActivityChangesToAsking() {
        val working = row(SessionActivity.Working, AgentRuntime.Named("Claude"))

        assertEquals("승인 필요", sessionRowState(working, korean, hasPermissionRequest = true).label)
        assertEquals(MoruBadgeType.WARNING, sessionRowState(working, korean, hasPermissionRequest = true).badgeType)
        assertEquals("작업 중", sessionRowState(working, korean).label)
    }

    @Test
    fun aLivePermissionRequestKeepsACompletedTurnProminent() {
        val completed = row(SessionActivity.Quiet, AgentRuntime.Named("Codex"))
        val state = sessionRowState(completed, korean, hasPermissionRequest = true)

        assertEquals("승인 필요", state.label)
        assertEquals(MoruBadgeType.WARNING, state.badgeType)
        assertEquals(SessionRowEmphasis.Plain, state.emphasis)
    }

    @Test
    fun finishedResponsesStayReadableWhileWaitingIdleAndEndedSessionsFade() {
        val claude = AgentRuntime.Named("Claude")
        fun emphasis(activity: SessionActivity, agent: AgentRuntime = claude) = sessionRowState(row(activity, agent), korean).emphasis

        assertEquals(SessionRowEmphasis.Active, emphasis(SessionActivity.Working))
        assertEquals(SessionRowEmphasis.Active, emphasis(SessionActivity.Quiet, AgentRuntime.Terminal))
        assertEquals(SessionRowEmphasis.Faded, emphasis(SessionActivity.Idle, AgentRuntime.Terminal))
        assertEquals(SessionRowEmphasis.Faded, emphasis(SessionActivity.Exited))
        assertEquals(SessionRowEmphasis.Plain, emphasis(SessionActivity.Quiet))
        assertEquals(SessionRowEmphasis.Faded, emphasis(SessionActivity.Waiting))
        assertEquals(SessionRowEmphasis.Plain, emphasis(SessionActivity.Asking))
        assertEquals(SessionRowEmphasis.Plain, emphasis(SessionActivity.Failed))
    }
}
