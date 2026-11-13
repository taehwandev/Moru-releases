package app.thdev.moru.core.data.agent

import java.io.File
import java.io.RandomAccessFile

/*
 * Where the agent CLIs keep their transcripts and how their ends are read, shared by [agentTranscriptReply] and
 * [agentTranscriptThread].
 */

internal fun claudeTranscript(home: File, id: String): File? =
    File(home, ".claude/projects").listFiles()?.asSequence()?.map { File(it, "$id.jsonl") }?.firstOrNull(File::isFile)

private val codexPaths = java.util.concurrent.ConcurrentHashMap<String, File>()

internal fun codexTranscript(home: File, id: String): File? {
    codexPaths[id]?.takeIf(File::isFile)?.let { return it }
    val sessions = File(home, ".codex/sessions").takeIf(File::isDirectory) ?: return null
    // Newest days first: a running conversation was written to recently.
    val found = sessions.walkTopDown().maxDepth(4)
        .filter { it.isFile && it.name.startsWith("rollout-") && it.name.endsWith("-$id.jsonl") }
        .maxByOrNull(File::lastModified) ?: return null
    codexPaths[id] = found
    return found
}

internal fun tailLines(file: File): List<String> = runCatching {
    RandomAccessFile(file, "r").use { log ->
        val bytes = ByteArray(minOf(log.length(), MaxTranscriptBytes).toInt())
        log.seek(log.length() - bytes.size)
        log.readFully(bytes)
        // The first line may begin mid-record; parsing drops it.
        String(bytes, Charsets.UTF_8).lines()
    }
}.getOrDefault(emptyList())

private const val MaxTranscriptBytes = 2L * 1024 * 1024
