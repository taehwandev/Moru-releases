package app.thdev.moru.feature.git

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import app.thdev.moru.core.model.git.history.GitHistoryResult
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruResizableSplitPane
import app.thdev.moru.feature.git.components.navigation.GitBranchesPopup
import app.thdev.moru.feature.git.i18n.historyStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val RefsColumnWidth = 212.dp
private val ShowRefsFrom = 960.dp
private val ColumnGap = 8.dp
private val HistoryMinWidth = 420.dp
private val DetailMinWidth = 300.dp
private val ChangesMaxWidth = 440.dp
private const val ChangesShare = 0.34f

/** The right column's width for the changes to stage; it gives way before the commit list drops under [HistoryMinWidth]. */
private fun detailWidthFor(available: Dp, refsTaken: Dp): Dp {
    val wanted = (available * ChangesShare).coerceIn(DetailMinWidth, ChangesMaxWidth)
    val room = available - refsTaken - ColumnGap - HistoryMinWidth
    return minOf(wanted, room).coerceAtLeast(DetailMinWidth)
}

/**
 * The Git screen's body, all on one screen: branches on the left, the commit graph with its messages in the middle,
 * and the working changes on the right. Choosing a commit only swaps the right column for that commit's files, so the
 * history stays in view; choosing a file there, or one of the working changes, opens its diff in place of the
 * history, and closing it brings the history back. Navigation, width and window choices belong to this repository
 * view; refreshing its log keeps those choices intact.
 */
@Composable
internal fun GitWorkspaceBody(
    state: GitManagementUiState,
    workspacePath: String,
    history: GitHistorySource,
    onEvent: (GitManagementEvent) -> Unit,
    modifier: Modifier = Modifier,
    detailWidth: Dp? = null,
    onDetailWidthChange: (Dp?) -> Unit = {},
) {
    var selectedHash by remember(workspacePath) { mutableStateOf<String?>(null) }
    var openPath by remember(workspacePath) { mutableStateOf<String?>(null) }
    var showBranches by remember(workspacePath) { mutableStateOf(false) }
    var showDetail by remember(workspacePath) { mutableStateOf(true) }
    var detached by remember(workspacePath) { mutableStateOf(false) }
    val strings = historyStrings()
    val log by produceState<GitHistoryResult?>(null, workspacePath, state.historyRevision) {
        value = null
        value = withContext(Dispatchers.Default) { history.loadHistory(workspacePath) }
    }
    val commitDiff by produceState<CommitDiffResult?>(null, workspacePath, selectedHash) {
        value = null
        val hash = selectedHash ?: return@produceState
        value = withContext(Dispatchers.Default) { history.loadCommitDiff(workspacePath, hash) }
    }
    BoxWithConstraints(modifier) {
        val available = maxWidth
        val showRefs = available >= ShowRefsFrom
        val fittedDetailWidth = detailWidthFor(available, if (showRefs) RefsColumnWidth + ColumnGap else 0.dp)
        val splitDetail = available >= 620.dp
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(ColumnGap)) {
            if (!showRefs) {
                Row(horizontalArrangement = Arrangement.spacedBy(ColumnGap)) {
                    MoruButton("Branches", { showBranches = !showBranches }, type = MoruButtonType.SECONDARY)
                    if (!splitDetail) {
                        MoruButton(if (openPath != null) "Diff" else "History", { showDetail = false },
                            type = if (!showDetail || detached) MoruButtonType.PRIMARY else MoruButtonType.GHOST)
                        val detailLabel = if (selectedHash != null) "Commit" else "Changes"
                        MoruButton(if (detached) "Restore $detailLabel" else detailLabel,
                            { showDetail = true; detached = false },
                            type = if (showDetail && !detached) MoruButtonType.PRIMARY else MoruButtonType.GHOST)
                    }
                }
            }
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ColumnGap)) {
                if (showRefs) GitRefsSidebar(state, onEvent, Modifier.width(RefsColumnWidth).fillMaxHeight())
                MoruResizableSplitPane(
                    rightWidth = when {
                        detached -> 40.dp
                        splitDetail -> detailWidth ?: fittedDetailWidth
                        showDetail -> available
                        else -> 0.dp
                    },
                    onRightWidthChange = onDetailWidthChange,
                    onReset = { onDetailWidthChange(null) },
                    dividerLabel = strings.resizeGitDetail,
                    resetLabel = strings.resetPaneSize,
                    minLeftWidth = if (splitDetail && !detached) HistoryMinWidth else 0.dp,
                    minRightWidth = if (splitDetail && !detached) DetailMinWidth else 0.dp,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    resizable = splitDetail && !detached,
                    left = { paneModifier ->
                        if (splitDetail || !showDetail || detached) {
                            GitCenterPane(log, state.snapshot?.files.orEmpty(), selectedHash,
                                onSelectCommit = { hash -> selectedHash = hash; openPath = null; showDetail = true },
                                openPath = openPath,
                                onCloseFile = { openPath = null },
                                commitDiff = commitDiff,
                                workspacePath = workspacePath,
                                loadWorkingChanges = history.loadWorkingChanges,
                                refreshKey = state.historyRevision,
                                modifier = paneModifier,
                                branch = state.snapshot?.let(::gitBranchLabel),
                                folder = state.snapshot?.rootPath)
                        } else Box(paneModifier)
                    },
                    right = { paneModifier ->
                        if (splitDetail || showDetail || detached) {
                            GitDetachableDetailColumn(state, log, selectedHash, workspacePath, onEvent,
                                commitDiff = commitDiff,
                                openPath = openPath,
                                onOpenFile = { path -> openPath = path; showDetail = false },
                                detached = detached,
                                onDetach = { detached = true },
                                onRestore = { detached = false; showDetail = true },
                                modifier = paneModifier,
                                onCloseCommit = { selectedHash = null; openPath = null })
                        } else Box(paneModifier)
                    },
                )
            }
        }
        if (showBranches && !showRefs) {
            GitBranchesPopup(state, maxHeight.coerceIn(240.dp, 600.dp), onEvent, onDismiss = { showBranches = false })
        }
    }
}
