package app.thdev.moru.app

import java.awt.EventQueue
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser

/**
 * Show a native directory chooser and return the selected folder's absolute path,
 * or null if the user cancels. On macOS this uses AWT [FileDialog] in directory
 * mode (the reliable native picker under Compose Desktop); elsewhere it falls
 * back to [JFileChooser]. Runs on the AWT event thread. The dialog opens in [startIn] (the folder holding the open
 * project, where its sibling projects are) rather than wherever the system last left it.
 */
internal fun pickProjectFolder(startIn: File? = null, title: String = "Open Project"): String? {
    var result: String? = null
    val show = { result = chooseDirectory(startIn?.takeIf { folder -> folder.isDirectory }, title) }
    if (EventQueue.isDispatchThread()) {
        show()
    } else {
        EventQueue.invokeAndWait(show)
    }
    System.err.println("[Moru] pickProjectFolder -> $result")
    return result
}

private fun chooseDirectory(startIn: File?, title: String): String? {
    val isMac = System.getProperty("os.name").orEmpty().lowercase().let { os ->
        os.contains("mac") || os.contains("darwin")
    }
    if (isMac) {
        val previous = System.getProperty("apple.awt.fileDialogForDirectories")
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        try {
            val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
            startIn?.let { folder -> dialog.directory = folder.absolutePath }
            dialog.isVisible = true
            val directory = dialog.directory
            val file = dialog.file
            return if (directory != null && file != null) File(directory, file).absolutePath else null
        } finally {
            if (previous != null) {
                System.setProperty("apple.awt.fileDialogForDirectories", previous)
            } else {
                System.clearProperty("apple.awt.fileDialogForDirectories")
            }
        }
    }
    val chooser = JFileChooser().apply {
        dialogTitle = title
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isMultiSelectionEnabled = false
        startIn?.let { folder -> currentDirectory = folder }
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile?.absolutePath
    } else {
        null
    }
}

private val ImageExtensions = setOf("png", "jpg", "jpeg", "gif", "webp", "heic", "bmp", "tif", "tiff")

/** Shows the system file chooser for pictures, several at once, and returns their absolute paths; empty on cancel. */
internal fun pickImages(title: String): List<String> {
    return pickLocalFiles(title, imagesOnly = true)
}

/** Selects local files of any type. */
internal fun pickFiles(title: String): List<String> = pickLocalFiles(title, imagesOnly = false)

private fun pickLocalFiles(title: String, imagesOnly: Boolean): List<String> {
    var result = emptyList<String>()
    val show = {
        val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
        dialog.isMultipleMode = true
        if (imagesOnly) dialog.setFilenameFilter { _, name -> name.substringAfterLast('.').lowercase() in ImageExtensions }
        dialog.isVisible = true
        result = dialog.files.orEmpty().filter { it.isFile }.map { it.absolutePath }
    }
    if (EventQueue.isDispatchThread()) show() else EventQueue.invokeAndWait(show)
    return result
}
