package app.thdev.moru.core.data.git

import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.core.model.git.GitActionResult
import app.thdev.moru.core.model.git.GitRepositoryState
import java.io.RandomAccessFile
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GitDiscoverySafetyTest {
    @Test fun malformedOrTruncatedStatusDoesNotBecomeAPartialSnapshot() {
        assertFailsWith<IllegalArgumentException> { GitStatusParser.parse(" M file".toByteArray()) }
        assertFailsWith<IllegalArgumentException> { GitStatusParser.parse("R  new\u0000".toByteArray()) }
        assertFailsWith<IllegalArgumentException> { GitStatusParser.parse(" M ../outside\u0000".toByteArray()) }
        assertFailsWith<IllegalArgumentException> { GitStatusParser.parse("bad\u0000".toByteArray()) }
    }

    @Test fun oversizedDirtyContentIsUnavailableRatherThanApprovedWithAnIncompleteHash() = GitTestRepository().use { fixture ->
        RandomAccessFile(fixture.root.resolve("oversized.bin").toFile(), "rw").use { it.setLength(268_435_457L) }
        val unavailable = assertIs<GitRepositoryState.Unavailable>(fixture.adapter.snapshot(fixture.root.toString()))
        assertTrue(unavailable.message.contains("safe snapshot limit"))
    }

    @Test fun qualifiedRemoteRefsResolveAmbiguityWithoutAcceptingArbitraryExpressions(): Unit = GitTestRepository().use { fixture ->
        fixture.commit()
        fixture.git("remote", "add", "origin", fixture.bare().toString())
        fixture.success(GitAction.Push("origin", "main"))
        fixture.git("branch", "origin/main")
        assertIs<GitActionResult.Rejected>(fixture.execute(GitAction.MergeBranch("origin/main")))
        fixture.success(GitAction.MergeBranch("refs/remotes/origin/main"))
        fixture.success(GitAction.CreateBranch("topic", "refs/remotes/origin/main", checkout = false))
        assertIs<GitActionResult.Rejected>(fixture.execute(GitAction.CreateBranch("other", "refs/tags/missing")))
    }

    @Test fun tagAmbiguityDoesNotChangeTheCurrentBranchIdentityAndTagChangesInvalidateApproval() = GitTestRepository().use { fixture ->
        fixture.commit()
        val before = fixture.snapshot().revision
        fixture.git("tag", "main")
        val after = fixture.snapshot()
        assertEquals("main", after.branchName)
        assertNotEquals(before, after.revision)
    }

    @Test fun diagnosticsRemoveUrlUserinfoQueriesAndFragments() {
        // Synthetic credentials exercise redaction without storing a credential-bearing URL literal.
        val fakeUrl = "https://" + "user:pass" + "@host.invalid/repo?token=secret#credential"
        val diagnostic = GitDiagnosticRedactor.redact("failed $fakeUrl and ssh://user@host.invalid/project")
        assertFalse(diagnostic.contains("user:"))
        assertFalse(diagnostic.contains("pass"))
        assertFalse(diagnostic.contains("secret"))
        assertFalse(diagnostic.contains("credential"))
        assertTrue(diagnostic.contains("host.invalid/repo"))
    }

    @Test fun symbolicLinkContentsAreFingerprintedWithoutReadingTheirTargets() = GitTestRepository().use { fixture ->
        fixture.commit()
        val outside = Files.writeString(fixture.base.resolve("outside.txt"), "private")
        val link = Files.createSymbolicLink(fixture.root.resolve("link"), outside)
        val first = fixture.snapshot()
        Files.writeString(outside, "external mutation")
        assertEquals(first.revision, fixture.snapshot().revision)
        Files.delete(link)
        Files.createSymbolicLink(link, fixture.base.resolve("different.txt"))
        assertNotEquals(first.revision, fixture.snapshot().revision)
    }
}
