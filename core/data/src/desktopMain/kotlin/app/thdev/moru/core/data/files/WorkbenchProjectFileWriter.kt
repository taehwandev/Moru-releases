package app.thdev.moru.core.data.files

import app.thdev.moru.core.model.files.WorkbenchFileSaveOutcome
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Writes edited text back to a project file. Mirrors the path safety of
 * [openWorkbenchProjectFilePreview]: the target must resolve inside the active
 * project root and must already exist as a regular file, so saving can only
 * overwrite a file the editor actually opened.
 */
internal fun File.saveWorkbenchProjectFile(
    relativePath: String,
    content: String,
): WorkbenchFileSaveOutcome {
    val normalizedPath = relativePath.trim()
    if (normalizedPath.isBlank()) {
        return WorkbenchFileSaveOutcome.Failed(relativePath, "No file path was selected.")
    }

    val root = canonicalFile
    val target = resolve(normalizedPath).canonicalFile
    if (!target.toPath().startsWith(root.toPath())) {
        return WorkbenchFileSaveOutcome.Failed(normalizedPath, "Path is outside the active project.")
    }
    if (!target.exists()) {
        return WorkbenchFileSaveOutcome.Failed(normalizedPath, "File does not exist.")
    }
    if (!target.isFile) {
        return WorkbenchFileSaveOutcome.Failed(normalizedPath, "Only regular files can be saved.")
    }

    val bytes = content.toByteArray(StandardCharsets.UTF_8)
    return runCatching {
        target.outputStream().use { stream -> stream.write(bytes) }
        WorkbenchFileSaveOutcome.Saved(normalizedPath, bytes.size.toLong())
    }.getOrElse { error ->
        WorkbenchFileSaveOutcome.Failed(
            normalizedPath,
            "Unable to save file: ${error.message ?: "unknown error"}",
        )
    }
}
