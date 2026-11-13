package app.thdev.moru.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.LocalMoruNativeMaterial
import app.thdev.moru.core.designsystem.theme.MoruColors
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.designsystem.theme.freshMintCkColors
import app.thdev.moru.core.designsystem.theme.lightCkColors
import app.thdev.moru.core.model.TaskAttention
import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.feature.workbench.ProjectRow
import app.thdev.moru.feature.workbench.ProjectTaskSidebar
import app.thdev.moru.feature.workbench.ProjectTaskSidebarState
import app.thdev.moru.feature.workbench.TaskDisplayState
import app.thdev.moru.feature.workbench.TaskGroup
import app.thdev.moru.feature.workbench.TaskRow
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Reproduces the all-collapsed sidebar inside an Apple/native-material app. */
@OptIn(ExperimentalTestApi::class)
class CollapsedSidebarCaptureTest {
    @Test fun light() = checkLayout("collapsed-light", lightCkColors, 280)
    @Test fun dark() = checkLayout("collapsed-dark", darkCkColors, 280)
    @Test fun mint() = checkLayout("collapsed-mint", freshMintCkColors, 280)
    @Test fun narrow() = checkLayout("collapsed-narrow", darkCkColors, 240)

    private fun checkLayout(name: String, colors: MoruColors, paneWidth: Int) =
        runDesktopComposeUiTest(width = paneWidth, height = 950) {
            var state by mutableStateOf(sidebar())
            val opened = mutableListOf<String>()
            setContent {
                MoruTheme(colors = colors, materialStyle = MoruMaterialStyle.Apple) {
                    CompositionLocalProvider(LocalMoruNativeMaterial provides true) {
                        Box(Modifier.fillMaxSize().background(Color.Magenta)) {
                            ProjectTaskSidebar(
                                state, {}, { id ->
                                    state = state.copy(projects = state.projects.map { it.copy(selected = it.id == id) })
                                }, {}, { _, _ -> }, {}, Modifier.width(paneWidth.dp),
                                sessionSlots = DesktopWorkbenchSessionSlots, sessions = sessions,
                                onSelectSession = { row, project -> opened += "$project:${row.sessionId}" },
                                showHeader = false,
                            )
                        }
                    }
                }
            }
            listOf("KeyFlow", "Moru", "moru-core", "Docs").forEach {
                onNodeWithText(it).assertIsDisplayed()
            }
            sessions.filter { it.activity != SessionActivity.Waiting }.forEach {
                onNodeWithTag("project-agent-line:${it.sessionId}").assertIsDisplayed()
            }
            onNodeWithTag("project-agent-line:idle").assertDoesNotExist()
            val before = onNodeWithTag("project-agent-line:login").fetchSemanticsNode().boundsInRoot.left
            onNodeWithText("KeyFlow").performClick()
            assertEquals(before, onNodeWithTag("project-agent-line:login").fetchSemanticsNode().boundsInRoot.left,
                "Project selection must not shift its children")
            onNodeWithTag("project-agent-line:login").performClick()
            runOnIdle { assertEquals(listOf("keyflow:login"), opened) }
            val titles = listOf("KeyFlow", "Moru", "moru-core", "Docs").map {
                onNodeWithText(it).fetchSemanticsNode().boundsInRoot
            }
            val lastRows = listOf("login", "pairing", "runtime", "upload")
            lastRows.zip(titles.drop(1)).forEach { (session, nextProject) ->
                assertTrue(onNodeWithTag("project-agent-line:$session").fetchSemanticsNode().boundsInRoot.bottom < nextProject.top,
                    "The next Project must start after the preceding Project's preview")
            }
            val image = onRoot().captureToImage()
            assertEquals(colors.background, image.toPixelMap()[1, image.height - 2],
                "Native glass must not expose the magenta backdrop through the sidebar")
            System.getenv("MORU_SIDEBAR_CAPTURE_DIR")?.let { directory ->
                val output = File(directory, "$name.png")
                output.parentFile.mkdirs()
                ImageIO.write(image.toAwtImage(), "png", output)
            }
        }

    private fun sidebar(): ProjectTaskSidebarState {
        val projects = listOf("keyflow" to "KeyFlow", "moru" to "Moru", "core" to "moru-core", "docs" to "Docs")
        val tasks = mapOf("keyflow" to listOf("login" to "Verify Apple sign-in"),
            "moru" to listOf("stability" to "Inspect service stability", "pairing" to "Refine pairing popover"),
            "core" to listOf("runtime" to "Review runtime usage"),
            "docs" to listOf("upload" to "Check usage upload"))
        return ProjectTaskSidebarState(projects.map { (id, name) ->
            ProjectRow(id, name, "/projects/$id", null, true, false, id == "moru", tasks.getValue(id).map { (key, title) ->
                TaskRow(key, id, name, title, TaskDisplayState.Working, TaskGroup.Running, TaskAttention.None, true, false)
            })
        }, "moru", null)
    }

    private val sessions = listOf(
        row("login", "login", SessionActivity.Quiet, "Verified callback URLs and login state"),
        row("limits", "stability", SessionActivity.Working, "Investigating why the service reached its usage limit"),
        row("reading", "stability", SessionActivity.Working, "Reading quota snapshot persistence"),
        row("pairing", "pairing", SessionActivity.Quiet, "Updated the device pairing interface"),
        row("runtime", "runtime", SessionActivity.Working, "Checking runtime observation boundaries"),
        row("upload", "upload", SessionActivity.Quiet, "Verified the private usage upload"),
        row("idle", "login", SessionActivity.Waiting, "Waiting shell"),
    )

    private fun row(id: String, task: String, activity: SessionActivity, title: String) = AgentSessionRow(
        id, title, AgentRuntime.Named("Codex"), activity, null, 0, null, null, false, taskKey = task,
    )
}
