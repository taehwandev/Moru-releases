package app.thdev.moru.feature.git

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButtonSize
import app.thdev.moru.core.designsystem.component.MoruCompactTextField
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.GitRepositorySnapshot
import app.thdev.moru.core.model.git.GitWorktreeInfo
import app.thdev.moru.feature.git.components.branches.GitRefSectionHeader
import app.thdev.moru.feature.git.components.branches.GitTagRow
import app.thdev.moru.feature.git.components.branches.GitWorktreeRow

/**
 * The repository's refs in foldable sections, as a Git client's sidebar lists them: local branches, remote branches,
 * worktrees and tags, filtered by one search field. A right click on any ref opens what can be done to it, from
 * switching to deleting; nothing is changed without the usual approval. Only creating or renaming a branch asks for a
 * name, in a form that opens below the refs.
 */
@Composable
internal fun GitRefsSidebar(state: GitManagementUiState, onEvent: (GitManagementEvent) -> Unit, modifier: Modifier = Modifier) {
    val snapshot = state.snapshot
    var query by remember(snapshot?.rootPath) { mutableStateOf("") }
    var folded by remember(snapshot?.rootPath) { mutableStateOf(emptySet<String>()) }
    var editor by remember(snapshot?.rootPath) { mutableStateOf<GitBranchEditor>(GitBranchEditor.Closed) }
    val clipboard = LocalClipboardManager.current
    val menus = GitRefMenus(
        state, onEvent,
        onCreateFrom = { source ->
            onEvent(GitManagementEvent.EditNewBranchName(if (source.isRemote) source.name.substringAfter('/') else ""))
            editor = GitBranchEditor.Create(source)
        },
        onRename = { branch ->
            onEvent(GitManagementEvent.EditRenameBranchName(branch.name))
            editor = GitBranchEditor.Rename(branch)
        },
        copy = { clipboard.setText(AnnotatedString(it)) },
    )
    val needle = query.trim()
    fun matches(vararg values: String?) = needle.isEmpty() || values.any { it?.contains(needle, ignoreCase = true) == true }
    val branches = snapshot?.branches.orEmpty().filterNot { it.isRemote && it.name.endsWith("/HEAD") }
    val local = branches.filter { !it.isRemote && matches(it.name, it.checkedOutPath) }
    val remote = branches.filter { it.isRemote && matches(it.name) }
    // Every checkout, unfiltered, so a branch's house or trees mark holds while the search hides its worktree row.
    val allWorktrees = snapshot?.let(::worktreesOf).orEmpty()
    val worktrees = allWorktrees.filter { matches(it.branch, it.path) }
    val tags = snapshot?.tags.orEmpty().filter { matches(it) }
    fun toggle(title: String) { folded = if (title in folded) folded - title else folded + title }

    Column(modifier.ckSurface(MoruSurfaceRole.Panel)) {
        MoruCompactTextField(query, { query = it }, Modifier.fillMaxWidth().padding(6.dp).testTag("git-branch-search"),
            placeholder = "Filter refs")
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().testTag("git-branches"),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            contentPadding = PaddingValues(start = 4.dp, end = 4.dp, bottom = 8.dp),
        ) {
            section("Local", MoruIconName.Branch, local.size, folded, ::toggle, trailing = {
                MoruIconButton(MoruIconName.Plus, "New branch", {
                    onEvent(GitManagementEvent.EditNewBranchName(""))
                    editor = GitBranchEditor.Create(source = null)
                }, Modifier.testTag("git-new-branch"), enabled = state.interactive, size = MoruButtonSize.Small)
            }) {
                items(local, key = { "local:${it.name}" }) { branch ->
                    GitRefRow(branch, state.selectedBranch == branch.selectionKey, state.interactive, onEvent,
                        { menus.branch(branch) }, allWorktrees)
                }
            }
            section("Remote", MoruIconName.Cloud, remote.size, folded, ::toggle) {
                items(remote, key = { "remote:${it.name}" }) { branch ->
                    GitRefRow(branch, state.selectedBranch == branch.selectionKey, state.interactive, onEvent,
                        { menus.branch(branch) }, allWorktrees)
                }
            }
            section("Worktrees", MoruIconName.Worktree, worktrees.size, folded, ::toggle) {
                items(worktrees, key = { "worktree:${it.path}" }) { worktree ->
                    val branch = branches.firstOrNull { !it.isRemote && it.name == worktree.branch }
                    GitWorktreeRow(worktree, viewed = worktree.path == snapshot?.rootPath,
                        onSelect = { branch?.let { onEvent(GitManagementEvent.SelectBranch(it.selectionKey)) } },
                        enabled = state.interactive) { menus.worktree(worktree) }
                }
            }
            section("Tags", MoruIconName.Tag, tags.size, folded, ::toggle) {
                items(tags, key = { "tag:$it" }) { tag -> GitTagRow(tag) { menus.tag(tag) } }
            }
            if (local.isEmpty() && remote.isEmpty() && worktrees.isEmpty() && tags.isEmpty()) item(key = "empty") {
                MoruText(if (needle.isEmpty()) "No branches yet" else "No matching refs",
                    Modifier.padding(MoruTheme.spacing.medium), color = MoruTheme.colors.textSecondary)
            }
        }
        GitBranchActions(state, editor, onClose = { editor = GitBranchEditor.Closed }, onEvent,
            Modifier.fillMaxWidth().padding(horizontal = MoruTheme.spacing.small, vertical = MoruTheme.spacing.xSmall))
    }
}

/** Git's own worktree list, or, from an older read without one, the checkouts the branches name. */
private fun worktreesOf(snapshot: GitRepositorySnapshot): List<GitWorktreeInfo> = snapshot.worktrees.ifEmpty {
    snapshot.branches.mapNotNull { branch ->
        branch.checkedOutPath?.let { GitWorktreeInfo(it, branch.name, head = null, isMain = false) }
    }
}

private fun LazyListScope.section(
    title: String,
    icon: MoruIconName,
    count: Int,
    folded: Set<String>,
    onToggle: (String) -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    rows: LazyListScope.() -> Unit,
) {
    if (count == 0 && trailing == null) return
    val expanded = title !in folded
    item(key = "header:$title") {
        GitRefSectionHeader(title, formatGitCount(count), icon, expanded, onToggle = { onToggle(title) }, trailing = trailing)
    }
    if (expanded) rows()
}
