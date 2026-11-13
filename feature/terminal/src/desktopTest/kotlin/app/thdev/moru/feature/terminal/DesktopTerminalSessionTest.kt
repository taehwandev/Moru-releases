package app.thdev.moru.feature.terminal

import java.io.File
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.swing.SwingUtilities
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Exercises real sh PTYs without a Compose view or a visible Swing component. */
class DesktopTerminalSessionTest {
    private val keys = mutableListOf<String>()
    private val directories = mutableListOf<File>()

    @AfterTest
    fun cleanUp() {
        SwingUtilities.invokeAndWait { keys.forEach(JediTermSessionRegistry::release) }
        directories.forEach(File::deleteRecursively)
    }

    @Test
    fun hiddenLaunchRunsInItsFolderReportsEventsAndMountAcquisitionDoesNotRestartIt() {
        val key = newKey()
        val directory = newDirectory()
        val output = CountDownLatch(1)
        val lastOutputAt = AtomicLong()
        val exits = LinkedBlockingQueue<Int>()
        val onOutput: (Long) -> Unit = { lastOutputAt.set(it); output.countDown() }
        val onExit: (Int?) -> Unit = { it?.let(exits::add) }

        startDesktopTerminalSession(
            sessionKey = key,
            workingDirectory = directory.absolutePath,
            shell = "/bin/sh",
            startupCommand = "printf 'started\\n' >> started.txt; pwd > cwd.txt; printf 'hidden-session-output\\n'; sleep 0.2; exit 7",
            onCommandCaptured = {},
            fontFamily = null,
            fontSize = 13,
            scrollbackLines = 100,
            onOutput = onOutput,
            onExit = onExit,
        )
        val acquiredForView = acquireDesktopTerminalSession(
            key, directory.absolutePath, "/bin/sh", "printf 'duplicate\\n' >> started.txt; exit 0", {},
            null, 13, 100, onOutput, onExit,
        )
        val reacquired = acquireDesktopTerminalSession(
            key, directory.absolutePath, "/bin/sh", "printf 'duplicate\\n' >> started.txt; exit 0", {},
            null, 13, 100, onOutput, onExit,
        )

        assertSame(acquiredForView, reacquired)
        assertFalse(acquiredForView.session.component().isDisplayable, "No view was mounted by the launch path")
        assertTrue(output.await(10, TimeUnit.SECONDS), "Hidden terminal output must reach its owner")
        assertEquals(7, exits.poll(10, TimeUnit.SECONDS), "Hidden process exit must reach its owner")
        assertTrue(lastOutputAt.get() > 0)
        assertEquals("started\n", directory.resolve("started.txt").readText())
        assertEquals(directory.canonicalPath, File(directory.resolve("cwd.txt").readText().trim()).canonicalPath)
    }

    @Test
    fun immediateExitIsReportedBeforeAnyViewExists() {
        val exits = LinkedBlockingQueue<Int>()
        startDesktopTerminalSession(
            sessionKey = newKey(), workingDirectory = newDirectory().absolutePath,
            shell = "/bin/sh", startupCommand = "exit 23", onCommandCaptured = {},
            fontFamily = null, fontSize = 13, scrollbackLines = 100,
            onExit = { it?.let(exits::add) },
        )
        assertEquals(23, exits.poll(10, TimeUnit.SECONDS))
    }

    @Test
    fun failedStartupDoesNotPoisonTheKeyAndCanBeRetried() {
        val key = newKey()
        val directory = newDirectory()
        assertFails {
            startDesktopTerminalSession(
                key, directory.absolutePath, "/missing/moru-shell", null, {}, null, 13, 100,
            )
        }
        val exits = LinkedBlockingQueue<Int>()
        startDesktopTerminalSession(
            key, directory.absolutePath, "/bin/sh", "printf 'retried\\n' > retry.txt; exit 0", {}, null, 13, 100,
            onExit = { it?.let(exits::add) },
        )
        assertEquals(0, exits.poll(10, TimeUnit.SECONDS))
        assertEquals("retried\n", directory.resolve("retry.txt").readText())
    }

    @Test
    fun startingFromTheSwingEventThreadDoesNotRedispatchOrWaitOnItself() {
        val key = newKey()
        val directory = newDirectory()
        val exits = LinkedBlockingQueue<Int>()
        SwingUtilities.invokeAndWait {
            startDesktopTerminalSession(
                key, directory.absolutePath, "/bin/sh", "printf 'edt\\n' > edt.txt; exit 0", {}, null, 13, 100,
                onExit = { it?.let(exits::add) },
            )
        }
        assertEquals(0, exits.poll(10, TimeUnit.SECONDS))
        assertEquals("edt\n", directory.resolve("edt.txt").readText())
    }

    private fun newKey() = "hidden-terminal-${UUID.randomUUID()}".also(keys::add)
    private fun newDirectory() = Files.createTempDirectory("moru-hidden-terminal").toFile().also(directories::add)
}
