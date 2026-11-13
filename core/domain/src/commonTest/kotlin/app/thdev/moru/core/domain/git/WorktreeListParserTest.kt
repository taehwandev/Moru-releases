package app.thdev.moru.core.domain.git

import app.thdev.moru.core.model.git.CodeWorktree
import kotlin.test.Test
import kotlin.test.assertEquals

class WorktreeListParserTest {
    @Test fun quotedPorcelainPathsUseTheSameGitDecoder() {
        val output = """
            worktree "/repo/a\tline\n\"\\\303\251"
            HEAD abc123
            branch refs/heads/main

            worktree /repo/detached
            HEAD def456
            detached

        """.trimIndent()
        val path = "/repo/a\tline\n\"\\é"
        assertEquals(listOf(CodeWorktree(path, "main", true), CodeWorktree("/repo/detached", null, false)), WorktreeListParser.parseWorktreeList(output))
        assertEquals(listOf(path to "abc123", "/repo/detached" to "def456"), WorktreeListParser.parseWorktreeHeads(output))
    }

    @Test fun nulPorcelainPreservesRawNewlinesQuotesAndUnicode() {
        val path = "/repo/한글\n\t\"\\😀"
        val output = "worktree $path\u0000HEAD abc\u0000branch refs/heads/main\u0000\u0000" +
            "worktree /repo/other\u0000HEAD def\u0000detached\u0000\u0000"
        assertEquals(listOf(CodeWorktree(path, "main", true), CodeWorktree("/repo/other", null, false)), WorktreeListParser.parseWorktreeList(output))
        assertEquals(listOf(path to "abc", "/repo/other" to "def"), WorktreeListParser.parseWorktreeHeads(output))
    }

    @Test fun bareAndUnbornRecordsDoNotInventHeadsOrMainCheckout() {
        val output = "worktree /repo.git\nbare\n\nworktree /repo/linked\nHEAD 00000000\nbranch refs/heads/new\n"
        assertEquals(listOf(CodeWorktree("/repo/linked", "new", false)), WorktreeListParser.parseWorktreeList(output))
        assertEquals(emptyList(), WorktreeListParser.parseWorktreeHeads(output))
        assertEquals(emptyList(), WorktreeListParser.parseWorktreeList(""))
    }
}
