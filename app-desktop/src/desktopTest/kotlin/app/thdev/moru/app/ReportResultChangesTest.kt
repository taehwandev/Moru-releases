package app.thdev.moru.app

import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.git.CodeWorktree
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import app.thdev.moru.core.model.git.diff.DiffFile
import app.thdev.moru.core.model.git.diff.DiffFileStatus
import app.thdev.moru.feature.codepanel.CodePanelSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReportResultChangesTest {
    @Test
    fun theUncommittedFilesOfEveryCheckoutTheSessionWorksInAreCountedOnce() {
        val source = FakeSource(
            roots = listOf("/w/app", "/w/lib", "/w/gone"),
            changes = mapOf(
                "/w/app" to CommitDiffResult.Loaded(listOf(file("a.kt"), file("b.kt")), truncated = false),
                "/w/lib" to CommitDiffResult.Loaded(listOf(file("a.kt")), truncated = false),
                "/w/gone" to CommitDiffResult.Unavailable("not a repository"),
            ),
        )
        assertEquals(3, uncommittedFiles(source, "s"))
        assertNull(uncommittedFiles(FakeSource(emptyList(), emptyMap()), "s"))
    }

    private fun file(path: String) = DiffFile(path, path, DiffFileStatus.Modified, emptyList(), isBinary = false)

    private class FakeSource(val roots: List<String>, val changes: Map<String, CommitDiffResult>) : CodePanelSource {
        override fun worktrees(projectFolder: String): List<CodeWorktree> = emptyList()
        override fun sessionWorktreeRoots(sessionId: String) = roots
        override fun changeStamp(worktreeRoot: String): String? = null
        override fun changes(worktreeRoot: String) = changes.getValue(worktreeRoot)
        override fun readFile(worktreeRoot: String, relativePath: String): WorkbenchFilePreview = error("unused")
    }
}
