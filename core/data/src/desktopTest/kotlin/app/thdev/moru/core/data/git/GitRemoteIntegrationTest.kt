package app.thdev.moru.core.data.git

import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.core.model.git.GitActionResult
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GitRemoteIntegrationTest {
    @Test fun explicitPushPublishesOnlyOneBranchAndEstablishesItsUpstream() = GitTestRepository().use { fixture ->
        fixture.commit()
        val remote = fixture.bare()
        fixture.git("remote", "add", "origin", remote.toString())
        fixture.git("branch", "spare")
        fixture.git("config", "push.default", "matching")
        fixture.git("config", "remote.origin.push", "refs/heads/spare:refs/heads/spare")
        fixture.git("config", "push.followTags", "true")
        fixture.git("tag", "--annotate", "unrequested-tag", "--message", "Do not publish")
        fixture.success(GitAction.Push("origin", "published"))
        assertEquals("refs/heads/published", fixture.gitAt(remote, "for-each-ref", "--format=%(refname)"))
        val snapshot = fixture.snapshot()
        assertEquals("origin", snapshot.upstreamRemote)
        assertEquals("published", snapshot.upstreamBranch)
        assertEquals("origin/published", snapshot.upstream)
        assertEquals(0, snapshot.ahead)
        assertEquals(0, snapshot.behind)
    }

    @Test fun fetchUsesOnlySelectedRemoteThenPullFastForwardsItsExplicitBranch() = GitTestRepository().use { fixture ->
        fixture.commit()
        val remote = fixture.bare()
        fixture.git("remote", "add", "origin", remote.toString())
        fixture.git("remote", "add", "secondary", remote.toString())
        fixture.success(GitAction.Push("origin", "main"))
        val peer = fixture.clone(remote)
        Files.writeString(peer.resolve("peer.txt"), "remote change")
        fixture.gitAt(peer, "add", "--", "peer.txt")
        fixture.gitAt(peer, "commit", "--message", "Peer commit")
        fixture.gitAt(peer, "push", "origin", "refs/heads/main:refs/heads/main")
        fixture.success(GitAction.Fetch("origin"))
        assertTrue(fixture.snapshot().branches.none { it.isRemote && it.name.startsWith("secondary/") })
        assertEquals(1, fixture.snapshot().behind)
        fixture.success(GitAction.Pull("origin", "main"))
        assertEquals("remote change", Files.readString(fixture.root.resolve("peer.txt")))
        assertEquals(0, fixture.snapshot().behind)
    }

    @Test fun divergentPullAndPushFailWithoutOverwritingEitherHead() = GitTestRepository().use { fixture ->
        fixture.commit()
        val remote = fixture.bare()
        fixture.git("remote", "add", "origin", remote.toString())
        fixture.success(GitAction.Push("origin", "main"))
        val peer = fixture.clone(remote)
        Files.writeString(peer.resolve("remote.txt"), "peer")
        fixture.gitAt(peer, "add", "--", "remote.txt")
        fixture.gitAt(peer, "commit", "--message", "Remote diverges")
        fixture.gitAt(peer, "push", "origin", "refs/heads/main:refs/heads/main")
        val remoteHead = fixture.gitAt(remote, "rev-parse", "refs/heads/main")
        fixture.commit("local", "local.txt")
        val localHead = fixture.snapshot().head
        assertIs<GitActionResult.Failed>(fixture.execute(GitAction.Pull("origin", "main")))
        assertEquals(localHead, fixture.snapshot().head)
        assertEquals(null, fixture.snapshot().operationInProgress)
        assertIs<GitActionResult.Failed>(fixture.execute(GitAction.Push("origin", "main")))
        assertEquals(remoteHead, fixture.gitAt(remote, "rev-parse", "refs/heads/main"))
    }

    @Test fun dirtyPullRejectsBeforeFetchingRemoteChanges() = GitTestRepository().use { fixture ->
        fixture.commit()
        val remote = fixture.bare()
        fixture.git("remote", "add", "origin", remote.toString())
        fixture.success(GitAction.Push("origin", "main"))
        val peer = fixture.clone(remote)
        Files.writeString(peer.resolve("remote.txt"), "remote")
        fixture.gitAt(peer, "add", "remote.txt")
        fixture.gitAt(peer, "commit", "--message", "Remote update")
        fixture.gitAt(peer, "push", "origin", "refs/heads/main:refs/heads/main")
        val tracking = fixture.git("rev-parse", "refs/remotes/origin/main")
        fixture.write("untracked.txt", "retain")
        assertIs<GitActionResult.Rejected>(fixture.execute(GitAction.Pull("origin", "main")))
        assertEquals(tracking, fixture.git("rev-parse", "refs/remotes/origin/main"))
    }
}
