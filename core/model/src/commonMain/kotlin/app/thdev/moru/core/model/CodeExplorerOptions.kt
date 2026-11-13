package app.thdev.moru.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * How the Code explorer lists files: tighter rows, whether the open file is always shown and selected, how each
 * folder's entries are ordered, whether what git ignores is listed, and whether a chain of folders that each hold only
 * one folder reads as one row (`src/main/java`).
 */
@Serializable
data class CodeExplorerOptions(
    val compact: Boolean = false,
    @SerialName("follow_open_file") val followOpenFile: Boolean = false,
    @SerialName("sort_by") val sortBy: CodeExplorerSort = CodeExplorerSort.Name,
    @SerialName("folders_first") val foldersFirst: Boolean = true,
    @SerialName("show_ignored") val showIgnored: Boolean = true,
    @SerialName("merge_single_folders") val mergeSingleFolders: Boolean = false,
)
