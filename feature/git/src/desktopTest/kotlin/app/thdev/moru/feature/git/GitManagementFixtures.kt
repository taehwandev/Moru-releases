package app.thdev.moru.feature.git

import app.thdev.moru.core.model.git.GitBranchInfo
import app.thdev.moru.core.model.git.GitRepositorySnapshot
import app.thdev.moru.core.model.git.GitWorkingFile

/** Fixture data only: stateless content can be rendered without Git, Swing, or a PTY. */
internal object GitManagementFixtures {
    const val Folder = "/work/project worktree"
    val staged = GitWorkingFile("src/staged.kt", 'M', ' ')
    val unstaged = GitWorkingFile("src/unstaged.kt", ' ', 'M')
    val untracked = GitWorkingFile("new file[1].txt", '?', '?')
    val snapshot = GitRepositorySnapshot(
        rootPath = Folder,
        branchName = "topic",
        head = "0123456789abcdef",
        files = listOf(staged, unstaged, untracked),
        branches = listOf(
            GitBranchInfo("topic", isCurrent = true, upstream = "origin/topic", checkedOutPath = Folder, isMerged = true),
            GitBranchInfo("reviewed", isMerged = true),
            GitBranchInfo("busy", checkedOutPath = "/work/another worktree"),
            GitBranchInfo("origin/topic", isRemote = true),
        ),
        remotes = listOf("origin", "upstream"),
        revision = "reviewed-r1",
        upstream = "origin/topic",
        upstreamRemote = "origin",
        upstreamBranch = "topic",
        ahead = 1,
        behind = 0,
    )
}
