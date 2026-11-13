package app.thdev.moru.core.terminal

enum class TerminalSessionKind {
    TERMINAL,
    WEB_PREVIEW,

    /** An agent CLI driven in its structured mode and drawn as a conversation, with no terminal (experimental). */
    LAB,
    ;

    /** True for the sessions a Task holds side by side in its tabs: terminals and Lab conversations. */
    val isTaskSession: Boolean get() = this == TERMINAL || this == LAB
}

enum class TerminalSessionStatus {
    STARTING,
    READY,
    EXITED,
    FAILED,
}

enum class TerminalOutputStream {
    INPUT,
    STDOUT,
    STDERR,
    SYSTEM,
}

data class TerminalSessionSpec(
    val title: String? = null,
    val kind: TerminalSessionKind = TerminalSessionKind.TERMINAL,
    val shell: String = "zsh",
    val workingDirectory: String = ".",
    val attachedItemId: String? = null,
    val url: String? = null,
    val startupCommand: String? = null,
    /** The terminal's name in the terminal host, kept across restarts so Moru reattaches to the same running program. */
    val hostKey: String? = null,
)

data class TerminalBufferLine(
    val stream: TerminalOutputStream,
    val text: String,
)

data class TerminalSessionSnapshot(
    val id: String,
    val spec: TerminalSessionSpec,
    val status: TerminalSessionStatus,
    val lines: List<TerminalBufferLine> = emptyList(),
    val pendingText: String = "",
    val commandHistory: List<TerminalCommandHistoryEntry> = emptyList(),
    /** Wall-clock time of the last output the visible terminal received, if any. */
    val lastOutputAtMillis: Long? = null,
    /** Exit code of the session's process once it ended; null while running or unknown. */
    val exitCode: Int? = null,
    /** Wall-clock time this app created the session, so a start that never printed can stop counting as starting. */
    val startedAtMillis: Long? = null,
) {
    val title: String
        get() = spec.title ?: when (spec.kind) {
            TerminalSessionKind.TERMINAL -> spec.attachedItemId ?: spec.shell
            TerminalSessionKind.WEB_PREVIEW -> spec.url ?: "Web Preview"
            TerminalSessionKind.LAB -> spec.attachedItemId ?: "Lab"
        }
}

interface TerminalSessionManager {
    fun revision(): Long
    fun listSessions(): List<TerminalSessionSnapshot>
    fun selectedSessionId(): String?
    fun selectSession(sessionId: String)
    /** Registers generic or restored surfaces, including Tasks with more terminals than a new one admits. */
    fun createSession(spec: TerminalSessionSpec): TerminalSessionSnapshot
    /** Admits a new Task terminal before allocating its id or starting any process. */
    fun createTaskTerminalSession(spec: TerminalSessionSpec): TerminalSessionSnapshot {
        TaskTerminalSessionCapacity.requireAvailable(this, spec)
        return createSession(spec)
    }
    /**
     * Changes only a terminal or Lab session's owning Task, preserving its live state and selection.
     * Returns false for missing sessions, blank Tasks, previews, or unchanged owners; a full Task throws
     * [TaskTerminalSessionCapacity.LimitReached] before any mutation.
     */
    fun moveSessionToTask(sessionId: String, taskId: String): Boolean
    fun closeSession(sessionId: String)
    fun writeRawInput(sessionId: String, input: String)
    fun writeInput(sessionId: String, input: String)
    fun resizeSession(sessionId: String, columns: Int, rows: Int)
    fun recordCommandHistory(sessionId: String, command: String)

    /** Updates restart metadata only; never sends [command] to the live terminal. Stale host keys are rejected. */
    fun recordResumeCommand(sessionId: String, expectedHostKey: String, command: String): Boolean = false

    /**
     * The visible terminal reports that it received output at [atMillis]. This must stay
     * cheap and must not change [revision], because it fires on every output chunk.
     */
    fun reportOutput(sessionId: String, atMillis: Long) {}

    /** The visible terminal reports that its process ended with [exitCode] (null if unknown). */
    fun reportExit(sessionId: String, exitCode: Int?) {}
}

class InMemoryTerminalSessionManager(
    initialSessions: List<TerminalSessionSpec> = emptyList(),
) : TerminalSessionManager {
    private val sessions = mutableListOf<TerminalSessionSnapshot>()
    private val commandParsers = mutableMapOf<String, TerminalCommandInputParser>()
    private var selectedId: String? = null
    private var nextIndex = 1
    private var revision = 0L

    init {
        initialSessions.forEach { spec -> createSession(spec) }
    }

    override fun revision(): Long = revision

    override fun listSessions(): List<TerminalSessionSnapshot> = sessions.toList()

    override fun selectedSessionId(): String? = selectedId

    override fun selectSession(sessionId: String) {
        if (sessions.any { session -> session.id == sessionId }) {
            selectedId = sessionId
            revision += 1
        }
    }

    override fun createSession(spec: TerminalSessionSpec): TerminalSessionSnapshot {
        val id = when (spec.kind) {
            TerminalSessionKind.TERMINAL -> "term-${nextIndex++}"
            TerminalSessionKind.WEB_PREVIEW -> "web-${nextIndex++}"
            TerminalSessionKind.LAB -> "lab-${nextIndex++}"
        }
        val snapshot = TerminalSessionSnapshot(
            id = id,
            spec = spec,
            status = TerminalSessionStatus.READY,
            lines = initialLinesFor(spec),
        )
        sessions.add(snapshot)
        commandParsers[id] = TerminalCommandInputParser()
        selectedId = id
        revision += 1
        return snapshot
    }

    override fun moveSessionToTask(sessionId: String, taskId: String): Boolean {
        if (taskId.isBlank()) return false
        val index = sessions.indexOfFirst { it.id == sessionId }
        val session = sessions.getOrNull(index) ?: return false
        if (!session.spec.kind.isTaskSession || session.spec.attachedItemId == taskId) return false

        val spec = session.spec.copy(attachedItemId = taskId)
        TaskTerminalSessionCapacity.requireAvailable(this, spec)
        sessions[index] = session.copy(spec = spec)
        revision += 1
        return true
    }

    override fun closeSession(sessionId: String) {
        val index = sessions.indexOfFirst { session -> session.id == sessionId }
        if (index < 0) {
            return
        }

        sessions.removeAt(index)
        commandParsers.remove(sessionId)
        SessionResponseAttention.forget(sessionId)
        if (selectedId == sessionId) {
            selectedId = sessions.getOrNull(index)?.id ?: sessions.lastOrNull()?.id
        }
        revision += 1
    }

    override fun writeRawInput(
        sessionId: String,
        input: String,
    ) {
        val completedCommands = commandParsers.getOrPut(sessionId) { TerminalCommandInputParser() }
            .accept(input)
        updateSession(sessionId) { session ->
            completedCommands.fold(
                session.copy(
                    pendingText = session.pendingText + input,
                ),
            ) { updatedSession, command ->
                updatedSession.withRecordedCommand(command)
            }
        }
    }

    override fun writeInput(sessionId: String, input: String) {
        updateSession(sessionId) { session ->
            session.copy(
                lines = session.lines + TerminalBufferLine(TerminalOutputStream.INPUT, input),
                pendingText = "",
            ).withRecordedCommand(input)
        }
    }

    override fun resizeSession(
        sessionId: String,
        columns: Int,
        rows: Int,
    ) {
        updateSession(sessionId) { session ->
            session.copy(
                lines = session.lines + TerminalBufferLine(
                    stream = TerminalOutputStream.SYSTEM,
                    text = "resize ${columns}x$rows",
                ),
            )
        }
    }

    override fun recordCommandHistory(sessionId: String, command: String) {
        updateSession(sessionId) { session ->
            session.withRecordedCommand(command)
        }
    }

    override fun recordResumeCommand(sessionId: String, expectedHostKey: String, command: String): Boolean {
        val index = sessions.indexOfFirst { it.id == sessionId }
        val session = sessions.getOrNull(index) ?: return false
        if (session.spec.kind != TerminalSessionKind.TERMINAL || session.spec.hostKey != expectedHostKey ||
            command.isBlank() || session.spec.startupCommand == command) return false
        sessions[index] = session.copy(spec = session.spec.copy(startupCommand = command))
        revision += 1
        return true
    }

    override fun reportOutput(sessionId: String, atMillis: Long) {
        val index = sessions.indexOfFirst { session -> session.id == sessionId }
        if (index >= 0) {
            sessions[index] = sessions[index].copy(lastOutputAtMillis = atMillis)
        }
    }

    override fun reportExit(sessionId: String, exitCode: Int?) {
        updateSession(sessionId) { session ->
            session.copy(status = TerminalSessionStatus.EXITED, exitCode = exitCode)
        }
    }

    private fun updateSession(
        sessionId: String,
        transform: (TerminalSessionSnapshot) -> TerminalSessionSnapshot,
    ) {
        val index = sessions.indexOfFirst { session -> session.id == sessionId }
        if (index >= 0) {
            sessions[index] = transform(sessions[index])
            revision += 1
        }
    }

    private fun initialLinesFor(spec: TerminalSessionSpec): List<TerminalBufferLine> {
        return when (spec.kind) {
            TerminalSessionKind.TERMINAL -> listOf(
                TerminalBufferLine(TerminalOutputStream.SYSTEM, "session ready: ${spec.shell}"),
                TerminalBufferLine(TerminalOutputStream.SYSTEM, "cwd: ${spec.workingDirectory}"),
            ) + listOfNotNull(
                spec.startupCommand?.let { command ->
                    TerminalBufferLine(TerminalOutputStream.INPUT, command)
                },
            )
            TerminalSessionKind.WEB_PREVIEW -> listOf(
                TerminalBufferLine(TerminalOutputStream.SYSTEM, "web preview surface ready"),
                TerminalBufferLine(TerminalOutputStream.SYSTEM, "url: ${spec.url ?: "about:blank"}"),
            )
            TerminalSessionKind.LAB -> emptyList()
        }
    }
}
