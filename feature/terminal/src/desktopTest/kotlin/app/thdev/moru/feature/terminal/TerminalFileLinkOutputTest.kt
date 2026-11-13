package app.thdev.moru.feature.terminal

import com.jediterm.terminal.HyperlinkStyle
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TerminalFileLinkOutputTest {
    private val key = "file-link-${System.nanoTime()}"

    @AfterTest
    fun release() {
        releaseTerminalSession(key)
        TerminalFileOpens.clear()
    }

    @Test
    fun aPathPrintedOnTheMainScreenOpensItsFile() = assertPrintedPathOpens(alternateScreen = false)

    /** Claude Code and Codex draw on the alternate screen. */
    @Test
    fun aPathPrintedOnTheAlternateScreenOpensItsFile() = assertPrintedPathOpens(alternateScreen = true)

    private fun assertPrintedPathOpens(alternateScreen: Boolean) {
        val folder = Files.createTempDirectory("printed").toFile().canonicalFile
        File(folder, "docs").mkdirs()
        val prd = File(folder, "docs/PRD.md").apply { writeText("# PRD") }
        val enter = if (alternateScreen) "\\033[?1049h" else ""
        val entry = acquireDesktopTerminalSession(
            sessionKey = key,
            workingDirectory = folder.path,
            shell = "/bin/sh",
            startupCommand = "printf '${enter}Wrote docs/PRD.md\\n'; sleep 30",
            onCommandCaptured = {},
            fontFamily = null,
            fontSize = 13,
            scrollbackLines = 100,
        )

        val link = waitForLink(entry.session)
        assertNotNull(link, "the printed path should be a link")
        link.linkInfo.navigate()

        val request = assertNotNull(TerminalFileOpens.request)
        assertEquals(key, request.sessionKey)
        assertEquals(prd.path, request.path)
    }

    private fun waitForLink(session: JediTermSession): HyperlinkStyle? {
        val deadline = System.nanoTime() + 15_000_000_000L
        while (System.nanoTime() < deadline) {
            val buffer = session.textBuffer
            buffer.lock()
            val found = try {
                (0 until buffer.screenLinesCount).asSequence()
                    .flatMap { buffer.getLine(it).entries.asSequence() }
                    .map { it.style }.filterIsInstance<HyperlinkStyle>().firstOrNull()
            } finally {
                buffer.unlock()
            }
            if (found != null) return found
            Thread.sleep(50)
        }
        return null
    }
}
