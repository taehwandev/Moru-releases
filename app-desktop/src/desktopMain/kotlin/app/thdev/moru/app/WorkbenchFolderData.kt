package app.thdev.moru.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import app.thdev.moru.core.model.git.WorkbenchGitStatus
import app.thdev.moru.core.model.files.WorkbenchProjectTreeEntry
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Tree and git observations carry the folder that their desktop callbacks operate on. */
internal data class WorkbenchFolderData(
    val rootPath: String,
    val treeEntries: List<WorkbenchProjectTreeEntry>,
    val gitStatus: WorkbenchGitStatus,
) {
    fun forFolder(folder: File): WorkbenchFolderData =
        if (rootPath == folder.absolutePath) this else empty(folder)

    companion object {
        fun empty(folder: File) = WorkbenchFolderData(
            rootPath = folder.absolutePath,
            treeEntries = emptyList(),
            gitStatus = WorkbenchGitStatus.NotRepository,
        )
    }
}

/** A different folder owns fresh state immediately, including the frame before its scans finish. */
@Composable
internal fun rememberWorkbenchFolderData(
    displayedRoot: File,
    hasProject: Boolean,
    treeRefreshKey: Int,
    gitRefreshKey: Int,
    scanGit: (File) -> WorkbenchGitStatus,
    scanTree: (File) -> List<WorkbenchProjectTreeEntry>,
): WorkbenchFolderData {
    val scanRoot = displayedRoot.absoluteFile
    return key(scanRoot.absolutePath, hasProject) {
        val treeEntries by produceState(initialValue = emptyList<WorkbenchProjectTreeEntry>(), treeRefreshKey) {
            value = if (hasProject) withContext(Dispatchers.IO) { scanTree(scanRoot) } else emptyList()
        }
        val gitStatus by produceState(initialValue = WorkbenchGitStatus.NotRepository, gitRefreshKey) {
            value = if (hasProject) withContext(Dispatchers.IO) { scanGit(scanRoot) }
                else WorkbenchGitStatus.NotRepository
        }
        WorkbenchFolderData(scanRoot.absolutePath, treeEntries, gitStatus).forFolder(displayedRoot)
    }
}
