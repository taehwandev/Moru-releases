package app.thdev.moru.core.terminal

import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import com.pty4j.WinSize
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

/**
 * Tracks terminal sessions and, by default, owns one PTY per session.
 *
 * With [spawnProcesses] false the manager is a registry only: the visible terminal
 * widget owns the single PTY and reports its output and exit through [reportOutput]
 * and [reportExit]. The app uses that mode, because a second hidden PTY per session
 * would run every startup command twice.
 */
class DesktopPtyTerminalSessionManager(
    initialSessions: List<TerminalSessionSpec> = emptyList(),
    private val charset: Charset = StandardCharsets.UTF_8,
    private val spawnProcesses: Boolean = true,
    /** Whether output answers something Moru just did to the session; see [SessionStimuli]. */
    private val isReaction: (sessionId: String, atMillis: Long) -> Boolean = SessionStimuli::isReaction,
) : TerminalSessionManager {
    private val lock = Any()
    private val sessions = linkedMapOf<String, ManagedPtySession>()
    private val lastOutputAt = ConcurrentHashMap<String, Long>()
    private val printed = ConcurrentHashMap.newKeySet<String>()
    private val startedAt = ConcurrentHashMap<String, Long>()
    private val revision = AtomicLong(0)
    private var selectedId: String? = null
    private var nextIndex = 1

    init {
        initialSessions.forEach { spec -> createSession(spec) }
    }

    override fun revision(): Long = revision.get()

    override fun listSessions(): List<TerminalSessionSnapshot> = synchronized(lock) {
        sessions.values.map { session ->
            session.snapshot.copy(
                lastOutputAtMillis = lastOutputAt[session.snapshot.id],
                startedAtMillis = startedAt[session.snapshot.id],
            )
        }
    }

    override fun reportOutput(sessionId: String, atMillis: Long) {
        // A redraw answering an attach, resize or keystroke leaves the last output time alone; it still proves the
        // process started. Output after submitted input belongs to a new turn, even inside that reaction window.
        if (SessionResponseAttention.outputReceived(sessionId) || !isReaction(sessionId, atMillis)) {
            lastOutputAt[sessionId] = atMillis
        }
        if (printed.add(sessionId)) {
            // First output proves the visible process started; later output must not touch revision.
            updateSession(sessionId) { snapshot ->
                if (snapshot.status == TerminalSessionStatus.STARTING) {
                    snapshot.copy(status = TerminalSessionStatus.READY)
                } else {
                    snapshot
                }
            }
        }
    }

    override fun reportExit(sessionId: String, exitCode: Int?) {
        updateSession(sessionId) { snapshot ->
            snapshot.copy(
                status = TerminalSessionStatus.EXITED,
                exitCode = exitCode,
                lines = (snapshot.lines + TerminalBufferLine(
                    TerminalOutputStream.SYSTEM,
                    "process exited with code ${exitCode ?: "unknown"}",
                )).takeLast(MAX_BUFFER_LINES),
            )
        }
    }

    override fun selectedSessionId(): String? = synchronized(lock) {
        selectedId
    }

    override fun selectSession(sessionId: String) {
        synchronized(lock) {
            if (sessions.containsKey(sessionId)) {
                selectedId = sessionId
                revision.incrementAndGet()
            }
        }
    }

    override fun createTaskTerminalSession(spec: TerminalSessionSpec): TerminalSessionSnapshot = synchronized(lock) {
        // Keep admission and registration atomic, so simultaneous requests cannot both claim the fourth slot.
        TaskTerminalSessionCapacity.requireAvailable(this, spec)
        createSession(spec)
    }

    override fun createSession(requested: TerminalSessionSpec): TerminalSessionSnapshot {
        // A terminal keeps one name in the terminal host for its whole life, across restarts of Moru.
        val spec = when {
            requested.hostKey != null -> requested
            requested.kind == TerminalSessionKind.TERMINAL -> requested.copy(hostKey = "moru-${java.util.UUID.randomUUID()}")
            // A Lab session keeps its conversation under this key instead of a program in the terminal host.
            requested.kind == TerminalSessionKind.LAB -> requested.copy(hostKey = "lab-${java.util.UUID.randomUUID()}")
            else -> requested
        }
        val id = nextSessionId(spec.kind)
        val session = when (spec.kind) {
            TerminalSessionKind.TERMINAL -> startPtySession(id, spec)
            TerminalSessionKind.WEB_PREVIEW -> ManagedPtySession(
                snapshot = TerminalSessionSnapshot(
                    id = id,
                    spec = spec,
                    status = TerminalSessionStatus.READY,
                    lines = listOf(
                        TerminalBufferLine(TerminalOutputStream.SYSTEM, "web preview surface ready"),
                        TerminalBufferLine(TerminalOutputStream.SYSTEM, "url: ${spec.url ?: "about:blank"}"),
                    ),
                ),
            )
            TerminalSessionKind.LAB -> ManagedPtySession(
                snapshot = TerminalSessionSnapshot(id = id, spec = spec, status = TerminalSessionStatus.READY),
            )
        }

        startedAt[id] = System.currentTimeMillis()
        synchronized(lock) {
            sessions[id] = session
            selectedId = id
            revision.incrementAndGet()
        }

        session.process?.let { process ->
            startReaderThread(id, process)
            startWaitThread(id, process)
            spec.startupCommand?.let { commandLine ->
                sendInput(id, commandLine, recordHistory = false)
            }
        }

        return session.snapshot
    }

    override fun moveSessionToTask(sessionId: String, taskId: String): Boolean = synchronized(lock) {
        if (taskId.isBlank()) return@synchronized false
        val session = sessions[sessionId] ?: return@synchronized false
        if (!session.snapshot.spec.kind.isTaskSession || session.snapshot.spec.attachedItemId == taskId) {
            return@synchronized false
        }

        val spec = session.snapshot.spec.copy(attachedItemId = taskId)
        TaskTerminalSessionCapacity.requireAvailable(this, spec)
        sessions[sessionId] = session.copy(snapshot = session.snapshot.copy(spec = spec))
        revision.incrementAndGet()
        true
    }

    override fun closeSession(sessionId: String) {
        val removed = synchronized(lock) {
            val session = sessions.remove(sessionId) ?: return
            lastOutputAt.remove(sessionId)
            printed.remove(sessionId)
            SessionStimuli.forget(sessionId)
            SessionResponseAttention.forget(sessionId)
            startedAt.remove(sessionId)
            if (selectedId == sessionId) {
                selectedId = sessions.keys.firstOrNull()
            }
            revision.incrementAndGet()
            session
        }

        removed.process?.destroy()
    }

    override fun writeRawInput(
        sessionId: String,
        input: String,
    ) {
        val session = synchronized(lock) {
            sessions[sessionId]
        } ?: return

        val process = session.process ?: return
        runCatching {
            process.outputStream.write(input.toByteArray(charset))
            process.outputStream.flush()
        }.onSuccess {
            val commands = session.inputParser.accept(input)
            commands.forEach { command -> recordCommandHistory(sessionId, command) }
        }.onFailure { error ->
            appendLine(sessionId, TerminalOutputStream.STDERR, "write failed: ${error.message.orEmpty()}")
        }
    }

    override fun writeInput(
        sessionId: String,
        input: String,
    ) {
        sendInput(sessionId, input, recordHistory = true)
    }

    private fun sendInput(
        sessionId: String,
        input: String,
        recordHistory: Boolean,
    ) {
        val session = synchronized(lock) {
            sessions[sessionId]
        } ?: return

        val process = session.process ?: return
        runCatching {
            process.outputStream.write(input.toByteArray(charset))
            if (!input.endsWith("\n")) {
                process.outputStream.write('\n'.code)
            }
            process.outputStream.flush()
        }.onSuccess {
            appendLine(sessionId, TerminalOutputStream.INPUT, input.trimEnd('\n'))
            if (recordHistory) {
                recordCommandHistory(sessionId, input)
            }
        }.onFailure { error ->
            appendLine(sessionId, TerminalOutputStream.STDERR, "write failed: ${error.message.orEmpty()}")
        }
    }

    override fun resizeSession(
        sessionId: String,
        columns: Int,
        rows: Int,
    ) {
        val process = synchronized(lock) {
            sessions[sessionId]?.process
        } ?: return

        runCatching {
            process.setWinSize(WinSize(columns, rows))
        }.onSuccess {
            appendLine(sessionId, TerminalOutputStream.SYSTEM, "resize ${columns}x$rows")
        }.onFailure { error ->
            appendLine(sessionId, TerminalOutputStream.STDERR, "resize failed: ${error.message.orEmpty()}")
        }
    }

    override fun recordCommandHistory(sessionId: String, command: String) {
        updateSession(sessionId) { snapshot ->
            snapshot.withRecordedCommand(command)
        }
    }

    override fun recordResumeCommand(sessionId: String, expectedHostKey: String, command: String): Boolean = synchronized(lock) {
        val session = sessions[sessionId] ?: return@synchronized false
        val spec = session.snapshot.spec
        if (spec.kind != TerminalSessionKind.TERMINAL || spec.hostKey != expectedHostKey ||
            command.isBlank() || spec.startupCommand == command) return@synchronized false
        sessions[sessionId] = session.copy(snapshot = session.snapshot.copy(spec = spec.copy(startupCommand = command)))
        revision.incrementAndGet()
        true
    }

    private fun startPtySession(
        id: String,
        spec: TerminalSessionSpec,
    ): ManagedPtySession {
        if (!spawnProcesses) {
            return ManagedPtySession(
                snapshot = TerminalSessionSnapshot(
                    id = id,
                    spec = spec,
                    status = TerminalSessionStatus.STARTING,
                ),
            )
        }
        return try {
            val command = shellCommand(spec.shell)
            val environment = desktopTerminalEnvironment()
            val process = PtyProcessBuilder()
                .setCommand(command)
                .setEnvironment(environment)
                .setDirectory(File(spec.workingDirectory).absolutePath)
                .setInitialColumns(DEFAULT_COLUMNS)
                .setInitialRows(DEFAULT_ROWS)
                .start()

            val snapshot = TerminalSessionSnapshot(
                id = id,
                spec = spec,
                status = TerminalSessionStatus.READY,
                lines = listOf(
                    TerminalBufferLine(TerminalOutputStream.SYSTEM, "PTY ready: ${command.joinToString(" ")}"),
                    TerminalBufferLine(TerminalOutputStream.SYSTEM, "cwd: ${spec.workingDirectory}"),
                ),
            )

            ManagedPtySession(
                snapshot = snapshot,
                process = process,
            )
        } catch (error: IOException) {
            ManagedPtySession(
                snapshot = TerminalSessionSnapshot(
                    id = id,
                    spec = spec,
                    status = TerminalSessionStatus.FAILED,
                    lines = listOf(
                        TerminalBufferLine(TerminalOutputStream.STDERR, "PTY start failed: ${error.message.orEmpty()}"),
                    ),
                ),
            )
        }
    }

    private fun startReaderThread(
        sessionId: String,
        process: PtyProcess,
    ) {
        thread(
            name = "moru-pty-reader-$sessionId",
            isDaemon = true,
        ) {
            val buffer = ByteArray(DEFAULT_READ_BUFFER_SIZE)
            while (true) {
                val read = try {
                    process.inputStream.read(buffer)
                } catch (_: IOException) {
                    break
                }
                if (read < 0) {
                    break
                }
                if (read > 0) {
                    appendTerminalText(sessionId, String(buffer, 0, read, charset))
                }
            }
        }
    }

    private fun startWaitThread(
        sessionId: String,
        process: PtyProcess,
    ) {
        thread(
            name = "moru-pty-wait-$sessionId",
            isDaemon = true,
        ) {
            val exitCode = runCatching { process.waitFor() }.getOrDefault(-1)
            updateSession(sessionId) { snapshot ->
                snapshot.copy(
                    status = TerminalSessionStatus.EXITED,
                    exitCode = exitCode,
                    lines = (snapshot.lines + TerminalBufferLine(
                        TerminalOutputStream.SYSTEM,
                        "process exited with code $exitCode",
                    )).takeLast(MAX_BUFFER_LINES),
                )
            }
        }
    }

    private fun appendTerminalText(
        sessionId: String,
        text: String,
    ) {
        updateSession(sessionId) { snapshot ->
            val parsedOutput = parseTerminalText(
                pendingText = snapshot.pendingText,
                text = stripAnsiControl(text),
            )
            snapshot.copy(
                lines = (snapshot.lines + parsedOutput.completedLines
                    .filter { line -> line.isNotEmpty() }
                    .map { line -> TerminalBufferLine(TerminalOutputStream.STDOUT, line) })
                    .takeLast(MAX_BUFFER_LINES),
                pendingText = parsedOutput.pendingText,
            )
        }
    }

    private fun parseTerminalText(
        pendingText: String,
        text: String,
    ): ParsedTerminalOutput {
        val completedLines = mutableListOf<String>()
        val currentLine = StringBuilder(pendingText)
        var index = 0

        while (index < text.length) {
            val char = text[index]
            when (char) {
                '\r' -> {
                    if (text.getOrNull(index + 1) == '\n') {
                        completedLines += currentLine.toString()
                        currentLine.clear()
                        index += 1
                    } else {
                        currentLine.clear()
                    }
                }
                '\n' -> {
                    completedLines += currentLine.toString()
                    currentLine.clear()
                }
                '\b',
                '\u007F',
                -> {
                    if (currentLine.isNotEmpty()) {
                        currentLine.setLength(currentLine.length - 1)
                    }
                }
                else -> {
                    if (!char.isIsoControlExceptTab()) {
                        currentLine.append(char)
                    }
                }
            }
            index += 1
        }

        return ParsedTerminalOutput(
            completedLines = completedLines,
            pendingText = currentLine.toString(),
        )
    }

    private fun appendLine(
        sessionId: String,
        stream: TerminalOutputStream,
        text: String,
    ) {
        updateSession(sessionId) { snapshot ->
            snapshot.copy(
                lines = (snapshot.lines + TerminalBufferLine(stream, stripAnsiControl(text))).takeLast(MAX_BUFFER_LINES),
            )
        }
    }

    private fun updateSession(
        sessionId: String,
        transform: (TerminalSessionSnapshot) -> TerminalSessionSnapshot,
    ) {
        synchronized(lock) {
            val session = sessions[sessionId] ?: return
            sessions[sessionId] = session.copy(snapshot = transform(session.snapshot))
            revision.incrementAndGet()
        }
    }

    private fun nextSessionId(kind: TerminalSessionKind): String {
        return synchronized(lock) {
            when (kind) {
                TerminalSessionKind.TERMINAL -> "term-${nextIndex++}"
                TerminalSessionKind.WEB_PREVIEW -> "web-${nextIndex++}"
                TerminalSessionKind.LAB -> "lab-${nextIndex++}"
            }
        }
    }

    private fun shellCommand(shell: String): Array<String> {
        val osName = System.getProperty("os.name").lowercase()
        if (osName.contains("win")) {
            return arrayOf("cmd.exe")
        }

        val executable = when {
            shell.startsWith("/") -> shell
            shell == "bash" -> "/bin/bash"
            shell == "zsh" -> "/bin/zsh"
            shell == "sh" -> "/bin/sh"
            else -> shell
        }

        return arrayOf(executable, "-l")
    }

    private fun stripAnsiControl(text: String): String {
        return text.replace(ANSI_CONTROL_PATTERN, "")
    }

    private data class ManagedPtySession(
        val snapshot: TerminalSessionSnapshot,
        val process: PtyProcess? = null,
        val inputParser: TerminalCommandInputParser = TerminalCommandInputParser(),
    )

    private data class ParsedTerminalOutput(
        val completedLines: List<String>,
        val pendingText: String,
    )

    private companion object {
        private const val DEFAULT_READ_BUFFER_SIZE = 4096
        private const val DEFAULT_COLUMNS = 180
        private const val DEFAULT_ROWS = 48
        private const val MAX_BUFFER_LINES = 500
        private val ANSI_CONTROL_PATTERN = Regex("\\u001B\\[[0-?]*[ -/]*[@-~]")
    }
}

private fun Char.isIsoControlExceptTab(): Boolean {
    return isISOControl() && this != '\t'
}
