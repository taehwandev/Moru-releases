package app.thdev.moru.core.data.files

import app.thdev.moru.core.model.files.WorkbenchFileCreateOutcome
import java.io.File

/**
 * Create a new empty project file. Mirrors the writer's path safety: the target
 * must resolve inside the project root and must not already exist, so creation
 * can never clobber an existing file or escape the workspace.
 */
internal fun File.createWorkbenchProjectFile(relativePath: String): WorkbenchFileCreateOutcome {
    val normalizedPath = relativePath.trim()
    if (normalizedPath.isBlank()) {
        return WorkbenchFileCreateOutcome.Failed(relativePath, "No file name was provided.")
    }

    val root = canonicalFile
    val target = resolve(normalizedPath).canonicalFile
    if (!target.toPath().startsWith(root.toPath())) {
        return WorkbenchFileCreateOutcome.Failed(normalizedPath, "Path is outside the active project.")
    }
    if (target.exists()) {
        return WorkbenchFileCreateOutcome.Failed(normalizedPath, "File already exists.")
    }

    return runCatching {
        target.parentFile?.mkdirs()
        if (!target.createNewFile()) {
            return@runCatching WorkbenchFileCreateOutcome.Failed(normalizedPath, "File already exists.")
        }
        WorkbenchFileCreateOutcome.Created(normalizedPath)
    }.getOrElse { error ->
        WorkbenchFileCreateOutcome.Failed(
            normalizedPath,
            "Unable to create file: ${error.message ?: "unknown error"}",
        )
    }
}
