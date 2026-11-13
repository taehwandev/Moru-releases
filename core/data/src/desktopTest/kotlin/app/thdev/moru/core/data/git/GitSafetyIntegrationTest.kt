package app.thdev.moru.core.data.git

import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.core.model.git.GitActionResult
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GitSafetyIntegrationTest {
    @Test fun inputCannotBecomeOptionsShellCodeOrArbitraryRevisionExpressions() = GitTestRepository().use { fixture ->
        fixture.commit()
        listOf(GitAction.CreateBranch("--help"), GitAction.CreateBranch("bad name"), GitAction.CreateBranch("topic", "HEAD~1"),
            GitAction.Fetch("--all"), GitAction.Stage(listOf("../outside")), GitAction.MergeBranch("HEAD~1"))
            .forEach { assertIs<GitActionResult.Rejected>(fixture.execute(it)) }
        fixture.success(GitAction.CreateBranch("topic;literal", checkout = false))
        assertTrue(fixture.snapshot().branches.any { it.name == "topic;literal" })
        assertEquals("main", fixture.snapshot().branchName)
    }

    @Test fun operationMarkersInvalidateProposalsAndBlockUnsupportedActions() = GitTestRepository().use { fixture ->
        fixture.commit()
        fixture.git("branch", "topic")
        fixture.git("remote", "add", "origin", fixture.bare().toString())
        val approved = fixture.snapshot()
        Files.writeString(fixture.root.resolve(".git/MERGE_HEAD"), "${approved.head}\n")
        assertIs<GitActionResult.Rejected>(fixture.adapter.execute(fixture.root.toString(), approved.revision, GitAction.Fetch("origin")))
        assertEquals("MERGE_HEAD", fixture.snapshot().operationInProgress)
        listOf(GitAction.SwitchBranch("topic"), GitAction.Fetch("origin"), GitAction.Push("origin", "main"), GitAction.Commit("Blocked"))
            .forEach { assertIs<GitActionResult.Rejected>(fixture.execute(it)) }
    }

    @Test fun detachedHeadIsExplicitAndCannotBePublished(): Unit = GitTestRepository().use { fixture ->
        fixture.commit()
        fixture.git("remote", "add", "origin", fixture.bare().toString())
        fixture.git("switch", "--detach", "HEAD")
        assertEquals(null, fixture.snapshot().branchName)
        assertTrue(fixture.snapshot().head != null)
        assertIs<GitActionResult.Rejected>(fixture.execute(GitAction.Push("origin", "main")))
    }

    @Test fun hookFailureReportsSanitizedDiagnosticsAndRetainsTheIndex() = GitTestRepository().use { fixture ->
        fixture.commit()
        fixture.write("tracked.txt", "ready")
        fixture.success(GitAction.Stage(listOf("tracked.txt")))
        // Synthetic credentials are assembled only for the sanitizer regression.
        val fakeUrl = "https://" + "user:password" + "@example.invalid/repo?token=secret"
        hook(fixture, "echo '$fakeUrl' >&2\nexit 1")
        val result = assertIs<GitActionResult.Failed>(fixture.execute(GitAction.Commit("Hook rejects")))
        assertFalse(result.message.contains("password"))
        assertFalse(result.message.contains("token=secret"))
        assertTrue(result.message.contains("example.invalid"))
        assertTrue(fixture.snapshot().files.single().hasStaged)
    }

    @Test fun timedOutMutationReportsUncertainOutcomeWithoutRetrying() = GitTestRepository().use { fixture ->
        fixture.commit()
        fixture.write("tracked.txt", "ready")
        fixture.success(GitAction.Stage(listOf("tracked.txt")))
        val approved = fixture.snapshot()
        hook(fixture, "sleep 5")
        val adapter = DesktopGitRepository(GitProcessRunner(timeoutMillis = 500))
        val result = assertIs<GitActionResult.Failed>(adapter.execute(fixture.root.toString(), approved.revision, GitAction.Commit("Deadline")))
        assertTrue(result.message.contains("uncertain"))
        assertEquals(approved.head, fixture.snapshot().head)
    }

    @Test fun outputClippingIsUncertainEvenWhenTheMutationCompletes() = GitTestRepository().use { fixture ->
        fixture.commit()
        fixture.write("tracked.txt", "ready")
        fixture.success(GitAction.Stage(listOf("tracked.txt")))
        val approved = fixture.snapshot()
        hook(fixture, "printf '%s' '${"x".repeat(4_096)}'")
        val context = GitRepositoryDiscovery.read(fixture.root.toString(), GitProcessRunner())
        val result = assertIs<GitActionResult.Failed>(GitActionExecutor(GitProcessRunner(outputLimit = 1_024))
            .execute(context, approved, GitAction.Commit("Clipped output")))
        assertTrue(result.message.contains("uncertain"))
        assertFalse(fixture.snapshot().head == approved.head)
    }

    private fun hook(fixture: GitTestRepository, body: String) {
        val file = Files.writeString(fixture.hooks.resolve("pre-commit"), "#!/bin/sh\n$body\n")
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwx------"))
    }
}
