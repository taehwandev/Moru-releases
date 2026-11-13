package app.thdev.moru.core.data.knowledge

import app.thdev.moru.core.model.files.WorkbenchFilePreviewStatus
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopKnowledgeRepositoryTest {
    private val repository = DesktopKnowledgeRepository()

    @Test
    fun listsInstructionsSkillsAndDocsButSkipsCachesAndSymlinks() = withRoot { root ->
        root.resolve("AGENTS.md").writeText("# Instructions")
        root.resolve(".agents/skills/example").mkdirs()
        root.resolve(".agents/skills/example/SKILL.md").writeText("# Skill")
        root.resolve("sessions").mkdir()
        root.resolve("sessions/private.md").writeText("hidden")
        root.resolve("settings.json").writeText("{}"); root.resolve("guide.markdown").writeText("guide")
        val external = Files.createTempDirectory("knowledge-external").toFile()
        try {
            external.resolve("outside.md").writeText("outside")
            Files.createSymbolicLink(root.resolve("linked").toPath(), external.toPath())
            val index = repository.documents(root.path)
            assertEquals(listOf("AGENTS.md", "guide.markdown", ".agents/skills/example/SKILL.md"), index.paths)
            assertFalse(index.truncated)
            assertEquals(WorkbenchFilePreviewStatus.UNAVAILABLE, repository.open(root.path, "linked/outside.md").status)
        } finally { external.deleteRecursively() }
    }

    @Test
    fun relativeAndWikiLinksReadWithinSourceAndRejectEscapes() = withRoot { root ->
        root.resolve("docs").mkdir()
        root.resolve("docs/start.md").writeText("start")
        root.resolve("guide.md").writeText("guide content")
        root.resolve("My Note.md").writeText("space content")
        assertEquals("guide content", repository.openLink(root.path, "docs/start.md", "../guide.md#details").content)
        assertEquals("guide content", repository.openLink(root.path, "docs/start.md", "guide").content)
        assertEquals("space content", repository.openLink(root.path, "docs/start.md", "My Note").content)
        assertEquals(WorkbenchFilePreviewStatus.UNAVAILABLE, repository.open(root.path, "../outside.md").status)
        assertEquals(WorkbenchFilePreviewStatus.UNAVAILABLE, repository.openLink(root.path, "docs/start.md", "file:///etc/hosts").status)
        assertEquals(WorkbenchFilePreviewStatus.MISSING, repository.open(root.path, "missing.md").status)
    }

    @Test
    fun scanAndPreviewLimitsAreVisible() = withRoot { root ->
        repeat(510) { root.resolve("$it.md").writeText("hello") }
        val index = repository.documents(root.path)
        assertTrue(index.truncated)
        assertEquals(500, index.paths.size)
        root.resolve("large.md").writeText("x".repeat(300 * 1024))
        assertEquals(WorkbenchFilePreviewStatus.OVERSIZED, repository.open(root.path, "large.md").status)
        root.resolve("binary.md").writeBytes(byteArrayOf(0, 1, 2))
        assertEquals(WorkbenchFilePreviewStatus.BINARY, repository.open(root.path, "binary.md").status)
    }

    private fun withRoot(test: (java.io.File) -> Unit) {
        val root = Files.createTempDirectory("knowledge-reader").toFile()
        try { test(root) } finally { root.deleteRecursively() }
    }
}
