package app.thdev.moru.core.data.agent

import app.thdev.moru.core.model.AgentConversationRef
import java.io.File
import java.io.RandomAccessFile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The conversation the agent running among [pids] holds, read from the files the agent CLI itself keeps, so a closed
 * terminal can reopen that conversation even when its startup command never named one (Codex and AGY choose their own
 * ids, and an agent the user typed into a shell has no startup command):
 *
 * - Claude writes `~/.claude/sessions/<pid>.json` with its `sessionId`.
 * - Codex names it on its command line (`codex resume <id>`), holds its record
 *   `~/.codex/sessions/…/rollout-<time>-<id>.jsonl` open, or, from 0.162, leaves that to a shared daemon; then
 *   only a unique user thread in the terminal's folder updated since that `codex` started can be inferred.
 * - AGY holds its log open, which names its conversation once one started.
 *
 * [program] is the agent running there; when its conversation cannot be read the answer still names it with no id,
 * without borrowing a different agent's conversation. [openFiles] and [workingFolders] are
 * what the processes hold and work in. Nothing here asks an agent anything or changes its files. Call it off the UI
 * thread; [codexStartedAt] gives a running `codex` process's start, read from the system by default.
 * Automatic checkpoints should use [exactRunningAgentConversation] to avoid folder-based inference.
 */
fun runningAgentConversation(
    pids: List<Long>,
    openFiles: List<String>,
    program: String? = null,
    workingFolders: List<String> = emptyList(),
    home: File = File(System.getProperty("user.home")),
    codexStartedAt: (pid: Long) -> CodexProcess? = ::codexProcess,
): AgentConversationRef? = readRunningAgentConversation(
    pids, openFiles, program, workingFolders, home, codexStartedAt, exactOnly = false,
)

internal fun readRunningAgentConversation(
    pids: List<Long>,
    openFiles: List<String>,
    program: String?,
    workingFolders: List<String>,
    home: File,
    codexStartedAt: (pid: Long) -> CodexProcess?,
    exactOnly: Boolean,
    namedConversation: String? = null,
): AgentConversationRef? {
    val readers = mapOf(
        "claude" to { conversationFromCandidates(pids, exactOnly, namedConversation) { pid -> claudeConversation(home, pid) } },
        "codex" to { codexConversation(home, pids, openFiles, workingFolders, codexStartedAt, inferThread = !exactOnly) },
        "agy" to { agyConversation(openFiles, exactOnly, namedConversation) },
    )
    if (program != null) {
        val reader = readers[program] ?: return null
        return AgentConversationRef(program, reader())
    }
    return readers.entries.firstNotNullOfOrNull { (agent, reader) -> reader()?.let { id -> AgentConversationRef(agent, id) } }
}

private fun codexConversation(
    home: File,
    pids: List<Long>,
    openFiles: List<String>,
    folders: List<String>,
    started: (Long) -> CodexProcess?,
    inferThread: Boolean,
): String? {
    val opened = openFiles.filter { path -> "/.codex/sessions/" in path }
        .mapNotNull { path -> CodexRecord.matchEntire(path.substringAfterLast('/'))?.groupValues?.get(1) }
        .distinct()
    if (opened.isNotEmpty()) return opened.singleOrNull()
    val processes = pids.mapNotNull(started)
    val named = processes.mapNotNull(CodexProcess::namedThread).distinct()
    if (named.isNotEmpty()) return named.singleOrNull()?.takeIf { id -> processes.all { it.namedThread == id } }
    if (!inferThread) return null
    val codex = processes.singleOrNull() ?: return null
    val database = codexStateDatabase(home) ?: return null
    // A little slack: Codex may stamp the thread a moment before the process clock reads its own start.
    return codexThreadSince(database, folders, codex.startedAtMillis - StartSlackMillis)
}

internal fun codexProcess(pid: Long): CodexProcess? {
    val info = ProcessHandle.of(pid).orElse(null)?.info() ?: return null
    val command = info.command().orElse("").substringAfterLast('/')
    val arguments = info.arguments().map { it.toList() }.orElse(emptyList())
    val isCodex = command == "codex" || arguments.firstOrNull { !it.startsWith("-") }?.substringAfterLast('/')?.let {
        it == "codex" || it == "codex.js"
    } == true
    if (!isCodex) return null
    val started = info.startInstant().orElse(null)?.toEpochMilli() ?: return null
    val named = arguments.indexOf("resume").takeIf { it >= 0 }?.let { arguments.getOrNull(it + 1) }?.takeIf { Uuid.matches(it) }
    return CodexProcess(started, named)
}

private fun claudeConversation(home: File, pid: Long): String? {
    val session = File(home, ".claude/sessions/$pid.json").takeIf(File::isFile) ?: return null
    val fields = runCatching { Json.parseToJsonElement(session.readText()) as? JsonObject }.getOrNull() ?: return null
    return (fields["sessionId"] as? JsonPrimitive)?.content?.takeIf(String::isNotBlank)
}

private fun agyConversation(openFiles: List<String>, exactOnly: Boolean, namedConversation: String?): String? {
    val logs = openFiles.filter { path -> "/.gemini/antigravity-cli/log/" in path && path.endsWith(".log") }
        .map(::File)
        .sortedByDescending(File::lastModified)
    return conversationFromCandidates(logs, exactOnly, namedConversation) { log -> agyLogConversation(tail(log)) }
}

private fun <T> conversationFromCandidates(candidates: List<T>, exactOnly: Boolean, fallback: String? = null, read: (T) -> String?): String? =
    if (exactOnly) {
        val identities = candidates.asSequence().mapNotNull(read).distinct().take(2).toList()
        if (identities.isEmpty()) fallback else identities.singleOrNull()
    } else {
        candidates.firstNotNullOfOrNull(read)
    }

/** The end of a log, where the agent last named the conversation it works on. */
private fun tail(file: File): String = runCatching {
    RandomAccessFile(file, "r").use { log ->
        val bytes = ByteArray(minOf(log.length(), MaxLogBytes).toInt())
        log.seek(log.length() - bytes.size)
        log.readFully(bytes)
        String(bytes, Charsets.UTF_8)
    }
}.getOrDefault("")

private val Uuid = Regex("""[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}""")
private const val StartSlackMillis = 5_000L
private val CodexRecord = Regex("""rollout-.*-([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\.jsonl""")
private const val MaxLogBytes = 4L * 1024 * 1024
