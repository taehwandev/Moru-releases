package app.thdev.moru.core.terminal.drop

import java.awt.Image
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File
import java.net.URI
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

/** The flavor browsers use for dropped links, which a terminal accepts alongside files, images and text. */
val uriListFlavor: DataFlavor = DataFlavor("text/uri-list;class=java.lang.String")

/**
 * Turns what is dropped on a terminal into pasted text an agent CLI attaches: files become shell-escaped paths
 * (as Terminal.app does), browser images are saved to a file first, and web images are downloaded first.
 */
class TerminalDropReader(
    private val paste: (String) -> Unit,
    private val dropFolder: () -> File = ::defaultDropFolder,
    private val download: (URI, File) -> File? = ::downloadImage,
    private val inBackground: (() -> Unit) -> Unit = { work -> thread(isDaemon = true, name = "moru-terminal-drop") { work() } },
) {

    /** Pastes what [transferable] holds and returns true, or returns false when it holds nothing pasteable. */
    fun read(transferable: Transferable): Boolean {
        transferable.files()?.let { files ->
            paste(pastedPaths(files))
            return true
        }
        transferable.image()?.let { image ->
            paste(pastedPaths(listOf(saveImage(image, dropFolder()))))
            return true
        }
        val text = transferable.text() ?: return false
        val links = droppedUris(text)
        if (links == null) {
            paste(text)
            return true
        }
        if (links.all { it.scheme == "file" }) {
            paste(pastedPaths(links.map(::File)))
            return true
        }
        inBackground {
            val pasted = links.joinToString(" ") { link ->
                val file = if (link.scheme == "file") File(link) else download(link, dropFolder())
                file?.let(::shellPath) ?: link.toString()
            } + " "
            SwingUtilities.invokeLater { paste(pasted) }
        }
        return true
    }

    private fun Transferable.files(): List<File>? =
        if (isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            (getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)?.filterIsInstance<File>()?.takeIf { it.isNotEmpty() }
        } else {
            null
        }

    private fun Transferable.image(): Image? =
        if (isDataFlavorSupported(DataFlavor.imageFlavor)) getTransferData(DataFlavor.imageFlavor) as? Image else null

    private fun Transferable.text(): String? {
        val flavor = when {
            isDataFlavorSupported(uriListFlavor) -> uriListFlavor
            isDataFlavorSupported(DataFlavor.stringFlavor) -> DataFlavor.stringFlavor
            else -> return null
        }
        return (getTransferData(flavor) as? String)?.takeIf { it.isNotBlank() }
    }
}
