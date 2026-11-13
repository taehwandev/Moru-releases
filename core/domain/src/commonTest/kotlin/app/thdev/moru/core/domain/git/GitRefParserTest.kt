package app.thdev.moru.core.domain.git

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitRefParserTest {
    private val head = "a".repeat(40)
    private val tree = "worktree /repo\u0000HEAD $head\u0000branch refs/heads/main\u0000locked reason\u0000\u0000"
    private val refs = "refs/heads/main\u0000$head\u0000refs/remotes/origin/main\u0000*\u0000\u0000\n"

    @Test fun preservesHeadOccupancyLockAndUnknownMergeState() {
        val checkout = GitRefParser.worktreeList(tree).single()
        assertEquals(head, checkout.head); assertTrue(checkout.locked); assertTrue(checkout.isMain)
        val ref = GitRefParser.namedRefs(refs, GitRefParser.worktrees(tree)).single()
        assertEquals("/repo", ref.checkedOutPath)
        assertEquals("refs/remotes/origin/main", ref.upstream)
        assertNull(ref.isMerged)
    }

    @Test fun detachedPrunableAndUnbornRemainRealMetadata() {
        val text = tree + "worktree /other\u0000HEAD $head\u0000detached\u0000prunable gone\u0000\u0000"
        val other = GitRefParser.worktreeList(text).last()
        assertNull(other.branch); assertTrue(other.prunable); assertFalse(other.isMain)
        assertNull(GitRefParser.worktreeList(tree.replace(head, "0".repeat(40))).single().head)
    }

    @Test fun incompleteWorktreeAndRefRecordsFail() {
        assertFailsWith<IllegalArgumentException> { GitRefParser.worktreeList(tree.dropLast(1)) }
        assertFailsWith<IllegalArgumentException> { GitRefParser.worktreeList("worktree /repo\u0000\u0000") }
        assertFailsWith<IllegalArgumentException> { GitRefParser.namedRefs(refs.dropLast(1), emptyMap()) }
        assertFailsWith<IllegalArgumentException> { GitRefParser.namedRefs(refs.replace(head, "short"), emptyMap()) }
        assertFailsWith<IllegalArgumentException> { GitRefParser.namedRefs(refs + refs, emptyMap()) }
    }

    @Test fun literalInventoryNamesAllowPunctuationButRejectOptionsAndRevisionSyntax() {
        assertTrue(GitRefParser.validPublicRef("refs/heads/release,one"))
        assertTrue(GitRefParser.validPublicRef("refs/tags/--flag"))
        listOf("--all", "HEAD", "refs/heads/a..b", "refs/heads/a^{tree}", "refs/heads/.hidden",
            "refs/heads/a.lock", "refs/heads/a\n", "refs/private/snapshot").forEach {
            assertFalse(GitRefParser.validPublicRef(it), it)
        }
    }
}
