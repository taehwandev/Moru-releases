package app.thdev.moru.core.terminal

import app.thdev.moru.core.terminal.host.TerminalHostLauncher
import com.jediterm.core.util.TermSize
import com.jediterm.terminal.ProcessTtyConnector
import com.jediterm.terminal.TtyConnector
import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import com.pty4j.WinSize
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

fun createDesktopJediTermTtyConnector(
    shell: String,
    workingDirectory: String,
    startupCommand: String? = null,
    onCommandCaptured: (String) -> Unit = {},
    columns: Int = DefaultJediTermColumns,
    rows: Int = DefaultJediTermRows,
    charset: Charset = StandardCharsets.UTF_8,
    onOutput: () -> Unit = {},
    onExit: (Int?) -> Unit = {},
    onLocalServerUrl: (String) -> Unit = {},
    onOutputLine: (String) -> Unit = IgnoreOutputLine,
    onProcessStarted: (pid: Long) -> Unit = {},
    /** Called whenever Moru prods the program (attach, resize, input), so its redraw is not taken for work. */
    onStimulus: () -> Unit = {},
    /** Names the terminal in the terminal host so it outlives Moru; null runs it inside Moru, ending with it. */
    hostKey: String? = null,
    /** Observes the exact background reply sent to the program; terminal bytes stay unchanged. */
    onBackgroundColorRead: (rgb: Int) -> Unit = {},
    /** Called before an Enter outside a bracketed paste reaches the program. */
    onInputSubmitted: () -> Unit = {},
): TtyConnector {
    val command = desktopTerminalShellCommand(shell)
    if (hostKey != null) {
        // Reattaching makes the host replay the screen, and the program usually redraws too.
        onStimulus()
        val hosted = TerminalHostLauncher.open(hostKey, command, File(workingDirectory).absolutePath, startupCommand, columns, rows, onExit, charset)
        if (hosted != null) {
            onProcessStarted(hosted.pid)
            return HostedTtyConnector(
                hosted, charset, onCommandCaptured, onOutput, onLocalServerUrl,
                onOutputLine.takeUnless { it === IgnoreOutputLine }, onStimulus, onBackgroundColorRead, onInputSubmitted,
            )
        }
    }
    val process = PtyProcessBuilder()
        .setCommand(command.toTypedArray())
        .setEnvironment(desktopTerminalEnvironment())
        .setDirectory(File(workingDirectory).absolutePath)
        .setInitialColumns(columns)
        .setInitialRows(rows)
        .start()

    onProcessStarted(process.pid())

    startupCommand?.takeIf { command -> command.isNotBlank() }?.let { command ->
        val commandLine = if (command.endsWith("\n")) command else "$command\n"
        process.outputStream.write(commandLine.toByteArray(charset))
        process.outputStream.flush()
    }

    thread(name = "moru-jediterm-exit-watch", isDaemon = true) {
        onExit(runCatching { process.waitFor() }.getOrNull())
    }

    return DesktopPtyTtyConnector(
        process = process,
        charset = charset,
        commandLine = command,
        displayName = command.joinToString(" "),
        onCommandCaptured = onCommandCaptured,
        onOutput = onOutput,
        onLocalServerUrl = onLocalServerUrl,
        onOutputLine = onOutputLine,
        onStimulus = onStimulus,
        onBackgroundColorRead = onBackgroundColorRead,
        onInputSubmitted = onInputSubmitted,
    )
}

private class DesktopPtyTtyConnector(
    private val process: PtyProcess,
    private val charset: Charset,
    commandLine: List<String>,
    private val displayName: String,
    private val onCommandCaptured: (String) -> Unit,
    private val onOutput: () -> Unit,
    private val onLocalServerUrl: (String) -> Unit,
    private val onOutputLine: (String) -> Unit,
    private val onStimulus: () -> Unit,
    private val onBackgroundColorRead: (rgb: Int) -> Unit,
    private val onInputSubmitted: () -> Unit,
) : ProcessTtyConnector(process, charset, commandLine), ThemeReportingTtyConnector {
    private val inputParser = TerminalCommandInputParser()
    private val urlDetector = LocalServerUrlDetector()
    // The embedded terminal observes the rendered screen. Parse raw lines only for explicit subscribers.
    private val lineTracker = if (onOutputLine === IgnoreOutputLine) null else LastOutputLineTracker()
    private var reportedLine: String? = null
    private val themeReport = ThemeChangeReport()

    override fun read(buf: CharArray, offset: Int, length: Int): Int {
        val read = super.read(buf, offset, length)
        if (read > 0) {
            onOutput()
            val text = String(buf, offset, read)
            themeReport.accept(text)
            urlDetector.accept(text)?.let(onLocalServerUrl)
            lineTracker?.accept(text)?.let { line ->
                if (line != reportedLine) {
                    reportedLine = line
                    onOutputLine(line)
                }
            }
        }
        return read
    }

    override fun getName(): String {
        return displayName
    }

    override fun isConnected(): Boolean {
        return process.isAlive
    }

    override fun resize(termSize: TermSize) {
        onStimulus()
        process.setWinSize(WinSize(termSize.columns, termSize.rows))
    }

    override fun write(bytes: ByteArray) {
        onStimulus()
        val text = String(bytes, charset)
        val commands = inputParser.accept(text, onInputSubmitted)
        super.write(bytes)
        terminalBackgroundColorReply(text)?.let(onBackgroundColorRead)
        commands.forEach(onCommandCaptured)
    }

    override fun reportThemeChange(dark: Boolean) {
        val sequence = themeReport.sequenceFor(dark) ?: return
        onStimulus()
        super.write(sequence.toByteArray(charset))
    }

    override fun close() {
        super.close()
        if (process.isAlive) {
            process.destroy()
        }
    }
}

fun desktopTerminalShellCommand(shell: String): List<String> {
    val osName = System.getProperty("os.name").lowercase()
    if (osName.contains("win")) {
        return listOf("cmd.exe")
    }

    val tokens = shell.trim()
        .takeIf { value -> value.isNotEmpty() }
        ?.split(Regex("\\s+"))
        ?: listOf("zsh")
    val executableToken = tokens.first()
    val executable = when {
        executableToken.startsWith("/") -> executableToken
        executableToken == "bash" -> "/bin/bash"
        executableToken == "zsh" -> "/bin/zsh"
        executableToken == "sh" -> "/bin/sh"
        else -> executableToken
    }
    val arguments = tokens.drop(1)

    return if (arguments.isEmpty()) {
        listOf(executable, "-l")
    } else {
        listOf(executable) + arguments
    }
}

private const val DefaultJediTermColumns = 120
private const val DefaultJediTermRows = 36
private val IgnoreOutputLine: (String) -> Unit = {}
