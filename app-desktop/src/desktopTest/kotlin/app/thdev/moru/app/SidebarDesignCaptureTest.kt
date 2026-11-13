package app.thdev.moru.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruColors
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.designsystem.theme.freshMintCkColors
import app.thdev.moru.core.designsystem.theme.lightCkColors
import app.thdev.moru.core.model.TaskAttention
import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.model.session.SessionTaskRef
import app.thdev.moru.feature.sessions.SessionBoard
import app.thdev.moru.feature.workbench.ProjectRow
import app.thdev.moru.feature.workbench.ProjectTaskSidebar
import app.thdev.moru.feature.workbench.ProjectTaskSidebarState
import app.thdev.moru.feature.workbench.TaskDisplayState
import app.thdev.moru.feature.workbench.TaskGroup
import app.thdev.moru.feature.workbench.TaskRow
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/** Exercises the real app slots and preserves themed component images for visual review. */
@OptIn(ExperimentalTestApi::class)
class SidebarDesignCaptureTest {
    @Test fun light() = checkLayout("sidebar-light", lightCkColors, 280)
    @Test fun dark() = checkLayout("sidebar-dark", darkCkColors, 280)
    @Test fun mint() = checkLayout("sidebar-mint", freshMintCkColors, 280)
    @Test fun narrow() = checkLayout("sidebar-narrow", darkCkColors, 240)

    private fun checkLayout(name: String, colors: MoruColors, paneWidth: Int) =
        runDesktopComposeUiTest(width = paneWidth * 2, height = 950) {
            val tasks = mapOf(
                "design" to SessionTaskRef("Simplify navigation", "alpha", "Design workspace"),
                "website" to SessionTaskRef("Review the website", "beta", "Website"),
                "docs" to SessionTaskRef("Draft release notes", "beta", "Website"),
            )
            setContent {
                MoruTheme(darkTheme = !colors.isLight, colors = colors) {
                    Row(Modifier.background(colors.background)) {
                        ProjectTaskSidebar(
                            sidebar(), {}, {}, {}, { _, _ -> }, {}, Modifier.width(paneWidth.dp),
                            sessionSlots = DesktopWorkbenchSessionSlots, sessions = sessions,
                            showHeader = false,
                        )
                        SessionBoard(sessions, tasks, { _, _ -> }, { _, _ -> }, Modifier.width(paneWidth.dp))
                    }
                }
            }
            onAllNodesWithText("Design workspace")[0].assertExists()
            listOf("Needs attention", "Running", "Response ready", "Waiting").forEach { onNodeWithText(it).assertExists() }
            listOf("NEEDS ANSWER", "WORKING", "STARTING", "RESPONSE READY", "SHELL IDLE").forEach {
                onAllNodesWithText(it)[0].assertExists()
            }
            onNodeWithText("Checking 12 navigation files").assertExists()
            onAllNodesWithText("May I publish the preview and replace the current deployment?").assertCountEquals(2)
            onAllNodesWithText("Which version should I tag?").assertCountEquals(2)
            (System.getProperty("moru.sidebar.captureDir") ?: System.getenv("MORU_SIDEBAR_CAPTURE_DIR"))?.let { directory ->
                val output = File(directory, "$name.png")
                output.parentFile.mkdirs()
                ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", output)
            }
        }

    private fun sidebar() = ProjectTaskSidebarState(
        listOf(
            ProjectRow("alpha", "Design workspace", "/work/design", null, true, false, false,
                listOf(task("design", "alpha", "Design workspace", "Simplify navigation"))),
            ProjectRow("beta", "Website", "/work/website", null, true, true, true,
                listOf(
                    task("website", "beta", "Website", "Review the website"),
                    task("docs", "beta", "Website", "Draft release notes"),
                )),
        ), "beta", "website",
    )

    private fun task(id: String, project: String, name: String, title: String) = TaskRow(
        id, project, name, title, TaskDisplayState.Working, TaskGroup.Running, TaskAttention.None, true, id == "website",
    )

    private val sessions = listOf(
        row("claude", "Claude", SessionActivity.Working, "design", "Refining typography"),
        row("codex", "Codex", SessionActivity.Working, "design", "Checking navigation")
            .copy(lastLine = "Checking 12 navigation files", lastOutputAtMillis = 60_000, nowMillis = 120_000),
        row("agy", "Antigravity", SessionActivity.Working, "design", "Reviewing contrast"),
        row("overflow", "Claude", SessionActivity.Starting, "design", "Preparing checks"),
        row("question", "Codex", SessionActivity.Asking, "website", "May I publish the preview and replace the current deployment?")
            .copy(question = "May I publish the preview and replace the current deployment?"),
        row("done", "Claude", SessionActivity.Quiet, "website", "Updated links"),
        row("reply", "Codex", SessionActivity.Quiet, "docs", "Which version should I tag?")
            .copy(replyQuestion = "Which version should I tag?"),
        row("shell", null, SessionActivity.Idle, "website", "Local shell"),
    )

    private fun row(id: String, agent: String?, activity: SessionActivity, task: String, summary: String) = AgentSessionRow(
        sessionId = id, title = summary, agent = agent?.let(AgentRuntime::Named) ?: AgentRuntime.Terminal,
        activity = activity, lastOutputAtMillis = null, nowMillis = 0, exitCode = null,
        previewUrl = null, selected = id == "question", taskKey = task, summary = summary,
    )
}
