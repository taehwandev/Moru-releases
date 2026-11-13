package app.thdev.moru.app

import app.thdev.moru.core.data.git.workbench.DesktopWorkbenchGitReader
import app.thdev.moru.core.model.git.history.GitHistoryResult
import app.thdev.moru.feature.git.history.layoutGitGraph
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

/** The history the desktop reads from a real repository lays out the way the Git view draws it. */
class GitGraphFromRealHistoryTest {
    @Test
    fun theRealHistoryLaysOutAsTwoLanesWithTheMergeOnTheFirstRow() {
        val dir = Files.createTempDirectory("moru-git-graph").toFile()
        try {
            git(dir, "init", "-q", "-b", "main")
            git(dir, "config", "user.name", "Test Author")
            git(dir, "config", "user.email", "test@example.com")
            git(dir, "config", "commit.gpgsign", "false")
            commitFile(dir, "a.txt", "first commit")
            git(dir, "checkout", "-q", "-b", "feat")
            commitFile(dir, "b.txt", "feature work")
            git(dir, "checkout", "-q", "main")
            commitFile(dir, "c.txt", "main work")
            git(dir, "merge", "-q", "--no-ff", "feat", "-m", "Merge feat into main")

            val history = DesktopWorkbenchGitReader().history(dir.absolutePath)
            val rows = layoutGitGraph((history as GitHistoryResult.Loaded).commits)

            assertEquals(4, rows.size)
            assertEquals(0, rows.first().nodeLane, "the newest commit sits on lane 0")
            assertEquals(2, rows.maxOf { row -> row.laneCount }, "one branch beside main needs exactly two lanes")
            assertEquals(0, rows.last().outgoing.size, "the root commit has no parent to continue to")
            assertEquals(listOf(1), rows.last().incoming.map { link -> link.fromLane }, "the branch lane joins the root commit")
            assertEquals(2, rows.first().outgoing.size, "the merge fans out to both parents")
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun git(dir: File, vararg args: String) {
        val process = ProcessBuilder(listOf("git", "-C", dir.absolutePath) + args)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0) { "git ${args.toList()} failed: $output" }
    }

    private fun commitFile(dir: File, name: String, message: String) {
        File(dir, name).writeText("$name\n")
        git(dir, "add", name)
        git(dir, "commit", "-q", "-m", message)
    }
}
