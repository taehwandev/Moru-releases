package app.thdev.moru.core.terminal

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopPtyTerminalSessionManagerTest {
    @Test
    fun startsPtySessionAndCapturesCommandOutput() {
        val manager = DesktopPtyTerminalSessionManager()
        val session = manager.createSession(
            TerminalSessionSpec(
                title = "PTY Test",
                shell = "zsh",
                workingDirectory = System.getProperty("user.dir"),
            ),
        )

        manager.writeInput(session.id, "printf 'moru-pty-ok\\n'; exit")

        val captured = waitForLine(manager, "moru-pty-ok")

        manager.closeSession(session.id)

        assertTrue(captured, "Expected PTY output to contain moru-pty-ok")
    }

    @Test
    fun rawInputKeepsShellStateForCdCommands() {
        val tempDir = Files.createTempDirectory("moru-pty-cwd").toFile()
        val childDir = tempDir.resolve("child").apply { mkdirs() }
        val manager = DesktopPtyTerminalSessionManager()
        val session = manager.createSession(
            TerminalSessionSpec(
                title = "PTY CWD Test",
                shell = "zsh",
                workingDirectory = childDir.absolutePath,
            ),
        )

        manager.writeRawInput(session.id, "cd ..\rpwd\rexit\r")

        val captured = waitForLine(manager, tempDir.absolutePath)
        val history = manager.listSessions()
            .flatMap { snapshot -> snapshot.commandHistory }
            .map { entry -> entry.command }

        manager.closeSession(session.id)
        tempDir.deleteRecursively()

        assertTrue(captured, "Expected raw terminal input to preserve cd state and print ${tempDir.absolutePath}")
        assertEquals(listOf("cd ..", "pwd", "exit"), history)
    }

    @Test
    fun startupCommandRunsInsidePtySessionForHandoffContext() {
        val manager = DesktopPtyTerminalSessionManager()
        val session = manager.createSession(
            TerminalSessionSpec(
                title = "PTY Handoff Test",
                shell = "zsh",
                workingDirectory = System.getProperty("user.dir"),
                startupCommand = "printf 'moru-handoff-ready\\n'; exit",
            ),
        )

        val captured = waitForLine(manager, "moru-handoff-ready")

        manager.closeSession(session.id)

        assertTrue(captured, "Expected startup command output to be captured from the live PTY")
    }

    @Test
    fun jediTermConnectorCapturesCompletedCommandsFromWrites() {
        val capturedCommands = mutableListOf<String>()
        val connector = createDesktopJediTermTtyConnector(
            shell = "zsh",
            workingDirectory = System.getProperty("user.dir"),
            onCommandCaptured = { command -> capturedCommands += command },
        )

        connector.write("printf 'moru-connector-capture\\n'\r".toByteArray())

        connector.close()

        assertEquals(listOf("printf 'moru-connector-capture\\n'"), capturedCommands)
    }

    @Test
    fun registryModeSpawnsNoProcessAndRunsNoStartupCommand() {
        val marker = Files.createTempDirectory("moru-registry").toFile().resolve("started")
        val manager = DesktopPtyTerminalSessionManager(spawnProcesses = false)

        val session = manager.createSession(
            TerminalSessionSpec(
                workingDirectory = System.getProperty("user.dir"),
                startupCommand = "touch ${marker.absolutePath}",
            ),
        )
        Thread.sleep(500)

        assertEquals(TerminalSessionStatus.STARTING, manager.listSessions().single { it.id == session.id }.status)
        assertFalse(marker.exists(), "A registry-only session must not run its startup command")
        marker.parentFile.deleteRecursively()
    }

    @Test
    fun reportedOutputUpdatesLastOutputButOnlyTheFirstChangesRevision() {
        val manager = DesktopPtyTerminalSessionManager(spawnProcesses = false)
        val session = manager.createSession(TerminalSessionSpec(workingDirectory = System.getProperty("user.dir")))

        manager.reportOutput(session.id, atMillis = 1_000)
        val afterFirst = manager.revision()
        manager.reportOutput(session.id, atMillis = 2_000)

        val snapshot = manager.listSessions().single { it.id == session.id }
        assertEquals(2_000L, snapshot.lastOutputAtMillis)
        assertEquals(TerminalSessionStatus.READY, snapshot.status)
        assertEquals(afterFirst, manager.revision(), "Output reports must not trigger recomposition")
    }

    @Test
    fun reportedExitRecordsStatusAndExitCode() {
        val manager = DesktopPtyTerminalSessionManager(spawnProcesses = false)
        val session = manager.createSession(TerminalSessionSpec(workingDirectory = System.getProperty("user.dir")))

        manager.reportExit(session.id, exitCode = 3)

        val snapshot = manager.listSessions().single { it.id == session.id }
        assertEquals(TerminalSessionStatus.EXITED, snapshot.status)
        assertEquals(3, snapshot.exitCode)
        assertTrue(snapshot.lines.any { line -> line.text.contains("exited with code 3") })
    }

    @Test
    fun jediTermConnectorReportsOutputAndExitCode() {
        var outputChunks = 0
        val exitCodes = java.util.concurrent.LinkedBlockingQueue<Int?>()
        val connector = createDesktopJediTermTtyConnector(
            shell = "zsh",
            workingDirectory = System.getProperty("user.dir"),
            startupCommand = "printf 'moru-exit-report\\n'; exit 7",
            onOutput = { outputChunks += 1 },
            onExit = { code -> exitCodes.add(code) },
        )

        val output = readPtyOutput(connector)
        val reported = exitCodes.poll(10, java.util.concurrent.TimeUnit.SECONDS)
        connector.close()

        assertTrue(output.contains("moru-exit-report"), "Expected the command output, got: $output")
        assertTrue(outputChunks > 0, "Expected onOutput to fire for received output")
        assertEquals(7, reported)
    }

    @Test
    fun desktopPtyRestoresColorCapabilitiesAndPreservesAnsiBytes() {
        val environment = desktopTerminalEnvironment(
            mapOf(
                "NO_COLOR" to "1",
                "TERM" to "dumb",
                "COLORTERM" to "",
                "CLICOLOR" to "0",
            ),
        )
        assertFalse("NO_COLOR" in environment)
        assertEquals("xterm-256color", environment["TERM"])
        assertEquals("truecolor", environment["COLORTERM"])
        assertEquals("1", environment["CLICOLOR"])

        val connector = createDesktopJediTermTtyConnector(
            shell = "bash --noprofile --norc",
            workingDirectory = System.getProperty("user.dir"),
        )
        val expectedAnsi = "\u001B[38;2;17;34;51mmoru-ansi\u001B[0m"

        try {
            connector.write(
                (
                    "printf 'TERM=%s COLORTERM=%s CLICOLOR=%s NO_COLOR=%s\\n' \"\$TERM\" \"\$COLORTERM\" \"\$CLICOLOR\" \"\${NO_COLOR-}\"; " +
                        "printf '\\033[38;2;17;34;51mmoru-ansi\\033[0m\\n'; exit\n"
                    ).toByteArray(),
            )
            val output = readPtyOutput(connector)

            assertTrue(output.contains("TERM=xterm-256color COLORTERM=truecolor CLICOLOR=1 NO_COLOR="))
            assertTrue(output.contains(expectedAnsi), "Expected the PTY connector to preserve ANSI bytes unchanged.")
        } finally {
            connector.close()
        }
    }

    @Test
    fun resizeSessionReachesLivePtyAndRecordsResizeEvidence() {
        val manager = DesktopPtyTerminalSessionManager()
        val session = manager.createSession(
            TerminalSessionSpec(
                title = "PTY Resize Test",
                shell = "zsh",
                workingDirectory = System.getProperty("user.dir"),
            ),
        )

        manager.resizeSession(session.id, columns = 100, rows = 28)

        val resized = manager.listSessions()
            .single { snapshot -> snapshot.id == session.id }
            .lines
            .any { line -> line.text == "resize 100x28" }

        manager.writeInput(session.id, "exit")
        manager.closeSession(session.id)

        assertTrue(resized, "Expected resize evidence after resizing the live PTY")
    }

    @Test
    fun failedShellCreatesFailedSnapshotAndKeepsManagerAlive() {
        val manager = DesktopPtyTerminalSessionManager()
        val session = manager.createSession(
            TerminalSessionSpec(
                title = "Broken PTY Test",
                shell = "/missing/moru-shell",
                workingDirectory = System.getProperty("user.dir"),
            ),
        )

        assertEquals(TerminalSessionStatus.FAILED, session.status)
        assertTrue(session.lines.any { line -> line.text.contains("PTY start failed") })
        assertEquals(session.id, manager.selectedSessionId())

        manager.closeSession(session.id)

        assertTrue(manager.listSessions().isEmpty())
    }

    @Test
    fun gatedRealCommandSmokeRunsRepresentativeTerminalWorkloads() {
        if (System.getenv("MORU_TERMINAL_COMMAND_SMOKE") != "1") {
            return
        }

        val manager = DesktopPtyTerminalSessionManager()
        val workspace = desktopKmpRoot()
        val requireOptionalTools = System.getenv("MORU_TERMINAL_SMOKE_REQUIRE_OPTIONAL_TOOLS") == "1"

        assertPtyCommand(
            manager = manager,
            workspace = workspace,
            title = "vim smoke",
            command = "vim --clean -Nu NONE -n -es +'qall!' >/dev/null 2>&1 && printf 'moru-vim-ok\\n'",
            expected = "moru-vim-ok",
            timeoutMillis = 10_000,
        )
        println("moru-terminal-smoke: vim passed")
        assertOptionalPtyCommand(
            manager = manager,
            workspace = workspace,
            title = "claude smoke",
            executable = "claude",
            command = "claude --version >/dev/null 2>&1 && printf 'moru-claude-ok\\n'",
            expected = "moru-claude-ok",
            requireOptionalTools = requireOptionalTools,
        )
        println("moru-terminal-smoke: claude passed")
        assertOptionalPtyCommand(
            manager = manager,
            workspace = workspace,
            title = "codex smoke",
            executable = "codex",
            command = "codex --version >/dev/null 2>&1 && printf 'moru-codex-ok\\n'",
            expected = "moru-codex-ok",
            requireOptionalTools = requireOptionalTools,
        )
        println("moru-terminal-smoke: codex passed")
        assertPtyCommand(
            manager = manager,
            workspace = workspace,
            title = "gradle smoke",
            command = "MORU_TERMINAL_COMMAND_SMOKE=0 JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-21.jdk/Contents/Home ./gradlew test --quiet && printf 'moru-gradle-test-ok\\n'",
            expected = "moru-gradle-test-ok",
            timeoutMillis = 120_000,
        )
        println("moru-terminal-smoke: ./gradlew test passed")

        val devServer = manager.createSession(
            TerminalSessionSpec(
                title = "dev server smoke",
                shell = "zsh",
                workingDirectory = workspace,
            ),
        )
        try {
            manager.writeInput(devServer.id, "python3 -m http.server 0")
            assertTrue(
                waitForLine(manager, "Serving HTTP", timeoutMillis = 15_000),
                "Expected Python dev server to start inside the live PTY",
            )
            manager.writeRawInput(devServer.id, "\u0003")
            println("moru-terminal-smoke: python dev server passed")
        } finally {
            manager.writeInput(devServer.id, "exit")
            manager.closeSession(devServer.id)
        }
    }

    private fun assertPtyCommand(
        manager: TerminalSessionManager,
        workspace: String,
        title: String,
        command: String,
        expected: String,
        timeoutMillis: Long,
    ) {
        val session = manager.createSession(
            TerminalSessionSpec(
                title = title,
                shell = "zsh",
                workingDirectory = workspace,
            ),
        )
        try {
            manager.writeInput(session.id, "$command; exit")
            assertTrue(
                waitForLine(manager, expected, timeoutMillis = timeoutMillis),
                "Expected `$title` PTY command to produce $expected",
            )
        } finally {
            manager.closeSession(session.id)
        }
    }

    private fun readPtyOutput(connector: com.jediterm.terminal.TtyConnector): String {
        val output = StringBuilder()
        val buffer = CharArray(1_024)
        while (true) {
            val read = connector.read(buffer, 0, buffer.size)
            if (read <= 0) return output.toString()
            output.append(buffer, 0, read)
        }
    }

    private fun assertOptionalPtyCommand(
        manager: TerminalSessionManager,
        workspace: String,
        title: String,
        executable: String,
        command: String,
        expected: String,
        requireOptionalTools: Boolean,
    ) {
        val statusMarker = "moru-$executable-missing"
        val commandWithMarker = "if command -v $executable >/dev/null 2>&1; then $command; else printf '$statusMarker\\n'; fi"
        val session = manager.createSession(
            TerminalSessionSpec(
                title = title,
                shell = "zsh",
                workingDirectory = workspace,
            ),
        )
        try {
            manager.writeInput(session.id, "$commandWithMarker; exit")
            val commandRan = waitForLine(manager, expected, timeoutMillis = 20_000)
            val commandMissing = manager.listSessions()
                .flatMap { snapshot -> snapshot.lines }
                .any { line -> line.text.contains(statusMarker) }

            if (requireOptionalTools) {
                assertTrue(commandRan, "Expected `$executable` to run inside the live PTY")
            } else {
                assertTrue(commandRan || commandMissing, "Expected `$executable` to run or report missing")
            }
        } finally {
            manager.closeSession(session.id)
        }
    }

    private fun desktopKmpRoot(): String {
        return generateSequence(File(System.getProperty("user.dir"))) { file -> file.parentFile }
            .firstOrNull { file -> file.resolve("gradlew").isFile }
            ?.absolutePath
            ?: System.getProperty("user.dir")
    }

    private fun waitForLine(
        manager: TerminalSessionManager,
        expected: String,
        timeoutMillis: Long = 5_000,
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            val matched = manager.listSessions()
                .flatMap { session -> session.lines }
                .any { line -> line.text.contains(expected) }
            if (matched) {
                return true
            }

            Thread.sleep(100)
        }

        return false
    }
}
