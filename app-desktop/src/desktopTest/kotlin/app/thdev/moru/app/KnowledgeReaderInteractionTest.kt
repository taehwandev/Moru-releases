package app.thdev.moru.app

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.AgentWorkflowKind
import app.thdev.moru.core.model.AgentWorkflowSettings
import app.thdev.moru.core.model.AgentWorkflowSource
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.files.WorkbenchFilePreviewStatus
import app.thdev.moru.core.model.knowledge.KnowledgeDocumentIndex
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class KnowledgeReaderInteractionTest {
    private val sources = listOf(
        AgentWorkflowSource(AgentWorkflowKind.Codex, "/sources/codex", true),
        AgentWorkflowSource(AgentWorkflowKind.ClaudeCode, "/sources/claude", true),
        AgentWorkflowSource(AgentWorkflowKind.Hermes, "/sources/missing", false),
    )

    @Test
    fun sourceSelectionSearchReadAndRefreshUseSelectedRoot() = runDesktopComposeUiTest(width = 1000, height = 760) {
        val reads = CopyOnWriteArrayList<String>()
        setContent {
            MoruTheme {
                KnowledgeSourcesScreen("/project", AgentWorkflowSettings(), { _, _ -> sources }, {}, { _, _ -> null },
                    { KnowledgeDocumentIndex(listOf("AGENTS.md", "skills/guide.md")) },
                    { root, path -> reads.add("$root/$path"); preview(path, "# ${root.substringAfterLast('/')} instructions\n\nDocument body") },
                    { _, _, target -> preview(target, "Linked content") })
            }
        }
        waitUntil(timeoutMillis = 10_000) { reads.isNotEmpty() }
        waitForIdle()
        onNodeWithText("codex instructions").assertExists()
        assertTrue(onNodeWithText("AGENT OS").getBoundsInRoot().right < onNodeWithText("codex instructions").getBoundsInRoot().left)
        onNodeWithText("Search documents").performTextInput("guide")
        assertEquals(1, onAllNodesWithText("AGENTS.md").fetchSemanticsNodes().size)
        onNodeWithText("guide.md").performClick()
        waitUntil(timeoutMillis = 10_000) { reads.last().endsWith("skills/guide.md") }
        waitForIdle()
        onNodeWithText("Source", useUnmergedTree = true).performClick()
        onNodeWithText("# codex instructions\n\nDocument body").assertExists()
        onNodeWithContentDescription("Choose Agent OS source").performClick()
        onNodeWithText("Claude Code").performClick()
        waitUntil(timeoutMillis = 10_000) { reads.last().startsWith("/sources/claude") }
        waitForIdle()
        onNodeWithText("claude instructions").assertExists()
        val before = reads.size
        onNodeWithContentDescription("Refresh sources and files").performClick()
        waitUntil(timeoutMillis = 10_000) { reads.size > before }
        assertEquals("/sources/claude/AGENTS.md", reads.last())
        onNodeWithContentDescription("Choose Agent OS source").performClick()
        onNodeWithText("Hermes Agent · unavailable").performClick()
        waitForIdle()
        onNodeWithText("This source is unavailable. Locate its folder in Manage sources.").assertExists()
    }

    @Test
    fun slowPreviousDocumentCannotReplaceNewSource() = runDesktopComposeUiTest(width = 1000, height = 760) {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        setContent { MoruTheme {
            KnowledgeSourcesScreen("/project", AgentWorkflowSettings(), { _, _ -> sources }, {}, { _, _ -> null },
                { KnowledgeDocumentIndex(listOf("AGENTS.md")) },
                { root, path ->
                    if (root.endsWith("codex")) { started.countDown(); release.await(5, TimeUnit.SECONDS) }
                    preview(path, if (root.endsWith("codex")) "Old document" else "Current document")
                }, { _, _, target -> preview(target, "Linked content") })
        } }
        try {
            waitUntil(timeoutMillis = 10_000) { started.count == 0L }
            onNodeWithContentDescription("Choose Agent OS source").performClick()
            onNodeWithText("Claude Code").performClick()
            waitUntil(timeoutMillis = 10_000) { onAllNodesWithText("Current document").fetchSemanticsNodes().isNotEmpty() }
            release.countDown()
            waitForIdle()
            onNodeWithText("Current document").assertExists()
            onNodeWithText("Old document").assertDoesNotExist()
        } finally { release.countDown() }
    }

    @Test
    fun unexpectedSourceFailureShowsRecovery() = runDesktopComposeUiTest(width = 1000, height = 760) {
        setContent { MoruTheme {
            KnowledgeSourcesScreen("/project", AgentWorkflowSettings(), { _, _ -> error("unreadable") }, {}, { _, _ -> null },
                { KnowledgeDocumentIndex() }, { _, path -> preview(path, "unused") }, { _, _, target -> preview(target, "unused") })
        } }
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithText("Unable to find local sources. Try Refresh.").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithContentDescription("Refresh sources and files").assertExists()
    }

    @Test
    fun narrowLightAndDarkReaderRenders() {
        val previewSources = listOf(AgentWorkflowSource(AgentWorkflowKind.MoruCore, "/tools/moru-core", true)) + sources
        listOf(false, true).forEach { dark ->
            runDesktopComposeUiTest(width = 760, height = 720) {
                setContent { MoruTheme(darkTheme = dark) {
                    KnowledgeReaderContent(KnowledgeSourcesState(false, previewSources), previewSources.first(), KnowledgeDocumentIndex(listOf("AGENTS.md", "common/skills/review/SKILL.md", "workflows/README.md")),
                        preview("AGENTS.md", "# Agent instructions\n\nRead **local** documents.\n\n| Capability | Status |\n| --- | --- |\n| Reader | Available |\n\n> Keep sources authoritative.\n\n- [x] Shared reader\n\n```kotlin\nval source = \"local\"\n```"),
                        AgentWorkflowSettings(), {}, {}, {}, {}, {}, { _, _ -> null })
                } }
                onNodeWithText("Agent instructions").assertExists()
                val output = File("build/knowledge-reader-${if (dark) "dark" else "light"}.png")
                output.parentFile.mkdirs()
                ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", output)
            }
        }
    }

    private fun preview(path: String, content: String) = WorkbenchFilePreview(
        path = path, name = path.substringAfterLast('/'), status = WorkbenchFilePreviewStatus.TEXT,
        sizeBytes = content.length.toLong(), content = content, summaryText = "Local document",
    )
}
