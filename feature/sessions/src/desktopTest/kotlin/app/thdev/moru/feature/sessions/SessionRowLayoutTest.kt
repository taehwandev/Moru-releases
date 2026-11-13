package app.thdev.moru.feature.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.designsystem.theme.lightCkColors
import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.model.session.SessionTaskRef
import app.thdev.moru.core.ui.identity.taskIdentityAccent
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SessionRowLayoutTest {
    @Test
    fun finishedResponsesAndTheirOwnershipStayReadableAtNarrowWidthInEveryTheme() {
        val custom = lightCkColors.copy(primary = Color(0xFF006B82), textPrimary = Color(0xFF472D69))
        listOf(lightCkColors, darkCkColors, custom).forEachIndexed { themeIndex, colors ->
            runDesktopComposeUiTest(width = 240, height = 700) {
                val base = AgentSessionRow("working", "Review changes", AgentRuntime.Named("Codex"),
                    SessionActivity.Working, null, 0, null, null, false, taskKey = "task")
                setContent {
                    MoruTheme(colors = colors) {
                        Box(Modifier.background(colors.surface)) {
                            SessionBoard(
                                listOf(base, base.copy(sessionId = "completed", activity = SessionActivity.Quiet),
                                    base.copy(sessionId = "selected", activity = SessionActivity.Quiet, selected = true),
                                    base.copy(sessionId = "waiting", activity = SessionActivity.Waiting)),
                                mapOf("task" to SessionTaskRef("Review sidebar hierarchy", "project", "Moru")),
                                { _, _ -> }, { _, _ -> },
                            )
                        }
                    }
                }
                fun headlineContrast(id: String): Float {
                    val pixels = onNodeWithTag("session-headline:$id", useUnmergedTree = true)
                        .captureToImage().toPixelMap()
                    return (0 until pixels.height).maxOf { y ->
                        (0 until pixels.width).maxOf { x ->
                            val pixel = pixels[x, y]
                            maxOf(abs(pixel.red - colors.surface.red), abs(pixel.green - colors.surface.green),
                                abs(pixel.blue - colors.surface.blue))
                        }
                    }
                }
                val active = headlineContrast("working")
                val completed = headlineContrast("completed")
                val waiting = headlineContrast("waiting")
                assertTrue(completed >= active * .95f, "Finished responses must retain readable text contrast")
                assertTrue(waiting < active * .8f, "Waiting work must stand back from running work")
                assertTrue(headlineContrast("selected") >= completed, "Selection must retain readable contrast")
                val running = hasAnyAncestor(hasTestTag("session-board-section:Running"))
                val project = onNode(hasTestTag("session-board-project-heading:project") and running)
                    .fetchSemanticsNode().boundsInRoot
                val task = onNode(hasTestTag("session-board-task-heading:task") and running)
                    .fetchSemanticsNode().boundsInRoot
                val session = onNodeWithTag("session-row:working").fetchSemanticsNode().boundsInRoot
                assertTrue(project.left < task.left && task.left < session.left,
                    "Project, Task, and session must have distinct ownership indentation")
                assertTrue(task.right <= 240f && session.right <= 240f, "Ownership must fit the narrow sidebar")
                onNodeWithTag("session-row:selected").assertIsSelected()
                val selectedPixels = onNodeWithTag("session-row:selected").captureToImage().toPixelMap()
                val rail = selectedPixels[2, selectedPixels.height / 2]
                val taskAccent = taskIdentityAccent("task").color(colors)
                assertTrue(abs(rail.red - taskAccent.red) < .03f &&
                    abs(rail.green - taskAccent.green) < .03f && abs(rail.blue - taskAccent.blue) < .03f,
                    "A selected response must retain its owning Task's theme-colored selection rail")
                System.getenv("MORU_SESSION_SHOT_DIR")?.let { folder ->
                    val file = File(folder, "status-emphasis-$themeIndex.png")
                    file.parentFile.mkdirs()
                    ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", file)
                }
            }
        }
    }

    @Test
    fun providerAndStateLeaveAFullWorkLineAtNarrowWidthWhileDetailsAndActionsRemain() =
        runDesktopComposeUiTest(width = 240, height = 220) {
            val opened = mutableListOf<Pair<String, String>>()
            var selected = 0
            val row = AgentSessionRow("s", "Fallback", AgentRuntime.Named("Codex"), SessionActivity.Asking,
                null, 0, null, "http://localhost:5173", true,
                summary = "Investigating a long sidebar description in a narrow project", question = "Allow this command?")
            setContent {
                MoruTheme {
                    SessionRowView(row, onClick = { selected++ }, context = "Alpha · Fix sidebar",
                        onOpenPreview = { session, url -> opened += session to url })
                }
            }
            val agent = onNodeWithTag("session-agent:s", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val headline = onNodeWithTag("session-headline:s", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val status = onNodeWithTag("session-status:s", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertTrue(agent.right <= status.left)
            assertTrue(abs(agent.center.y - status.center.y) < 1f, "Provider and state must share their own line")
            assertTrue(headline.top >= status.bottom, "Work description must have a separate full-width line")
            assertTrue(status.right <= 240f, "Long work descriptions cannot push the state out of view")
            onNodeWithText("NEEDS ANSWER").assertExists()
            onNodeWithTag("session-row:s").assertIsSelected()
            onNodeWithText("Allow this command?").assertExists()
            onNodeWithText("Alpha · Fix sidebar").performClick()
            onNodeWithText("Preview localhost:5173").performClick()
            runOnIdle {
                assertEquals(1, selected)
                assertEquals(listOf("s" to "http://localhost:5173"), opened)
            }
        }
}
