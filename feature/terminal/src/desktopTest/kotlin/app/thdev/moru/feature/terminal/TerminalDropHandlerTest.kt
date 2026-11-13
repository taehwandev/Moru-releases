package app.thdev.moru.feature.terminal

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File
import java.nio.file.Files
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * A file dropped on a terminal is pasted as a path, so Claude Code and Codex attach it the way they do when a file
 * is dropped on Terminal.app. What other drops paste is covered by the core TerminalDropReaderTest.
 */
class TerminalDropHandlerTest {
    private val folder = Files.createTempDirectory("moru-drop-test").toFile()

    @Test
    fun droppedFilesArePastedAsEscapedPathsInsideABracketedPaste() = PreeditTestFixture().use { fixture ->
        fixture.output("\u001B[?2004h")
        val image = File(folder, "스크린샷 (1).png")
        val handler = fixture.panel.transferHandler as TerminalDropHandler

        SwingUtilities.invokeAndWait { assertTrue(handler.importFrom(FilesDropped(listOf(image)))) }

        val expected = "\u001B[200~${folder.absolutePath}/스크린샷\\ \\(1\\).png \u001B[201~"
        assertTrue(fixture.connector.awaitText(expected), fixture.connector.writtenText())
    }

    private class FilesDropped(private val files: List<File>) : Transferable {
        override fun getTransferDataFlavors() = arrayOf(DataFlavor.javaFileListFlavor)
        override fun isDataFlavorSupported(flavor: DataFlavor) = flavor == DataFlavor.javaFileListFlavor
        override fun getTransferData(flavor: DataFlavor): Any = files
    }
}
