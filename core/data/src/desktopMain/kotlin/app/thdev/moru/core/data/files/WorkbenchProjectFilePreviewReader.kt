package app.thdev.moru.core.data.files

import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.files.WorkbenchFilePreviewStatus
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

internal fun File.openWorkbenchProjectFilePreview(relativePath: String): WorkbenchFilePreview {
    val normalizedPath = relativePath.trim()
    if (normalizedPath.isBlank()) {
        return WorkbenchFilePreview.unavailable(relativePath, "No file path was selected.")
    }

    val root = canonicalFile
    val target = resolve(normalizedPath).canonicalFile
    if (!target.toPath().startsWith(root.toPath())) {
        return WorkbenchFilePreview.unavailable(normalizedPath, "Path is outside the active project.")
    }
    if (!target.exists()) {
        return WorkbenchFilePreview(
            path = normalizedPath,
            name = target.name.ifBlank { normalizedPath.substringAfterLast('/') },
            status = WorkbenchFilePreviewStatus.MISSING,
            sizeBytes = null,
            content = null,
            summaryText = "File does not exist.",
        )
    }
    if (!target.isFile) {
        return WorkbenchFilePreview.unavailable(normalizedPath, "Only regular files can be previewed.")
    }

    val sizeBytes = target.length()
    if (sizeBytes > MaxWorkbenchFileInlineBytes) {
        return WorkbenchFilePreview(
            path = normalizedPath,
            name = target.name,
            status = WorkbenchFilePreviewStatus.OVERSIZED,
            sizeBytes = sizeBytes,
            content = null,
            summaryText = "File is too large for inline preview.",
            truncated = true,
            truncationText = "Large files show metadata only to keep the Workbench responsive.",
        )
    }

    val bytes = runCatching {
        target.inputStream().use { stream ->
            stream.readNBytes(MaxWorkbenchFilePreviewBytes + 1)
        }
    }.getOrElse { error ->
        return WorkbenchFilePreview.unavailable(
            normalizedPath,
            "Unable to read file: ${error.message ?: "unknown error"}",
        )
    }

    if (bytes.isLikelyBinary()) {
        return WorkbenchFilePreview(
            path = normalizedPath,
            name = target.name,
            status = WorkbenchFilePreviewStatus.BINARY,
            sizeBytes = sizeBytes,
            content = null,
            summaryText = "Binary file preview is disabled.",
        )
    }

    val previewBytes = if (bytes.size > MaxWorkbenchFilePreviewBytes) {
        bytes.copyOf(MaxWorkbenchFilePreviewBytes)
    } else {
        bytes
    }
    val content = previewBytes.decodeUtf8OrNull()
        ?: return WorkbenchFilePreview(
            path = normalizedPath,
            name = target.name,
            status = WorkbenchFilePreviewStatus.BINARY,
            sizeBytes = sizeBytes,
            content = null,
            summaryText = "File is not valid UTF-8 text.",
        )
    val truncated = bytes.size > MaxWorkbenchFilePreviewBytes || sizeBytes > previewBytes.size
    val previewLineCount = content.lineCount()

    return WorkbenchFilePreview(
        path = normalizedPath,
        name = target.name,
        status = WorkbenchFilePreviewStatus.TEXT,
        sizeBytes = sizeBytes,
        content = content,
        summaryText = if (truncated) {
            "Showing $previewLineCount lines / ${previewBytes.size} of $sizeBytes bytes"
        } else {
            "$previewLineCount lines / $sizeBytes bytes"
        },
        truncated = truncated,
        truncationText = if (truncated) {
            "Preview is bounded; open the file in an editor for the full content."
        } else {
            null
        },
    )
}

private fun ByteArray.decodeUtf8OrNull(): String? {
    return try {
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(this))
            .toString()
    } catch (_: CharacterCodingException) {
        null
    }
}

private fun ByteArray.isLikelyBinary(): Boolean {
    if (isEmpty()) return false
    if (any { byte -> byte.toInt() == 0 }) return true

    val controlCount = count { byte ->
        val value = byte.toInt() and 0xff
        value < 0x20 && value !in TextControlBytes
    }
    return controlCount > size / 10
}

private fun String.lineCount(): Int {
    return if (isEmpty()) 0 else lineSequence().count()
}

private const val MaxWorkbenchFilePreviewBytes = 96 * 1024
private const val MaxWorkbenchFileInlineBytes = 256 * 1024L
private val TextControlBytes = setOf(0x08, 0x09, 0x0a, 0x0c, 0x0d)
