package app.thdev.moru.feature.workbench.launch

import app.thdev.moru.core.terminal.createDesktopJediTermTtyConnector
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.Assume.assumeTrue

/** Opt-in installed-CLI startup only: no task prompt, model request, or recorded terminal contents. */
class CodexInteractiveStartupSmokeTest {
    @Test
    fun freshCodexStartsInMorusRealPtyWithoutTheSharedServerError() {
        assertInteractiveStartup(agentLaunchCommand(AgentRuntime.Codex, "")!!)
    }

    @Test
    fun codexResumeInANewFolderStartsFreshWithoutTheSharedServerError() {
        assertInteractiveStartup(AgentConversation.resumeCommand(AgentRuntime.Codex, null)!!)
    }

    private fun assertInteractiveStartup(command: String) {
        assumeTrue(System.getenv("MORU_CODEX_STARTUP_SMOKE") == "1")
        val folder = Files.createTempDirectory("moru-codex-startup-")
        // Trust this empty test folder for this invocation only; never persist a trust decision in user settings.
        val trustOption = "-c 'projects.\"$folder\".trust_level=\"trusted\"'"
        val startupCommand = command.replace("codex --no-daemon", "codex --no-daemon $trustOption")
        val finished = CountDownLatch(1)
        val ready = AtomicBoolean(false)
        val serverError = AtomicBoolean(false)
        val connector = createDesktopJediTermTtyConnector(
            shell = "/bin/zsh",
            workingDirectory = folder.toString(),
            startupCommand = startupCommand,
            columns = 120,
            rows = 40,
        )
        val reader = thread(isDaemon = true) {
            val buffer = CharArray(4096)
            var recent = ""
            try {
                while (connector.isConnected && finished.count > 0) {
                    val count = connector.read(buffer, 0, buffer.size)
                    if (count < 0) break
                    recent = (recent + String(buffer, 0, count)).takeLast(32_768)
                    // Respond as a terminal emulator would; never print or save the received text.
                    if (recent.contains("\u001b[6n")) {
                        connector.write("\u001b[1;1R")
                        recent = recent.replace("\u001b[6n", "")
                    }
                    if (recent.contains("\u001b]10;?")) {
                        connector.write("\u001b]10;rgb:dddd/dddd/dddd\u001b\\")
                        recent = recent.replace("\u001b]10;?", "")
                    }
                    if (recent.contains("\u001b]11;?")) {
                        connector.write("\u001b]11;rgb:2020/2020/2020\u001b\\")
                        recent = recent.replace("\u001b]11;?", "")
                    }
                    if (recent.contains("Cannot use the background server") || recent.contains("Experimental feature request failed")) {
                        serverError.set(true)
                        finished.countDown()
                    } else if (recent.contains("Codex") && (recent.contains("for shortcuts") || recent.contains("/model"))) {
                        ready.set(true)
                        finished.countDown()
                    }
                }
            } finally {
                finished.countDown()
            }
        }
        try {
            val completed = finished.await(30, TimeUnit.SECONDS)
            assertTrue(completed && ready.get() && !serverError.get(),
                "Installed Codex startup: completed=$completed, ready=${ready.get()}, sharedServerError=${serverError.get()}")
        } finally {
            connector.write("\u0003\u0003")
            connector.close()
            reader.join(2_000)
            folder.toFile().deleteRecursively()
        }
    }
}
