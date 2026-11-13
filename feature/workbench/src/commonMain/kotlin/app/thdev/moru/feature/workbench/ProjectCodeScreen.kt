package app.thdev.moru.feature.workbench

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.CodeExplorerOptions
import app.thdev.moru.core.model.files.WorkbenchFileCreateOutcome
import app.thdev.moru.core.model.files.WorkbenchFileDeleteOutcome
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.files.WorkbenchFilePreviewStatus
import app.thdev.moru.core.model.files.WorkbenchFileRenameOutcome
import app.thdev.moru.core.model.files.WorkbenchFileSaveOutcome
import app.thdev.moru.core.model.files.WorkbenchProjectTreeEntry
import app.thdev.moru.core.model.git.WorkbenchGitStatus
import app.thdev.moru.feature.workbench.i18n.codeStrings

/**
 * The project's code, IDE-style: the file tree on the left and the chosen file on the right,
 * editable and saved through the same boundaries the Work tab uses. It only reads and edits
 * files. A supplied HTML renderer previews web content without starting project commands.
 */
@Composable
fun ProjectCodeScreen(
    workspacePath: String,
    entries: List<WorkbenchProjectTreeEntry>,
    gitStatus: WorkbenchGitStatus,
    onRefreshGitStatus: () -> Unit,
    onOpenProjectFile: (String) -> WorkbenchFilePreview,
    onSaveProjectFile: (String, String) -> WorkbenchFileSaveOutcome,
    onCreateProjectFile: (String) -> WorkbenchFileCreateOutcome,
    onRenameProjectFile: (String, String) -> WorkbenchFileRenameOutcome,
    onDeleteProjectFile: (String) -> WorkbenchFileDeleteOutcome,
    onRefreshProjectTree: () -> Unit,
    modifier: Modifier = Modifier,
    /** The file that was open in this folder last time, opened again if it is still there. */
    openFilePath: String? = null,
    onOpenFileChange: (path: String?) -> Unit = {},
    /** The folders open in the explorer last time; every other folder starts closed. */
    expandedFolders: Set<String> = emptySet(),
    onExpandedFoldersChange: (Set<String>) -> Unit = {},
    onLoadProjectFolder: (String) -> List<WorkbenchProjectTreeEntry> = { emptyList() },
    /** The explorer's options as last saved; they are the same in every folder. */
    explorerOptions: CodeExplorerOptions = CodeExplorerOptions(),
    onExplorerOptionsChange: (CodeExplorerOptions) -> Unit = {},
) {
    val strings = codeStrings()
    var options by remember { mutableStateOf(explorerOptions) }
    // It starts on last time's file when that file is still there, so there is never an empty first frame to report.
    var editorState by remember(workspacePath) {
        mutableStateOf(
            openFilePath?.let(onOpenProjectFile)
                ?.takeIf { preview -> preview.status != WorkbenchFilePreviewStatus.MISSING && preview.status != WorkbenchFilePreviewStatus.UNAVAILABLE }
                ?.let(WorkbenchEditorState::of),
        )
    }
    var saveStatus by remember(workspacePath) { mutableStateOf<String?>(null) }
    val openFile = { path: String ->
        editorState = WorkbenchEditorState.of(onOpenProjectFile(path))
        saveStatus = null
    }
    // Starts from last time's open folders and keeps working even when nothing saves them.
    var openFolders by remember(workspacePath) { mutableStateOf(expandedFolders) }
    val openPath = editorState?.preview?.path
    LaunchedEffect(openPath) { onOpenFileChange(openPath) }

    Row(modifier = modifier.fillMaxSize().ckSurface(MoruSurfaceRole.Page)) {
        ProjectWorktreePane(
            workspacePath = workspacePath,
            entries = entries,
            selectedFilePath = editorState?.preview?.path,
            onOpenFile = openFile,
            onCreateNote = fun(rawName: String): WorkbenchFileCreateOutcome {
                val relativePath = newNoteRelativePath(rawName)
                    ?: return WorkbenchFileCreateOutcome.Failed(rawName, "Invalid note name.")
                val outcome = onCreateProjectFile(relativePath)
                if (outcome is WorkbenchFileCreateOutcome.Created) {
                    openFile(outcome.path)
                    onRefreshProjectTree()
                }
                return outcome
            },
            gitStatus = gitStatus,
            onRefreshGitStatus = onRefreshGitStatus,
            expandedFolders = openFolders,
            onExpandedFoldersChange = { folders ->
                openFolders = folders
                onExpandedFoldersChange(folders)
            },
            onLoadFolder = onLoadProjectFolder,
            options = options,
            onOptionsChange = { changed ->
                options = changed
                onExplorerOptionsChange(changed)
            },
            modifier = Modifier.width(280.dp).fillMaxHeight(),
        )
        val state = editorState
        if (state == null) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(24.dp), contentAlignment = Alignment.Center) {
                MoruText(text = strings.selectFilePrompt, color = MoruTheme.colors.textSecondary)
            }
        } else {
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                ProjectFilePreviewPane(
                    state = state,
                    onClose = {
                        editorState = null
                        saveStatus = null
                    },
                    onRename = fun(rawName: String): WorkbenchFileRenameOutcome {
                        val target = newNoteRelativePath(rawName)
                            ?: return WorkbenchFileRenameOutcome.Failed(rawName, "Invalid name.")
                        val outcome = onRenameProjectFile(state.preview.path, target)
                        if (outcome is WorkbenchFileRenameOutcome.Renamed) {
                            openFile(outcome.toPath)
                            onRefreshProjectTree()
                        }
                        return outcome
                    },
                    onDelete = {
                        if (onDeleteProjectFile(state.preview.path) is WorkbenchFileDeleteOutcome.Deleted) {
                            editorState = null
                            saveStatus = null
                            onRefreshProjectTree()
                        }
                    },
                    onDraftChange = { draft ->
                        editorState = state.withDraft(draft)
                        saveStatus = null
                    },
                    onSave = save@{
                        if (!state.isDirty) return@save
                        when (val outcome = onSaveProjectFile(state.preview.path, state.draft)) {
                            is WorkbenchFileSaveOutcome.Saved -> {
                                editorState = state.markSaved()
                                saveStatus = strings.savedBytes(outcome.bytesWritten)
                            }
                            is WorkbenchFileSaveOutcome.Failed -> saveStatus = outcome.message
                        }
                    },
                    saveStatus = saveStatus,
                    onOpenWikiLink = { target ->
                        if (state.isDirty) {
                            saveStatus = "Save your changes before opening a document link."
                        } else {
                            val path = resolveMarkdownDocumentLink(state.preview.path, target, entries.filter { it.isFile }.map { it.path })
                            if (path == null) saveStatus = "This document link is unavailable or outside the project."
                            else openFile(path)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
    }
}
