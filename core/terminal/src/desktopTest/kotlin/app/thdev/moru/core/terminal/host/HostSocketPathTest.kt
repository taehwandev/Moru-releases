package app.thdev.moru.core.terminal.host

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class HostSocketPathTest {
    private val temp = Files.createTempDirectory("moru-socket").toAbsolutePath()
    private val longDataDir = File("/Users/${"x".repeat(80)}/Library/Application Support/Moru Community")

    @Test
    fun aShortDataFolderKeepsTheSocketInItsRunFolder() {
        val dataDir = File("/Users/me/Library/Application Support/Moru")
        assertEquals(dataDir.resolve("run/terminal-host-1.sock").toPath(), TerminalHostLauncher.socketFor(dataDir, temp))
    }

    @Test
    fun aLongDataFolderUsesTheMoruNameInTheTempFolder() {
        val socket = TerminalHostLauncher.socketFor(longDataDir, temp)
        assertEquals(temp.resolve("moru-${longDataDir.absolutePath.hashCode()}-1.sock"), socket)
    }

}
