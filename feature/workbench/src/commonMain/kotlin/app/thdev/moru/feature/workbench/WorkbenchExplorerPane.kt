package app.thdev.moru.feature.workbench

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.MoruTextField
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.domain.files.ProjectExplorerPolicy
import app.thdev.moru.core.model.CodeExplorerOptions
import app.thdev.moru.core.model.files.WorkbenchFileCreateOutcome
import app.thdev.moru.core.model.files.WorkbenchProjectTreeEntry
import app.thdev.moru.core.model.git.WorkbenchGitStatus
import app.thdev.moru.feature.workbench.i18n.codeStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ProjectWorktreePane(
    workspacePath: String,
    entries: List<WorkbenchProjectTreeEntry>,
    selectedFilePath: String?,
    onOpenFile: (String) -> Unit,
    gitStatus: WorkbenchGitStatus,
    onRefreshGitStatus: () -> Unit,
    /** The folders the user opened; every other folder is closed. */
    expandedFolders: Set<String> = emptySet(),
    onExpandedFoldersChange: (Set<String>) -> Unit = {},
    /** Compact rows and following the open file, saved for every folder. */
    options: CodeExplorerOptions = CodeExplorerOptions(),
    onOptionsChange: (CodeExplorerOptions) -> Unit = {},
    /** Reads the children of a folder the scan deferred; called off the UI thread. */
    onLoadFolder: (String) -> List<WorkbenchProjectTreeEntry> = { emptyList() },
    onCreateNote: (String) -> WorkbenchFileCreateOutcome = { name ->
        WorkbenchFileCreateOutcome.Failed(name, "Creating notes is not wired.")
    },
    modifier: Modifier = Modifier,
) {
    val strings = codeStrings()
    var newNoteName by remember { mutableStateOf<String?>(null) }
    var newNoteError by remember { mutableStateOf<String?>(null) }
    var loadedFolders by remember(entries) { mutableStateOf(emptyMap<String, List<WorkbenchProjectTreeEntry>>()) }
    // The scan as the options lay it out; read children of deferred folders are laid out the same way.
    val arranged = remember(entries, options) { arrangeProjectTree(entries, options) }
    val arrangedLoaded = remember(loadedFolders, options) { loadedFolders.mapValues { arrangeProjectTree(it.value, options) } }
    val rows = remember(arranged, expandedFolders, arrangedLoaded) {
        visibleProjectTreeRows(arranged, expandedFolders, arrangedLoaded)
    }
    val pendingFolders = deferredFoldersToLoad(rows, expandedFolders, loadedFolders)
    LaunchedEffect(pendingFolders) {
        for (folder in pendingFolders) {
            val children = withContext(Dispatchers.Default) { onLoadFolder(folder) }
            loadedFolders = loadedFolders + (folder to children)
        }
    }
    val changes = remember(gitStatus) { projectTreeChanges(gitStatus) }
    val listState = rememberLazyListState()
    // A file to bring into view: its folders open first, then the list scrolls once its row exists.
    var revealPath by remember { mutableStateOf<String?>(null) }
    val reveal = { path: String ->
        val revealed = ProjectExplorerPolicy.revealFolders(path, expandedFolders)
        if (revealed != expandedFolders) onExpandedFoldersChange(revealed)
        revealPath = path
    }
    LaunchedEffect(revealPath, rows) {
        val path = revealPath ?: return@LaunchedEffect
        val index = ProjectExplorerPolicy.revealIndex(rows, path)
        if (index >= 0) {
            listState.animateScrollToItem(index)
            revealPath = null
        }
    }
    LaunchedEffect(selectedFilePath, options.followOpenFile) {
        if (options.followOpenFile) selectedFilePath?.let(reveal)
    }
    // Opening a file from quick open also opens the folders above it, so the tree shows where it lives.
    val openFileInTree = { path: String ->
        reveal(path)
        onOpenFile(path)
    }
    // Every folder the tree knows, except those left unread or ignored, so a dependency folder never floods it.
    val expandAll = {
        onExpandedFoldersChange(ProjectExplorerPolicy.expandAll(entries, loadedFolders, expandedFolders))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .ckSurface(MoruSurfaceRole.Panel),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        ProjectExplorerToolbar(
            workspacePath = workspacePath,
            canLocate = selectedFilePath != null,
            onLocate = { selectedFilePath?.let(reveal) },
            onExpandAll = expandAll,
            onCollapseAll = { onExpandedFoldersChange(emptySet()) },
            options = options,
            onOptionsChange = onOptionsChange,
            composingNote = newNoteName != null,
            onToggleNote = {
                newNoteName = if (newNoteName == null) "" else null
                newNoteError = null
            },
        )

        newNoteName?.let { pendingName ->
            NewNoteInputRow(
                name = pendingName,
                error = newNoteError,
                onNameChange = { updated ->
                    newNoteName = updated
                    newNoteError = null
                },
                onConfirm = {
                    when (val outcome = onCreateNote(pendingName)) {
                        is WorkbenchFileCreateOutcome.Created -> {
                            newNoteName = null
                            newNoteError = null
                        }
                        is WorkbenchFileCreateOutcome.Failed -> newNoteError = outcome.message
                    }
                },
            )
        }

        var quickOpenQuery by remember { mutableStateOf("") }
        MoruTextField(
            value = quickOpenQuery,
            onValueChange = { quickOpenQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            placeholder = strings.quickOpenPlaceholder,
            singleLine = true,
        )
        val matches = remember(quickOpenQuery, entries) {
            quickOpenMatches(quickOpenQuery, entries.filter { it.isFile }.map { it.path })
        }

        if (!ProjectExplorerPolicy.isSearchActive(quickOpenQuery)) {
            // Measured in the bold a selected row uses, so selecting the widest name never clips it.
            val contentWidth = rememberProjectTreeContentWidth(rows, projectTreeNameStyle(selected = true))
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                // Rows as wide as the longest name; wider than the pane, the tree scrolls sideways.
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .width(maxOf(maxWidth, contentWidth))
                        .fillMaxHeight()
                        .padding(horizontal = 0.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(
                        items = rows,
                        key = { entry -> entry.path },
                    ) { entry ->
                        val expanded = entry.path in expandedFolders
                        ProjectTreeRow(
                            entry = entry,
                            expanded = expanded,
                            selected = entry.path == selectedFilePath,
                            change = changes[entry.path],
                            compact = options.compact,
                            onClick = {
                                when {
                                    entry.isFile -> onOpenFile(entry.path)
                                    expanded -> onExpandedFoldersChange(expandedFolders - entry.path)
                                    else -> onExpandedFoldersChange(expandedFolders + entry.path)
                                }
                            },
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 0.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                if (matches.isEmpty()) {
                    item(key = "no-matches") {
                        MoruText(
                            text = strings.noMatches,
                            color = MoruTheme.colors.textSecondary,
                            style = MoruTheme.typography.caption,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
                items(items = matches, key = { path -> path }) { path ->
                    QuickOpenRow(
                        path = path,
                        selected = path == selectedFilePath,
                        onOpenFile = openFileInTree,
                    )
                }
            }
        }
        WorkbenchGitStatusPane(
            status = gitStatus,
            onRefresh = onRefreshGitStatus,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
