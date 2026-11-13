package app.thdev.moru.core.terminal.host

import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** A terminal in the host keeps running after Moru lets go of it, shows its output again on return, and ends on request. */
class TerminalHostTest {
    private fun readUntil(connector: HostTtyConnector, needle: String, timeoutMillis: Long = 10_000): String {
        val seen = StringBuilder()
        val buffer = CharArray(4096)
        val reader = thread(isDaemon = true) {
            while (!seen.contains(needle)) {
                val count = runCatching { connector.read(buffer, 0, buffer.size) }.getOrDefault(-1)
                if (count < 0) break
                synchronized(seen) { seen.append(buffer, 0, count) }
            }
        }
        reader.join(timeoutMillis)
        return synchronized(seen) { seen.toString() }
    }

    /** Runs a host in this test and waits until it listens, so nothing ever starts a detached one in its place. */
    private fun serveHost(socket: java.nio.file.Path, idleExitMillis: Long = 60_000): Thread {
        TerminalHostLauncher.configureSocketForTest(socket)
        val served = thread(isDaemon = true) { TerminalHost(socket, idleExitMillis = idleExitMillis).run() }
        val deadline = System.currentTimeMillis() + 5_000
        while (TerminalHostLauncher.heldKeys() == null && System.currentTimeMillis() < deadline) Thread.sleep(20)
        return served
    }

    @Test
    fun aProgramMoruNeverAttachedToEndsByItsKeyAndAnUnknownKeyStartsNothing() {
        val folder = Files.createTempDirectory("moru-host").toFile()
        val socket = folder.resolve("run/h.sock").toPath()
        serveHost(socket)
        try {
            // An earlier run left a program; this run never shows it, as for a Task deleted before it was opened.
            val left = assertNotNull(TerminalHostLauncher.open("orphan", listOf("/bin/sh"), folder.absolutePath, "", 80, 24, {}))
            val pid = left.pid
            left.close()
            assertEquals(setOf("orphan"), TerminalHostLauncher.heldKeys())

            assertTrue(TerminalHostLauncher.terminate("orphan"))
            val deadline = System.currentTimeMillis() + 5_000
            while (ProcessHandle.of(pid).map { it.isAlive }.orElse(false) && System.currentTimeMillis() < deadline) Thread.sleep(50)
            assertFalse(ProcessHandle.of(pid).map { it.isAlive }.orElse(false), "the orphaned shell must end")

            assertFalse(TerminalHostLauncher.terminate("never-opened"))
            Thread.sleep(300)
            assertFalse("never-opened" in TerminalHostLauncher.heldKeys().orEmpty(), "ending an unknown key must not start it")
        } finally {
            TerminalHostLauncher.terminateAll()
            folder.deleteRecursively()
        }
    }

    @Test
    fun aTerminalSurvivesMoruLettingGoAndEndsWhenAskedTo() {
        val folder = Files.createTempDirectory("moru-host").toFile()
        val socket = folder.resolve("run/h.sock").toPath()
        val served = serveHost(socket, idleExitMillis = 2_000)
        try {
            val exits = CopyOnWriteArrayList<Int?>()
            val shell = listOf("/bin/sh")
            val first = assertNotNull(
                TerminalHostLauncher.open("term-a", shell, folder.absolutePath, "echo started-$$; PS1='> '", 80, 24, { exits += it }),
            )
            assertTrue(first.created)
            val pid = first.pid
            assertTrue(readUntil(first, "started-").contains("started-"))

            // Moru quits: it only lets go.
            first.close()
            Thread.sleep(300)
            assertTrue(ProcessHandle.of(pid).map { it.isAlive }.orElse(false), "the shell must keep running")
            assertEquals(emptyList(), exits)

            // Moru comes back: same program, its earlier output first, and it still answers.
            val second = assertNotNull(TerminalHostLauncher.open("term-a", shell, folder.absolutePath, "echo started-again", 80, 24, { exits += it }))
            assertFalse(second.created)
            assertEquals(pid, second.pid)
            second.write("echo back-$((40+2))\n")
            val shown = readUntil(second, "back-42")
            assertTrue(shown.contains("started-"), "replay missing: $shown")
            assertTrue(shown.contains("back-42"))
            assertFalse(shown.contains("started-again"), "a reattach must not run the startup command again")

            // The user closes the terminal: the program ends and Moru hears it.
            second.terminate()
            val deadline = System.currentTimeMillis() + 5_000
            while (ProcessHandle.of(pid).map { it.isAlive }.orElse(false) && System.currentTimeMillis() < deadline) Thread.sleep(50)
            assertFalse(ProcessHandle.of(pid).map { it.isAlive }.orElse(false), "the shell must end")

            // With nothing left, the host goes away by itself.
            served.join(TimeUnit.SECONDS.toMillis(20))
            assertFalse(served.isAlive, "an empty host must exit")
        } finally {
            TerminalHostLauncher.terminateAll()
            folder.deleteRecursively()
        }
    }

    @Test
    fun aProgramThatEndsWhileMoruIsAwayShowsItsLastOutputAndExitCode() {
        val folder = Files.createTempDirectory("moru-host").toFile()
        val socket = folder.resolve("run/h.sock").toPath()
        serveHost(socket)
        try {
            val first = assertNotNull(TerminalHostLauncher.open("term-b", listOf("/bin/sh"), folder.absolutePath, "sleep 0.5; echo finished; exit 3", 80, 24, {}))
            first.close()
            Thread.sleep(1_500)
            val exits = CopyOnWriteArrayList<Int?>()
            val again = assertNotNull(TerminalHostLauncher.open("term-b", listOf("/bin/sh"), folder.absolutePath, "", 80, 24, { exits += it }))
            assertTrue(readUntil(again, "finished").contains("finished"))
            // The terminal view keeps reading until the end; the exit arrives at the end of the output.
            val rest = CharArray(4096)
            while (again.read(rest, 0, rest.size) >= 0) Unit
            assertEquals(3, again.waitFor())
            assertEquals(listOf<Int?>(3), exits)
        } finally {
            TerminalHostLauncher.terminateAll()
            folder.deleteRecursively()
        }
    }

    @Test
    fun theHostNamesTheTerminalsItHoldsWithoutStartingOne() {
        val folder = Files.createTempDirectory("moru-host").toFile()
        val socket = folder.resolve("run/h.sock").toPath()
        try {
            TerminalHostLauncher.configureSocketForTest(socket)
            assertEquals(null, TerminalHostLauncher.heldKeys(), "no host runs, and asking must not start one")
            thread(isDaemon = true) { TerminalHost(socket, idleExitMillis = 60_000).run() }
            val deadline = System.currentTimeMillis() + 5_000
            while (TerminalHostLauncher.heldKeys() == null && System.currentTimeMillis() < deadline) Thread.sleep(50)
            assertEquals(emptySet(), TerminalHostLauncher.heldKeys())
            val held = assertNotNull(TerminalHostLauncher.open("term-c", listOf("/bin/sh"), folder.absolutePath, "", 80, 24, {}))
            held.close()
            assertEquals(setOf("term-c"), TerminalHostLauncher.heldKeys())
            held.terminate()
        } finally {
            TerminalHostLauncher.terminateAll()
            folder.deleteRecursively()
        }
    }
}
