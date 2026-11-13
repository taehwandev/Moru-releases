package app.thdev.moru.feature.terminal

import com.jediterm.terminal.ui.JediTermWidget
import java.io.File
import java.lang.management.ManagementFactory
import java.lang.ref.WeakReference
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.swing.SwingUtilities
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Opt-in diagnostic of the real terminal component, not a packaged-app or model-inference benchmark. */
class TerminalMemoryDiagnosticTest {
    @Test
    fun oneAndFiveFloodedPtysStayBoundedAndReleaseTheirWidgets() {
        assumeTrue(System.getenv("MORU_MEMORY_PROBE_MODE") == "pty")
        measure("pty", "i=0; while [ \"\$i\" -lt 20000 ]; do printf '%0160d\\n' \"\$i\"; i=\$((i+1)); done; printf done > flood-PROBE_INDEX.done; sleep 120")
    }

    @Test
    fun oneAndFiveClaudeClisStartWithoutAPromptAndCloseWithTheirPtys() {
        assumeTrue(System.getenv("MORU_MEMORY_PROBE_MODE") == "claude")
        val binary = File(System.getenv("MORU_MEMORY_PROBE_CLAUDE") ?: "/opt/homebrew/bin/claude")
        assertTrue(binary.canExecute(), "Set MORU_MEMORY_PROBE_CLAUDE to an installed Claude CLI")
        val quoted = "'" + binary.absolutePath.replace("'", "'\\''") + "'"
        measure("claude", "exec $quoted --safe-mode --no-chrome")
    }

    private fun measure(mode: String, startup: String) {
        val output = File(requireNotNull(System.getenv("MORU_MEMORY_PROBE_OUTPUT")))
        output.parentFile.mkdirs()
        output.writeText("mode,phase,sessions,rss_mib,heap_used_mib,heap_committed_mib,threads,child_rss_mib,child_count,output_events,history_lines\n")
        val folder = Files.createTempDirectory("moru-memory-probe").toFile()
        val keys = mutableListOf<String>()
        val references = mutableListOf<WeakReference<JediTermSession>>()
        val widgets = mutableListOf<WeakReference<JediTermWidget>>()
        val roots = mutableListOf<ProcessHandle>()
        val children = mutableSetOf<ProcessHandle>()
        val events = AtomicLong()
        try {
            sample(output, mode, "baseline", keys, roots, events)
            repeat(5) { index ->
                val key = "memory-probe-${UUID.randomUUID()}"
                keys += key
                startDesktopTerminalSession(
                    key, folder.absolutePath, "/bin/sh", "printf '%s\\n' \$\$ > probe-$index.pid; ${startup.replace("PROBE_INDEX", index.toString())}", {}, null, 13, 5000,
                    onOutput = { events.incrementAndGet() },
                )
                references += WeakReference(requireNotNull(JediTermSessionRegistry.find(key)).session)
                widgets += WeakReference(requireNotNull(JediTermSessionRegistry.find(key)?.session?.component() as? JediTermWidget))
                val pidFile = folder.resolve("probe-$index.pid")
                val deadline = System.nanoTime() + 5_000_000_000L
                while ((!pidFile.isFile || pidFile.length() == 0L) && System.nanoTime() < deadline) Thread.sleep(20)
                val root = ProcessHandle.of(pidFile.readText().trim().toLong()).orElseThrow()
                assertTrue(root.parent().orElseThrow().pid() == ProcessHandle.current().pid(), "The probe owns only its PTY root")
                roots += root
                if (index == 0 || index == 4) {
                    Thread.sleep(if (mode == "claude") 15_000 else 8_000)
                    if (mode == "pty") {
                        val floodDeadline = System.nanoTime() + 30_000_000_000L
                        val markers = (0..index).map { folder.resolve("flood-$it.done") }
                        while (markers.any { !it.isFile } && System.nanoTime() < floodDeadline) Thread.sleep(50)
                        assertTrue(markers.all { it.isFile }, "Every probe must complete its 20,000-line flood")
                    }
                    roots.forEach { owned -> owned.descendants().use { children += it.toList() } }
                    sample(output, mode, if (index == 0) "one" else "five", keys, roots, events)
                }
            }
            assertTrue(events.get() > 0, "Real PTYs must produce output")
            if (mode == "claude") {
                assertTrue(roots.size == 5 && roots.all { it.isAlive && it.info().command().orElse("").contains("claude") },
                    "Five probe CLI processes must remain live; no prompt is sent")
            }
            SwingUtilities.invokeAndWait { keys.forEach(JediTermSessionRegistry::release) }
            keys.forEach { assertNull(JediTermSessionRegistry.find(it)) }
            Thread.sleep(2_000)
            sample(output, mode, "closed", emptyList(), roots, events)
            // A bounded diagnostic retry allows queued Swing teardown to finish. GC is never used in production.
            val collectionDeadline = System.nanoTime() + 5_000_000_000L
            while ((references.any { it.get() != null } || widgets.any { it.get() != null }) && System.nanoTime() < collectionDeadline) {
                SwingUtilities.invokeAndWait {}
                System.gc()
                Thread.sleep(200)
            }
            sample(output, mode, "closed_gc_diagnostic", emptyList(), roots, events)
            assertTrue(references.all { it.get() == null } && widgets.all { it.get() == null },
                "Closed sessions and their Swing widgets must no longer be retained")
        } finally {
            SwingUtilities.invokeAndWait { keys.forEach(JediTermSessionRegistry::release) }
            (children + roots).filter { it.isAlive }.forEach { it.destroyForcibly() }
            folder.deleteRecursively()
        }
    }

    private fun sample(output: File, mode: String, phase: String, keys: List<String>, roots: List<ProcessHandle>, events: AtomicLong) {
        var history = 0
        SwingUtilities.invokeAndWait {
            keys.forEach { key ->
                val component = assertNotNull(JediTermSessionRegistry.find(key)?.session?.component() as? JediTermWidget)
                val lines = component.terminalTextBuffer.historyLinesCount
                assertTrue(lines <= 5000, "Configured scrollback bound must survive output flooding")
                if (mode == "pty") assertEquals(5000, lines, "Flooded terminal must fill its bounded history")
                history += lines
            }
        }
        val runtime = Runtime.getRuntime()
        val children = roots.flatMap { root -> listOf(root) + root.descendants().use { it.toList() } }.distinctBy { it.pid() }.filter { it.isAlive }
        val childRss = children.sumOf { rssMiB(it.pid()) }
        val row = listOf(
            mode, phase, keys.size, rssMiB(ProcessHandle.current().pid()),
            (runtime.totalMemory() - runtime.freeMemory()) / 1048576.0, runtime.totalMemory() / 1048576.0,
            ManagementFactory.getThreadMXBean().threadCount, childRss, children.size, events.get(), history,
        ).joinToString(",")
        output.appendText(row + "\n")
    }

    private fun rssMiB(pid: Long): Double {
        val process = ProcessBuilder("/bin/ps", "-o", "rss=", "-p", pid.toString()).start()
        return try {
            process.inputStream.bufferedReader().readText().trim().toDoubleOrNull()?.div(1024.0) ?: 0.0
        } finally {
            process.waitFor()
            process.destroy()
        }
    }
}
