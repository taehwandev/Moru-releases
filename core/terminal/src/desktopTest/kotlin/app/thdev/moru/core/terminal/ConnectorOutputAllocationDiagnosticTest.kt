package app.thdev.moru.core.terminal

import com.sun.management.OperatingSystemMXBean
import com.sun.management.ThreadMXBean
import java.io.File
import java.io.IOException
import java.lang.management.ManagementFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertTrue

/** Opt-in matched transport workloads; measures reader allocation, not whole-app retained memory. */
class ConnectorOutputAllocationDiagnosticTest {
    @Test
    fun unconsumedOutputDoesNotAllocateTheSubscribedLineParser() {
        val path = System.getenv("MORU_CONNECTOR_PROBE_OUTPUT")
        assumeTrue(path != null)
        val output = File(requireNotNull(path)).apply { parentFile?.mkdirs() }
        output.writeText("subscribed,allocated_mib,process_cpu_ms,wall_ms,characters,callbacks\n")
        // Warm the actual transport before recording either scenario.
        measure(false, 100)
        val plain = measure(false, 5_000)
        val subscribed = measure(true, 5_000)
        for (sample in listOf(plain, subscribed)) output.appendText(sample.csv() + "\n")
        assertTrue(subscribed.callbacks > 0, "Explicit line consumers must still receive output.")
        assertTrue(plain.allocatedBytes < subscribed.allocatedBytes / 2,
            "An unsubscribed reader should avoid the line parser allocation: $plain versus $subscribed")
    }

    private fun measure(subscribed: Boolean, lines: Int): Sample {
        val startup = "i=0; while [ \"\$i\" -lt $lines ]; do printf '%0160d\\n' \"\$i\"; i=\$((i+1)); done; printf 'MORU_PROBE_DONE\\n'; exit\n"
        var callbacks = 0
        val connector = if (subscribed) createDesktopJediTermTtyConnector(
            shell = "/bin/sh", workingDirectory = System.getProperty("java.io.tmpdir"),
            startupCommand = startup, onOutputLine = { callbacks++ },
        ) else createDesktopJediTermTtyConnector(
            shell = "/bin/sh", workingDirectory = System.getProperty("java.io.tmpdir"), startupCommand = startup,
        )
        val watchdog = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "connector-probe-timeout").apply { isDaemon = true }
        }
        val timeout = watchdog.schedule({ connector.close() }, 15, TimeUnit.SECONDS)
        val allocation = ManagementFactory.getThreadMXBean() as ThreadMXBean
        allocation.isThreadAllocatedMemoryEnabled = true
        val process = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        val threadId = Thread.currentThread().id
        val beforeAllocation = allocation.getThreadAllocatedBytes(threadId)
        val beforeCpu = process.processCpuTime
        val beforeWall = System.nanoTime()
        val buffer = CharArray(8_192)
        var characters = 0L
        var sawMarker = false
        var tail = ""
        try {
            while (true) {
                val count = try { connector.read(buffer, 0, buffer.size) } catch (error: IOException) {
                    if (sawMarker) break else throw error
                }
                if (count < 0) break
                characters += count
                val chunk = String(buffer, 0, count)
                val combined = tail + chunk
                sawMarker = sawMarker || combined.contains("\r\nMORU_PROBE_DONE\r\n")
                tail = combined.takeLast(64)
            }
            assertTrue(sawMarker && characters >= lines * 160L, "The owned PTY must finish the complete flood.")
            return Sample(subscribed, allocation.getThreadAllocatedBytes(threadId) - beforeAllocation,
                process.processCpuTime - beforeCpu, System.nanoTime() - beforeWall, characters, callbacks)
        } finally {
            timeout.cancel(false)
            watchdog.shutdownNow()
            connector.close()
        }
    }

    private data class Sample(
        val subscribed: Boolean, val allocatedBytes: Long, val cpuNanos: Long,
        val wallNanos: Long, val characters: Long, val callbacks: Int,
    ) {
        fun csv(): String = listOf(subscribed, allocatedBytes / 1_048_576.0,
            cpuNanos / 1_000_000.0, wallNanos / 1_000_000.0, characters, callbacks).joinToString(",")
    }
}
