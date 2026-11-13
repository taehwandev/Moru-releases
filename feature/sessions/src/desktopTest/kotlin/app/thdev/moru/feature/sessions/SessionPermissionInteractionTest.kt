package app.thdev.moru.feature.sessions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.terminal.SessionPermissionChoice
import app.thdev.moru.core.terminal.SessionPermissionPrompt
import app.thdev.moru.core.terminal.SessionPermissions
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.SessionActivity
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SessionPermissionInteractionTest {
    @Test fun theSidebarKeepsHookProseBehindDetails() = runDesktopComposeUiTest {
        val session = "permission-brief-test"
        val raw = "Bash command\nRun shell command\n────\ngit restore src/Main.kt\n────\nHook PreToolUse:Bash requires confirmation\nThe main-checkout exception needs explicit approval"
        SessionPermissions.observe(session, SessionPermissionPrompt("Do you want to proceed?", raw,
            listOf(SessionPermissionChoice("1", "Yes"), SessionPermissionChoice("2", "No")))) { true }
        try {
            setContent { MoruTheme { SessionPermissionActions(session, modifier = Modifier.width(240.dp)) } }
            onNodeWithText(raw).assertDoesNotExist()
            onNodeWithText("Run git restore\nMain.kt\nMain checkout exception requires confirmation.").assertIsDisplayed()
            onNodeWithText("Details").performClick()
            onNodeWithText(raw).assertIsDisplayed()
            onNodeWithText("Hide details").performClick()
            onNodeWithText(raw).assertDoesNotExist()
        } finally { SessionPermissions.forget(session) }
    }
    @Test fun aSidebarChoiceIsKeyboardReachableAndDoesNotOpenTheSession() = runDesktopComposeUiTest {
        val session = "permission-sidebar-test"
        var opened = 0
        var chosen: String? = null
        SessionPermissions.observe(session, SessionPermissionPrompt("Allow command?", "pwd", listOf(
            SessionPermissionChoice("allow", "Allow once"), SessionPermissionChoice("deny", "Deny")))) { chosen = it; true }
        try {
            setContent { MoruTheme { SessionRowView(
                AgentSessionRow(session, "Task terminal", AgentRuntime.Named("Claude"), SessionActivity.Asking,
                    null, 0, null, null, false, question = "Allow command?"),
                onClick = { opened++ }, modifier = Modifier.width(240.dp),
            ) } }
            onNodeWithTag("permission-choice-$session-deny")
                .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
                .assertIsFocused().performKeyInput { pressKey(Key.Enter) }
            runOnIdle { assertEquals("deny", chosen); assertEquals(0, opened) }
            onNodeWithTag("permission-request-$session").assertDoesNotExist()
        } finally { SessionPermissions.forget(session) }
    }

    @Test fun respondingFromOneSurfaceRemovesTheOtherAndFailureStaysVisible() = runDesktopComposeUiTest {
        val session = "permission-ui-test"
        var sends = 0
        var available = false
        SessionPermissions.observe(session, SessionPermissionPrompt("Allow command?", "pwd", listOf(
            SessionPermissionChoice("yes", "Allow once"), SessionPermissionChoice("no", "Deny")))) { sends++; available }
        try {
            setContent { MoruTheme { Column(Modifier.width(240.dp)) {
                SessionPermissionActions(session, modifier = Modifier.testTag("sidebar"))
                SessionPermissionActions(session, expectedRequestId = "historical-request", modifier = Modifier.testTag("history"))
            } } }
            onNodeWithTag("history").assertDoesNotExist()
            onNodeWithText("Allow once").performClick()
            onNodeWithText("Could not send this response. Check the current request in the terminal and try again.").assertIsDisplayed()
            available = true
            onNodeWithText("Deny").performClick()
            onNodeWithTag("permission-request-$session").assertDoesNotExist()
            assertEquals(2, sends)
        } finally { SessionPermissions.forget(session) }
    }
}
