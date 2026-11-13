package app.thdev.moru.core.data.files

import app.thdev.moru.core.model.files.WorkbenchFileDeleteOutcome
import app.thdev.moru.core.model.files.WorkbenchFileRenameOutcome
import java.io.File

/**
 * Rename (move) an existing project file. Both source and destination must
 * resolve inside the project root; the source must exist as a regular file and
 * the destination must not already exist, so a rename can never clobber another
 * file or escape the workspace.
 */
internal fun File.renameWorkbenchProjectFile(
    fromRelativePath: String,
    toRelativePath: String,
): WorkbenchFileRenameOutcome {
    val fromPath = fromRelativePath.trim()
    val toPath = toRelativePath.trim()
    if (fromPath.isBlank() || toPath.isBlank()) {
        return WorkbenchFileRenameOutcome.Failed(fromRelativePath, "A source and target name are required.")
    }

    val root = canonicalFile
    val source = resolve(fromPath).canonicalFile
    val target = resolve(toPath).canonicalFile
    if (!source.toPath().startsWith(root.toPath()) || !target.toPath().startsWith(root.toPath())) {
        return WorkbenchFileRenameOutcome.Failed(fromPath, "Path is outside the active project.")
    }
    if (!source.isFile) {
        return WorkbenchFileRenameOutcome.Failed(fromPath, "Only existing files can be renamed.")
    }
    if (target.exists()) {
        return WorkbenchFileRenameOutcome.Failed(toPath, "A file with that name already exists.")
    }

    return runCatching {
        target.parentFile?.mkdirs()
        if (source.renameTo(target)) {
            WorkbenchFileRenameOutcome.Renamed(fromPath, toPath)
        } else {
            WorkbenchFileRenameOutcome.Failed(fromPath, "The file could not be renamed.")
        }
    }.getOrElse { error ->
        WorkbenchFileRenameOutcome.Failed(fromPath, "Unable to rename file: ${error.message ?: "unknown error"}")
    }
}

/**
 * Delete an existing project file. The target must resolve inside the project
 * root and must be a regular file, so a delete can never remove a directory or
 * escape the workspace.
 */
internal fun File.deleteWorkbenchProjectFile(relativePath: String): WorkbenchFileDeleteOutcome {
    val normalizedPath = relativePath.trim()
    if (normalizedPath.isBlank()) {
        return WorkbenchFileDeleteOutcome.Failed(relativePath, "No file was selected.")
    }

    val root = canonicalFile
    val target = resolve(normalizedPath).canonicalFile
    if (!target.toPath().startsWith(root.toPath())) {
        return WorkbenchFileDeleteOutcome.Failed(normalizedPath, "Path is outside the active project.")
    }
    if (!target.isFile) {
        return WorkbenchFileDeleteOutcome.Failed(normalizedPath, "Only existing files can be deleted.")
    }

    return runCatching {
        if (target.delete()) {
            WorkbenchFileDeleteOutcome.Deleted(normalizedPath)
        } else {
            WorkbenchFileDeleteOutcome.Failed(normalizedPath, "The file could not be deleted.")
        }
    }.getOrElse { error ->
        WorkbenchFileDeleteOutcome.Failed(normalizedPath, "Unable to delete file: ${error.message ?: "unknown error"}")
    }
}
