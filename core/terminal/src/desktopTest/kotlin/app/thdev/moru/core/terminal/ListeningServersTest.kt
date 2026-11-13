package app.thdev.moru.core.terminal

import java.net.ServerSocket
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListeningServersTest {
    private fun freePort(): Int = ServerSocket(0).use { it.localPort }

    private fun waitUntil(what: String, timeoutMillis: Long = 15_000, check: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (System.nanoTime() < deadline) {
            if (check()) return
            Thread.sleep(100)
        }
        assertTrue(false, "timed out waiting for $what")
    }

    @Test
    fun aServerRunningUnderATerminalIsFoundByItsPortEvenThoughNothingPrintedIt() {
        val port = freePort()
        val sessionId = "listening-${System.nanoTime()}"
        // The program prints nothing about its address, like an agent that starts a server in the background.
        val connector = createDesktopJediTermTtyConnector(
            shell = "/bin/sh",
            workingDirectory = System.getProperty("java.io.tmpdir"),
            startupCommand = "python3 -c \"from http.server import HTTPServer, SimpleHTTPRequestHandler; " +
                "HTTPServer(('127.0.0.1', $port), SimpleHTTPRequestHandler).serve_forever()\"",
            onProcessStarted = { pid -> SessionShellProcesses.record(sessionId, pid) },
        )
        val reader = thread(isDaemon = true) {
            val buffer = CharArray(1024)
            while (connector.isConnected) if (connector.read(buffer, 0, buffer.size) < 0) break
        }
        try {
            waitUntil("the server to be found") {
                ListeningServers.refresh()
                ListeningServers.urlFor(sessionId) == "http://localhost:$port/"
            }
        } finally {
            connector.close()
            reader.join(2_000)
            ListeningServers.refresh()
            SessionShellProcesses.forget(sessionId)
            ListeningServers.forget(sessionId)
        }
    }

    @Test
    fun aSessionWithNoServerHasNoAddressAndAnUnreadableListingChangesNothing() {
        val sessionId = "quiet-${System.nanoTime()}"
        SessionShellProcesses.record(sessionId, ProcessHandle.current().pid())

        ListeningServers.refresh(listening = { "" })
        assertNull(ListeningServers.urlFor(sessionId))

        // The test JVM listens on nothing here, but if a reading is unavailable what was known stays.
        val port = 54321
        ListeningServers.refresh(listening = { "p${ProcessHandle.current().pid()}\nn127.0.0.1:$port\n" })
        // This process is the shell itself, not a child of it, so it does not count as running under the terminal.
        assertNull(ListeningServers.urlFor(sessionId))
        ListeningServers.refresh(listening = { null })
        assertNull(ListeningServers.urlFor(sessionId))

        SessionShellProcesses.forget(sessionId)
    }

    @Test
    fun theNewestProcessUnderTheShellWinsAndItsLowestPortIsUsed() {
        val sessionId = "fake-${System.nanoTime()}"
        val shell = ProcessBuilder("/bin/sh", "-c", "sleep 30 & sleep 30 & wait").start()
        try {
            SessionShellProcesses.record(sessionId, shell.pid())
            waitUntil("children to exist") { ProcessHandle.of(shell.pid()).get().children().count() >= 2 }
            val children = ProcessHandle.of(shell.pid()).get().children().map { it.pid() }.toList().sorted()
            val older = children.first()
            val newer = children.last()

            ListeningServers.refresh(listening = { "p$older\nn*:3000\np$newer\nn*:4100\nn*:4000\n" }, respondsToHttp = { true })

            assertEquals("http://localhost:4000/", ListeningServers.urlFor(sessionId))
            assertEquals(setOf(3000, 4000, 4100), ListeningServers.portsFor(sessionId))
            // The newer process's page port closed and it opened an internal one.
            ListeningServers.refresh(listening = { "p$older\nn*:3000\np$newer\nn*:4200\n" }, respondsToHttp = { it == 3000 })
            assertEquals("http://localhost:3000/", ListeningServers.urlFor(sessionId))
            ListeningServers.refresh(listening = { "p$newer\nn*:4200\n" }, respondsToHttp = { false })
            assertNull(ListeningServers.urlFor(sessionId), "only internal sockets remain; clear the stale preview")
            assertEquals(emptySet(), ListeningServers.portsFor(sessionId))
        } finally {
            shell.destroyForcibly()
            SessionShellProcesses.forget(sessionId)
            ListeningServers.forget(sessionId)
        }
    }

    @Test
    fun aListenerIsAskedForItsPageOnceNotOnEveryScan() {
        val sessionId = "probe-once-${System.nanoTime()}"
        val shell = ProcessBuilder("/bin/sh", "-c", "sleep 30 & sleep 30 & wait").start()
        try {
            SessionShellProcesses.record(sessionId, shell.pid())
            waitUntil("children to exist") { ProcessHandle.of(shell.pid()).get().children().count() >= 2 }
            val (server, other) = ProcessHandle.of(shell.pid()).get().children().map { it.pid() }.toList().sorted()
            val asked = mutableListOf<Int>()

            // A dev server that renders its home page for every `GET /` (KeyFlow read Firestore each time).
            repeat(10) {
                ListeningServers.refresh(listening = { "p$server\nn*:3100\n" }, respondsToHttp = { port -> asked += port; true })
            }
            assertEquals(listOf(3100), asked, "one request for the listener, however many scans see it")
            assertEquals("http://localhost:3100/", ListeningServers.urlFor(sessionId))

            // Another process on the same port is a new listener and is asked again.
            asked.clear()
            repeat(3) {
                ListeningServers.refresh(listening = { "p$other\nn*:3100\n" }, respondsToHttp = { port -> asked += port; true })
            }
            assertEquals(listOf(3100), asked)
        } finally {
            shell.destroyForcibly()
            SessionShellProcesses.forget(sessionId)
            ListeningServers.forget(sessionId)
        }
    }

    @Test
    fun aListenerThatIsNotAPageIsAskedAFewTimesSoAStartingServerIsStillFound() {
        val sessionId = "probe-start-${System.nanoTime()}"
        val shell = ProcessBuilder("/bin/sh", "-c", "sleep 30 & sleep 30 & wait").start()
        try {
            SessionShellProcesses.record(sessionId, shell.pid())
            waitUntil("children to exist") { ProcessHandle.of(shell.pid()).get().children().count() >= 2 }
            val (starting, internal) = ProcessHandle.of(shell.pid()).get().children().map { it.pid() }.toList().sorted()
            val asked = mutableListOf<Int>()
            val listing = "p$starting\nn*:5173\np$internal\nn*:9229\n"
            var ready = false

            ListeningServers.refresh(listening = { listing }, respondsToHttp = { port -> asked += port; ready && port == 5173 })
            assertNull(ListeningServers.urlFor(sessionId), "the dev server has not answered with a page yet")
            ready = true
            repeat(10) {
                ListeningServers.refresh(listening = { listing }, respondsToHttp = { port -> asked += port; ready && port == 5173 })
            }

            assertEquals("http://localhost:5173/", ListeningServers.urlFor(sessionId))
            assertEquals(2, asked.count { it == 5173 }, "found on the second ask, then never asked again")
            assertEquals(3, asked.count { it == 9229 }, "a socket that never serves a page is left alone after three asks")
        } finally {
            shell.destroyForcibly()
            SessionShellProcesses.forget(sessionId)
            ListeningServers.forget(sessionId)
        }
    }

    @Test
    fun whenNoTerminalRunsAProgramTheListingIsNotReadAndOldAddressesGo() {
        // A shell sitting at its prompt: a live process with no children.
        val idleShell = ProcessBuilder("sleep", "30").start()
        val sessionId = "idle-${System.nanoTime()}"
        val others = SessionShellProcesses.snapshot()
        others.keys.forEach(SessionShellProcesses::forget)
        try {
            SessionShellProcesses.record(sessionId, idleShell.pid())
            var listed = 0
            ListeningServers.refresh(listening = { listed += 1; "" })
            assertEquals(0, listed, "lsof must not run while no terminal runs a program")
            assertNull(ListeningServers.urlFor(sessionId))
        } finally {
            idleShell.destroyForcibly()
            SessionShellProcesses.forget(sessionId)
            others.forEach { (id, pid) -> SessionShellProcesses.record(id, pid) }
        }
    }
}
