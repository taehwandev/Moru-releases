package app.thdev.moru.core.data.git

import app.thdev.moru.core.domain.git.GitRefParser as SharedGitRefParser
import app.thdev.moru.core.model.git.GitBranchInfo
import app.thdev.moru.core.model.git.GitWorktreeInfo

/** JVM transport decoding stays local; interpretation is shared with the native host. */
internal object GitRefParser {
    fun worktrees(bytes: ByteArray): Map<String, String> = SharedGitRefParser.worktrees(GitStatusParser.decode(bytes))
    fun worktreeList(bytes: ByteArray): List<GitWorktreeInfo> = SharedGitRefParser.worktreeList(GitStatusParser.decode(bytes))
    fun tags(bytes: ByteArray): List<String> = SharedGitRefParser.tags(GitStatusParser.decode(bytes))
    fun branches(bytes: ByteArray, merged: Set<String>, worktrees: Map<String, String>): List<GitBranchInfo> =
        SharedGitRefParser.branches(GitStatusParser.decode(bytes), merged, worktrees)
}
