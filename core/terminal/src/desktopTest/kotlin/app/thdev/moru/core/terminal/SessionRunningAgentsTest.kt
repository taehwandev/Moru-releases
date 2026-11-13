package app.thdev.moru.core.terminal

import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionRunningAgentsTest {
    @Test
    fun agentsAreKnownByTheirExecutableOrTheScriptAnInterpreterRuns() {
        assertEquals("claude", runningAgentProgram("/opt/homebrew/Caskroom/claude-code@latest/2.1.296/claude", listOf("--resume")))
        assertEquals("codex", runningAgentProgram("/Users/me/.codex/packages/standalone/current/bin/codex", emptyList()))
        assertEquals("agy", runningAgentProgram("/Users/me/.local/bin/agy-real", listOf("--dangerously-skip-permissions")))
        assertEquals("codex", runningAgentProgram("/usr/local/bin/node", listOf("/usr/local/lib/node_modules/@openai/codex/bin/codex.js")))
        assertEquals("claude", runningAgentProgram("/usr/local/bin/node", listOf("--no-warnings", "/usr/local/lib/node_modules/@anthropic-ai/claude-code/cli.js")))
        assertEquals("agy", runningAgentProgram("/bin/bash", listOf("/Users/me/.local/bin/agy")))
    }

    @Test
    fun otherProgramsAndShellsAtTheirPromptAreNoAgent() {
        assertNull(runningAgentProgram("/bin/zsh", listOf("-l")))
        assertNull(runningAgentProgram("/bin/sh", listOf("-c", "npm run dev")))
        assertNull(runningAgentProgram("/usr/local/bin/node", listOf("server.js")))
        assertNull(runningAgentProgram("/usr/bin/vim", listOf("codex.md")))
    }

    @Test
    fun theAgentNearestTheShellIsTheOneTheTerminalShowsAndItGoesWhenItEnds() {
        val bin = Files.createTempDirectory("running-agents").toFile()
        // A Claude that runs Codex as a tool is still Claude to the user.
        script(bin, "codex", "sleep 30")
        val parentId = "b35bd238-3395-4334-b0d3-702ebbaee98a"
        val childId = "01a15cdd-1811-7021-8f06-bee1874ef4c9"
        script(bin, "claude", "\"${bin.absolutePath}/codex\" resume $childId &\nsleep 2\nexec \"${bin.absolutePath}/codex\" resume $childId")
        val sessionId = "running-agent-${System.nanoTime()}"
        val connector = createDesktopJediTermTtyConnector(
            shell = "/bin/sh",
            workingDirectory = System.getProperty("java.io.tmpdir"),
            startupCommand = "\"${bin.absolutePath}/claude\" --session-id $parentId",
            onProcessStarted = { pid -> SessionShellProcesses.record(sessionId, pid) },
        )
        val reader = thread(isDaemon = true) {
            val buffer = CharArray(1024)
            while (connector.isConnected) if (connector.read(buffer, 0, buffer.size) < 0) break
        }
        try {
            waitUntil("Claude to be found") {
                SessionRunningAgents.refresh()
                val process = SessionRunningAgents.readProcess(sessionId)
                SessionRunningAgents.agentFor(sessionId) == "claude" && process?.program == "claude" &&
                    parentId in process.arguments && childId !in process.arguments
            }
            // Codex replaces Claude as the program the shell runs.
            waitUntil("Codex to take over") {
                SessionRunningAgents.refresh()
                val process = SessionRunningAgents.readProcess(sessionId)
                SessionRunningAgents.agentFor(sessionId) == "codex" && process?.program == "codex" && childId in process.arguments
            }
        } finally {
            SessionShellProcesses.stopRunningProgram(sessionId)
            connector.close()
            reader.join(2_000)
            SessionShellProcesses.forget(sessionId)
            SessionRunningAgents.refresh()
            bin.deleteRecursively()
        }
        assertNull(SessionRunningAgents.agentFor(sessionId))
        assertNull(SessionRunningAgents.readProcess(sessionId))
    }

    private fun script(dir: File, name: String, body: String) {
        File(dir, name).apply { writeText("#!/bin/sh\n$body\n") }.setExecutable(true)
    }

    private fun waitUntil(what: String, timeoutMillis: Long = 15_000, check: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (System.nanoTime() < deadline) {
            if (check()) return
            Thread.sleep(100)
        }
        assertTrue(false, "timed out waiting for $what")
    }
}
