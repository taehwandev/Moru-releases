package app.thdev.moru.core.terminal

import com.jediterm.core.Color
import com.jediterm.terminal.CursorShape
import com.jediterm.terminal.TerminalDisplay
import com.jediterm.terminal.TerminalOutputStream
import com.jediterm.terminal.TtyBasedArrayDataStream
import com.jediterm.terminal.emulator.JediEmulator
import com.jediterm.terminal.emulator.mouse.MouseFormat
import com.jediterm.terminal.emulator.mouse.MouseMode
import com.jediterm.terminal.model.JediTerminal
import com.jediterm.terminal.model.StyleState
import com.jediterm.terminal.model.TerminalSelection
import com.jediterm.terminal.model.TerminalTextBuffer
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Read-only native builtin; no terminal window, model prompt or menu selection. */
suspend fun captureClaudeCliStatus(folder: String, shell: String): String = withContext(Dispatchers.IO) {
    val process = AtomicReference<ProcessHandle?>()
    val connector = createDesktopJediTermTtyConnector(
        shell = shell, workingDirectory = folder, startupCommand = "exec claude",
        columns = 120, rows = 48, hostKey = null,
        onProcessStarted = { process.set(ProcessHandle.of(it).orElse(null)) },
    )
    val style = StyleState()
    val buffer = TerminalTextBuffer(120, 48, style, 200)
    val terminal = JediTerminal(CaptureDisplay, buffer, style)
    terminal.setTerminalOutput(object : TerminalOutputStream {
        override fun sendBytes(bytes: ByteArray, userInput: Boolean) { connector.write(bytes) }
        override fun sendString(text: String, userInput: Boolean) { connector.write(text.toByteArray(Charsets.UTF_8)) }
    })
    val emulator = JediEmulator(TtyBasedArrayDataStream(connector), terminal)
    val pump = thread(name = "moru-cli-report-screen", isDaemon = true) {
        try { while (emulator.hasNext()) emulator.next() } catch (_: java.io.IOException) { /* Owner closes the stream. */ }
    }
    fun rows(): List<String>? {
        if (!buffer.tryLock()) return null
        return try { terminalScreenRows(buffer) } finally { buffer.unlock() }
    }
    try {
        val error = LocalCliCommandDriver("claude", "/status").execute(::rows) { text ->
            if (!connector.isConnected) false else {
                connector.write(text.toByteArray(Charsets.UTF_8)); true
            }
        }
        check(error == null) { "Claude did not provide its status. Open Login to check any startup or account prompt, then retry." }
        buffer.lock()
        try { terminalScreenRows(buffer).joinToString("\n") } finally { buffer.unlock() }
    } finally {
        // Snapshot descendants before closing the PTY, which can reparent children.
        val owned = process.get()?.let { root -> runCatching { root.descendants().use { it.toList() } + root }.getOrDefault(listOf(root)) }.orEmpty()
        runCatching { connector.close() }
        owned.forEach { runCatching { it.destroy() } }
        pump.join(300)
        owned.filter { it.isAlive }.forEach { runCatching { it.destroyForcibly() } }
        runCatching { CompletableFuture.allOf(*owned.map { it.onExit() }.toTypedArray()).get(500, TimeUnit.MILLISECONDS) }
    }
}

private object CaptureDisplay : TerminalDisplay {
    override fun setCursor(x: Int, y: Int) = Unit
    override fun setCursorShape(shape: CursorShape?) = Unit
    override fun beep() = Unit
    override fun scrollArea(top: Int, size: Int, dy: Int) = Unit
    override fun setCursorVisible(visible: Boolean) = Unit
    override fun useAlternateScreenBuffer(use: Boolean) = Unit
    override fun getWindowTitle() = ""
    override fun setWindowTitle(title: String) = Unit
    override fun getSelection(): TerminalSelection? = null
    override fun terminalMouseModeSet(mode: MouseMode) = Unit
    override fun setMouseFormat(format: MouseFormat) = Unit
    override fun ambiguousCharsAreDoubleWidth() = false
    override fun getWindowForeground() = Color(230, 230, 230)
    override fun getWindowBackground() = Color(24, 24, 24)
}
