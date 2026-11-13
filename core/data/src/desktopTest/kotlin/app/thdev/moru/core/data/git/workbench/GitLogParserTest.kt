package app.thdev.moru.core.data.git.workbench

import app.thdev.moru.core.model.git.history.GitRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitLogParserTest {
    private fun record(
        hash: String,
        parents: String,
        refs: String = "",
        subject: String = "subject",
        name: String = "Ada",
        email: String = "ada@example.com",
        at: String = "1700000000",
    ) = listOf(hash, parents, name, email, at, refs, subject).joinToString("\u001f") + "\u001e"

    @Test
    fun parsesEveryFieldOfARecord() {
        val commit = parseGitLog(record("a".repeat(40), "b".repeat(40), subject = "Fix the thing")).single()

        assertEquals("a".repeat(40), commit.hash)
        assertEquals(listOf("b".repeat(40)), commit.parents)
        assertEquals("Ada", commit.authorName)
        assertEquals("ada@example.com", commit.authorEmail)
        assertEquals(1_700_000_000L, commit.epochSeconds)
        assertEquals("Fix the thing", commit.subject)
        assertEquals("aaaaaaa", commit.shortHash)
    }

    @Test
    fun rootCommitHasNoParentsAndMergeHasTwo() {
        val commits = parseGitLog(
            record("m1", "p1 p2") + "\n" + record("r1", ""),
        )

        assertEquals(listOf("p1", "p2"), commits[0].parents)
        assertTrue(commits[0].isMerge)
        assertEquals(emptyList(), commits[1].parents)
        assertEquals(false, commits[1].isMerge)
    }

    @Test
    fun classifiesFullyQualifiedRefs() {
        val refs = parseGitLog(
            record(
                "c1",
                "p1",
                refs = "HEAD -> refs/heads/main, refs/remotes/origin/main, tag: refs/tags/v1.0, refs/heads/feat/x",
            ),
        ).single().refs

        assertEquals(
            listOf(
                GitRef("main", GitRef.Kind.LocalBranch, isCurrent = true),
                GitRef("origin/main", GitRef.Kind.RemoteBranch),
                GitRef("v1.0", GitRef.Kind.Tag),
                GitRef("feat/x", GitRef.Kind.LocalBranch),
            ),
            refs,
        )
    }

    @Test
    fun detachedHeadIsItsOwnRefAndRemoteHeadAliasIsHidden() {
        val refs = parseGitLog(record("c1", "p1", refs = "HEAD, refs/remotes/origin/HEAD")).single().refs

        assertEquals(listOf(GitRef("HEAD", GitRef.Kind.Head, isCurrent = true)), refs)
    }

    @Test
    fun subjectKeepsPunctuationAndNonAsciiText() {
        val commit = parseGitLog(record("c1", "p1", subject = "feat(ui): 주의 우선 카드, \"quotes\" & more")).single()

        assertEquals("feat(ui): 주의 우선 카드, \"quotes\" & more", commit.subject)
    }

    @Test
    fun skipsMalformedRecordsInsteadOfFailingTheWholeHistory() {
        val commits = parseGitLog("not a record\u001e" + record("ok", "p1") + "\u001e\u001e")

        assertEquals(listOf("ok"), commits.map { it.hash })
    }

    @Test
    fun emptyOutputYieldsNoCommits() {
        assertEquals(emptyList(), parseGitLog(""))
        assertEquals(emptyList(), parseGitLog("\n"))
    }

    @Test
    fun unparsableTimestampFallsBackToZero() {
        assertEquals(0L, parseGitLog(record("c1", "p1", at = "soon")).single().epochSeconds)
    }
}
