package app.thdev.moru.core.data.git.workbench

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkbenchGitStatusPorcelainParserTest {
    @Test
    fun parsesBranchAndChangeCountsFromPorcelainStatus() {
        val status = WorkbenchGitStatusPorcelainParser.parse(
            """
            ## main...origin/main [ahead 1]
             M docs/TODO.md
            M  desktop-kmp/app-desktop/src/Main.kt
            ?? desktop-kmp/feature/workbench/src/commonMain/NewPane.kt
            R  old-name.kt -> new-name.kt
            """.trimIndent(),
        )

        assertTrue(status.repositoryDetected)
        assertEquals("main", status.branchName)
        assertEquals(4, status.changedCount)
        assertEquals(2, status.stagedCount)
        assertEquals(1, status.unstagedCount)
        assertEquals(1, status.untrackedCount)
        assertEquals("docs/TODO.md", status.changes[0].path)
        assertEquals(" M", status.changes[0].shortStatus)
        assertEquals("old-name.kt", status.changes[3].originalPath)
        assertEquals("new-name.kt", status.changes[3].path)
    }

    @Test
    fun cleanStatusCarriesCleanMessage() {
        val status = WorkbenchGitStatusPorcelainParser.parse("## feature/git-panel")

        assertEquals("feature/git-panel", status.branchName)
        assertEquals(0, status.changedCount)
        assertEquals("Working tree clean.", status.message)
    }
}
