package app.thdev.moru.feature.sessions

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A navigation render must not keep an old selected-session cache until the revision poll. */
@OptIn(ExperimentalTestApi::class)
class AgentSessionSelectionRefreshTest {
    @Test
    fun taskNavigationAndSelectedSessionAreObservedTogetherWithoutAPoll() = runDesktopComposeUiTest {
        val sessions = InMemoryTerminalSessionManager()
        val first = sessions.createSession(TerminalSessionSpec(attachedItemId = "first"))
        val second = sessions.createSession(TerminalSessionSpec(attachedItemId = "second"))
        sessions.selectSession(first.id)
        val frozenRevision = sessions.revision()
        var task by mutableStateOf("first")
        val rendered = mutableListOf<Pair<String, String?>>()
        setContent {
            val currentTask = task
            val rows = rememberAgentSessionRows(sessions, frozenRevision, scans = false)
            SideEffect { rendered += currentTask to rows.singleOrNull { it.selected }?.taskKey }
        }
        runOnIdle {
            sessions.selectSession(second.id)
            task = "second"
        }
        runOnIdle {
            assertTrue(rendered.any { it.first == "second" })
            assertTrue(rendered.filter { it.first == "second" }.all { it.second == "second" },
                "Every render of the newly selected Task must already highlight its selected session")
        }
    }

    @Test
    fun externalRevisionStillRefreshesTheSelectedSessionWithoutNavigation() = runDesktopComposeUiTest {
        val sessions = InMemoryTerminalSessionManager()
        val first = sessions.createSession(TerminalSessionSpec())
        val second = sessions.createSession(TerminalSessionSpec())
        sessions.selectSession(first.id)
        var revision by mutableStateOf(sessions.revision())
        var selected: String? = null
        setContent {
            val rows = rememberAgentSessionRows(sessions, revision, scans = false)
            SideEffect { selected = rows.singleOrNull { it.selected }?.sessionId }
        }
        runOnIdle {
            sessions.selectSession(second.id)
            revision = sessions.revision()
        }
        runOnIdle { assertEquals(second.id, selected) }
    }
}
