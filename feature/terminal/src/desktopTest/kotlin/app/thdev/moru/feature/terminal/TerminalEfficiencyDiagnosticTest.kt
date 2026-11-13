package app.thdev.moru.feature.terminal

import com.sun.management.OperatingSystemMXBean
import java.awt.GraphicsEnvironment
import java.io.File
import java.lang.management.ManagementFactory
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertTrue

/** Opt-in native terminal fixture measurements; this does not benchmark the packaged app or agent inference. */
class TerminalEfficiencyDiagnosticTest {
    @Test
    fun measureOneAndFivePtysAtIdleAndDuringMetadataRedraws() {
        val target = System.getenv("MORU_EFFICIENCY_PROBE_OUTPUT")
        assumeTrue(target != null)
        assumeFalse(GraphicsEnvironment.isHeadless())
        val output = File(requireNotNull(target))
        output.parentFile?.mkdirs()
        output.writeText("phase,registered_sessions,live_sessions,visible_sessions,elapsed_seconds,cpu_seconds,cpu_percent_one_core,rss_mib,heap_used_mib,heap_committed_mib,revision_reads\n")
        TerminalEfficiencyWindow().use { fixture ->
            fixture.waitForSession(fixture.first)
            Thread.sleep(2_000)
            sample(output, "one_idle", fixture) { Thread.sleep(2_000) }
            sample(output, "one_metadata_redraw", fixture) { redraw(fixture) }
            repeat(4) {
                val created = fixture.addSession()
                fixture.manager.selectSession(created)
                requestTerminalFocus(created)
                fixture.waitUntil("started diagnostic PTY $created") { JediTermSessionRegistry.find(created) != null }
            }
            Thread.sleep(2_000)
            assertTrue(fixture.liveSessions() == 5, "Every diagnostic PTY must have started")
            sample(output, "five_idle", fixture) { Thread.sleep(2_000) }
            sample(output, "five_metadata_redraw", fixture) { redraw(fixture) }
        }
    }

    private fun redraw(fixture: TerminalEfficiencyWindow) {
        repeat(10) {
            fixture.redrawMetadata()
            Thread.sleep(200)
        }
    }

    private fun sample(output: File, phase: String, fixture: TerminalEfficiencyWindow, workload: () -> Unit) {
        val operatingSystem = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        val revisionBefore = fixture.revisionReads.get()
        val cpuBefore = operatingSystem.processCpuTime
        val started = System.nanoTime()
        workload()
        val elapsed = (System.nanoTime() - started) / 1_000_000_000.0
        val cpuSeconds = (operatingSystem.processCpuTime - cpuBefore) / 1_000_000_000.0
        val runtime = Runtime.getRuntime()
        output.appendText(listOf(
            phase, fixture.manager.listSessions().size, fixture.liveSessions(), fixture.visibleSessions(), elapsed, cpuSeconds,
            cpuSeconds / elapsed * 100, rssMiB(),
            (runtime.totalMemory() - runtime.freeMemory()) / 1048576.0,
            runtime.totalMemory() / 1048576.0, fixture.revisionReads.get() - revisionBefore,
        ).joinToString(",") + "\n")
    }

    private fun rssMiB(): Double {
        val process = ProcessBuilder("/bin/ps", "-o", "rss=", "-p", ProcessHandle.current().pid().toString()).start()
        return try {
            process.inputStream.bufferedReader().readText().trim().toDouble() / 1024.0
        } finally {
            process.waitFor()
            process.destroy()
        }
    }
}
