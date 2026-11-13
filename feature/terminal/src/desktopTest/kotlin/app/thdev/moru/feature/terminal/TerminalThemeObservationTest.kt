package app.thdev.moru.feature.terminal

import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.designsystem.theme.lightCkColors
import app.thdev.moru.core.terminal.SessionShellProcesses
import app.thdev.moru.core.terminal.SessionThemeColors
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** A real native session must keep its shell and typed input while the canvas changes. */
class TerminalThemeObservationTest {
    @Test
    fun themeChangesKeepTheShellAndARealNewQueryClearsTheStaleObservation() {
        val key = "theme-terminal-${UUID.randomUUID()}"
        val directory = Files.createTempDirectory("moru-terminal-theme").toFile()
        val commands = CopyOnWriteArrayList<String>()
        val exited = CountDownLatch(1)
        directory.resolve("probe.py").writeText("""
            import os, pathlib, select, sys, time, tty
            tty.setraw(sys.stdin.fileno())
            pathlib.Path('shell.pid').write_text(str(os.getppid()))
            sys.stdout.write('\x1b]11;?\x07')
            sys.stdout.flush()
            asked_again = False
            with open('input.bin', 'wb', buffering=0) as received:
                while not pathlib.Path('stop').exists():
                    if not asked_again and pathlib.Path('query-again').exists():
                        sys.stdout.write('\x1b]11;?\x07')
                        sys.stdout.flush()
                        asked_again = True
                    if select.select([sys.stdin], [], [], 0.05)[0]:
                        received.write(os.read(sys.stdin.fileno(), 4096))
        """.trimIndent())
        try {
            startDesktopTerminalSession(
                key, directory.absolutePath, "/bin/sh",
                "/usr/bin/python3 '${directory.resolve("probe.py").absolutePath}'",
                { commands += it }, null, 13, 100, onExit = { exited.countDown() }, colors = darkCkColors,
            )
            val entry = requireNotNull(JediTermSessionRegistry.find(key))
            val inputFile = directory.resolve("input.bin")
            waitFor("the initial OSC 11 reply") {
                inputFile.exists() && inputFile.readText().let { text ->
                    text.contains("\u001b]11;rgb:") && (text.endsWith("\u001b\\") || text.endsWith("\u0007"))
                }
            }
            val shellPid = requireNotNull(SessionShellProcesses.snapshot()[key])
            assertEquals(shellPid, directory.resolve("shell.pid").readText().toLong())
            assertFalse(SessionThemeColors.hasStaleBackground(key))
            val inputBeforeTheme = inputFile.readBytes()
            val commandsBeforeTheme = commands.toList()

            SwingUtilities.invokeAndWait {
                entry.paletteHolder.palette = TerminalPalette.from(lightCkColors)
                entry.session.applyTheme()
            }
            assertTrue(SessionThemeColors.hasStaleBackground(key), "The startup background differs from the current canvas")
            SwingUtilities.invokeAndWait {
                entry.paletteHolder.palette = TerminalPalette.from(darkCkColors)
                entry.session.applyTheme()
            }
            assertFalse(SessionThemeColors.hasStaleBackground(key), "Reverting to the queried color requires no restart")
            SwingUtilities.invokeAndWait {
                entry.paletteHolder.palette = TerminalPalette.from(lightCkColors)
                entry.session.applyTheme()
            }
            assertTrue(SessionThemeColors.hasStaleBackground(key))
            Thread.sleep(200)
            assertEquals(shellPid, SessionShellProcesses.snapshot()[key])
            assertTrue(ProcessHandle.of(shellPid).orElseThrow().isAlive)
            assertContentEquals(inputBeforeTheme, inputFile.readBytes(), "Changing theme must send no typed command or unsolicited reply")
            assertEquals(commandsBeforeTheme, commands.toList())

            directory.resolve("query-again").writeText("")
            waitFor("the fresh background query") { !SessionThemeColors.hasStaleBackground(key) }
            waitFor("the new OSC 11 reply") { inputFile.length() > inputBeforeTheme.size }
            assertEquals(shellPid, SessionShellProcesses.snapshot()[key])
            assertEquals(commandsBeforeTheme, commands.toList(), "A real background query is not a typed command")
        } finally {
            if (JediTermSessionRegistry.find(key) != null) {
                directory.resolve("query-again").writeText("")
                directory.resolve("stop").writeText("")
                exited.await(2, TimeUnit.SECONDS)
            }
            SwingUtilities.invokeAndWait { JediTermSessionRegistry.release(key) }
            assertFalse(SessionThemeColors.hasStaleBackground(key), "Released sessions must lose their observations")
            directory.deleteRecursively()
        }
    }

    private fun waitFor(description: String, read: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            if (read()) return
            Thread.sleep(20)
        }
        error("Timed out waiting for $description")
    }
}
