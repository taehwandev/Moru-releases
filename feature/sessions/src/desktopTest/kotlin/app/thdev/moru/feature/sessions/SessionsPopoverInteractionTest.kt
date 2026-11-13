package app.thdev.moru.feature.sessions

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.model.session.SessionTaskRef
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SessionsPopoverInteractionTest {
    @Test
    fun sameNamedTasksInOneProjectRemainSeparateGroups() = runDesktopComposeUiTest {
        val groupedTasks = mapOf(
            "a" to SessionTaskRef("Shared task", "p1", "Alpha"),
            "c" to SessionTaskRef("Shared task", "p1", "Alpha"),
            "b" to SessionTaskRef("Other task", "p2", "Beta"),
        )
        val picked = mutableListOf<Pair<String, String?>>()
        setContent {
            MoruTheme {
                SessionsPopover(
                    rows = listOf(row("1", "a"), row("2", "c"), row("3", "a"), row("4", "b")),
                    tasks = groupedTasks, selectedProjectId = "p1",
                    onSelectSession = { row, project -> picked += row.sessionId to project },
                    onOpenPreview = { _, _ -> },
                )
            }
        }
        onNodeWithText("Sessions").performClick()
        onNodeWithText("Alpha · 3").assertExists()
        onNodeWithTag("session-task-group:a").assertExists()
        onNodeWithTag("session-task-group:c").assertExists()
        onNodeWithText("Shared task · 2").assertExists()
        onNodeWithText("Shared task · 1").assertExists()
        onNodeWithText("T2").performScrollTo().performClick()
        runOnIdle { assertEquals(listOf<Pair<String, String?>>("2" to "p1"), picked) }
    }

    @Test
    fun escapeClosesThePopoverWithoutSelectingASession() = runDesktopComposeUiTest {
        var selected = false
        setContent {
            MoruTheme {
                SessionsPopover(
                    rows = listOf(row("1", "a")), tasks = tasks, selectedProjectId = "p1",
                    onSelectSession = { _, _ -> selected = true }, onOpenPreview = { _, _ -> },
                )
            }
        }
        onNodeWithText("Sessions").performClick()
        onNodeWithText("Alpha · Fix login").performKeyInput { pressKey(Key.Escape) }
        onNodeWithText("Alpha · Fix login").assertDoesNotExist()
        runOnIdle { assertEquals(false, selected) }
    }

    @Test
    fun longProjectGroupsScrollToOtherProjectsAndKeepTheirSelectionOwner() = runDesktopComposeUiTest {
        val picked = mutableListOf<Pair<String, String?>>()
        setContent {
            MoruTheme {
                SessionsPopover(
                    rows = (1..15).map { row(it.toString(), "a") } + row("16", "b"),
                    tasks = tasks, selectedProjectId = "p1",
                    onSelectSession = { row, project -> picked += row.sessionId to project },
                    onOpenPreview = { _, _ -> },
                )
            }
        }
        onNodeWithText("Sessions").performClick()
        onNodeWithText("Alpha · 15").assertExists()
        onNodeWithText("Beta · Add billing").performScrollTo().performClick()
        runOnIdle { assertEquals(listOf<Pair<String, String?>>("16" to "p2"), picked) }
    }

    private fun row(id: String, task: String?, activity: SessionActivity = SessionActivity.Working) = AgentSessionRow(
        sessionId = id, title = "T$id", agent = AgentRuntime.Named("Claude"), activity = activity,
        lastOutputAtMillis = null, nowMillis = 0, exitCode = null, previewUrl = null, selected = false, taskKey = task,
    )

    private val tasks = mapOf(
        "a" to SessionTaskRef("Fix login", "p1", "Alpha"),
        "b" to SessionTaskRef("Add billing", "p2", "Beta"),
    )

    @Test
    fun theListOpensOnPressAndNamesWhoseEachSessionIsAcrossAllProjects() = runDesktopComposeUiTest {
        setContent {
            MoruTheme {
                SessionsPopover(
                    rows = listOf(row("1", "a"), row("2", "b")), tasks = tasks, selectedProjectId = "p1",
                    onSelectSession = { _, _ -> }, onOpenPreview = { _, _ -> },
                )
            }
        }

        onNodeWithText("Alpha · Fix login").assertDoesNotExist()
        onNodeWithText("Sessions").performClick()

        onNodeWithText("Alpha · Fix login").assertExists()
        onNodeWithText("Beta · Add billing").assertExists()
        onNodeWithText("Alpha · 1").assertExists()
        onNodeWithText("Beta · 1").assertExists()
    }

    @Test
    fun narrowingToThisProjectHidesTheOthers() = runDesktopComposeUiTest {
        setContent {
            MoruTheme {
                SessionsPopover(
                    rows = listOf(row("1", "a"), row("2", "b")), tasks = tasks, selectedProjectId = "p1",
                    onSelectSession = { _, _ -> }, onOpenPreview = { _, _ -> },
                )
            }
        }

        onNodeWithText("Sessions").performClick()
        onNodeWithText("This project").performClick()

        onNodeWithText("Alpha · Fix login").assertExists()
        onNodeWithText("Beta · Add billing").assertDoesNotExist()
    }

    @Test
    fun pressingASessionReportsItWithItsProjectAndClosesTheList() = runDesktopComposeUiTest {
        val picked = mutableListOf<Pair<String, String?>>()
        setContent {
            MoruTheme {
                SessionsPopover(
                    rows = listOf(row("1", "a"), row("2", "b")), tasks = tasks, selectedProjectId = "p1",
                    onSelectSession = { row, project -> picked += row.sessionId to project }, onOpenPreview = { _, _ -> },
                )
            }
        }

        onNodeWithText("Sessions").performClick()
        onNodeWithText("Beta · Add billing").performClick()

        runOnIdle { assertEquals(listOf<Pair<String, String?>>("2" to "p2"), picked) }
        onNodeWithText("Beta · Add billing").assertDoesNotExist()
    }
}
