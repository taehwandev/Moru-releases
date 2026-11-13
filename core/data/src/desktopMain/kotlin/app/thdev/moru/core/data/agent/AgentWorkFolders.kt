package app.thdev.moru.core.data.agent

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READONLY
import androidx.sqlite.driver.bundled.SQLITE_OPEN_URI
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The folders the agents running in one terminal last worked in, read from each agent CLI's own conversation record.
 * They stay put between commands, so they name the worktree an agent moved into even while nothing runs there.
 *
 * - Claude writes `~/.claude/sessions/<pid>.json` naming its conversation, whose record notes the folder on every line
 *   and the folders its tool calls reach.
 * - Codex holds its record (`~/.codex/sessions/…/rollout-*.jsonl`) open; its commands name their folders. It also
 *   holds its subagents' records open (the guardian that reviews commands in auto mode), which say nothing about where
 *   the user's thread works, so only a record whose first line names a subagent source is passed over.
 * - AGY holds its own log open, which names its conversation; the conversation's database notes each command's folder.
 *
 * Every read is a file the agent wrote; nothing here asks an agent or changes its files. Call it off the UI thread.
 */
class AgentWorkFolders(private val home: File = File(System.getProperty("user.home"))) {
    private val agyLogs = ConcurrentHashMap<String, AgyLogPosition>()
    private val agySteps = ConcurrentHashMap<String, AgyStepReading>()
    private val codexSubagents = ConcurrentHashMap<String, Boolean>()

    /** The folders named by the agents among [pids], whose processes hold [openFiles], the most recent of each first. */
    fun read(pids: List<Long>, openFiles: List<String>): List<String> =
        (pids.flatMap(::claudeFolders) + codexFolders(openFiles) + agyFolders(openFiles)).distinct()

    private fun claudeFolders(pid: Long): List<String> {
        val session = File(home, ".claude/sessions/$pid.json").takeIf(File::isFile) ?: return emptyList()
        val fields = runCatching { Json.parseToJsonElement(session.readText()) as? JsonObject }.getOrNull() ?: return emptyList()
        val id = (fields["sessionId"] as? JsonPrimitive)?.content?.takeIf(String::isNotBlank) ?: return emptyList()
        val started = (fields["cwd"] as? JsonPrimitive)?.content
        val projects = File(home, ".claude/projects")
        val record = started?.let { File(projects, "${claudeProjectName(it)}/$id.jsonl") }?.takeIf(File::isFile)
            ?: projects.listFiles()?.asSequence()?.map { folder -> File(folder, "$id.jsonl") }?.firstOrNull(File::isFile)
            ?: return emptyList()
        return claudeRecordFolders(tail(record))
    }

    private fun codexFolders(openFiles: List<String>): List<String> {
        val records = openFiles.filter { path ->
            "/.codex/sessions/" in path && path.substringAfterLast('/').let { it.startsWith("rollout-") && it.endsWith(".jsonl") }
        }
        // A record's first line says whose it is and never changes, so each is read once.
        val own = records.filterNot { path -> codexSubagents.getOrPut(path) { codexSubagentRecord(File(path)) } }
        return own.ifEmpty { records }.map(::File)
            .maxByOrNull(File::lastModified)
            ?.let { record -> codexRecordFolders(tail(record)) }
            .orEmpty()
    }

    /** Whether the record's first line, its session_meta, names a subagent as its source. */
    private fun codexSubagentRecord(record: File): Boolean = runCatching {
        RandomAccessFile(record, "r").use { file ->
            val bytes = ByteArray(minOf(file.length(), FirstLineBytes).toInt())
            file.readFully(bytes)
            String(bytes, Charsets.UTF_8).substringBefore('\n').contains(CodexSubagentSource)
        }
    }.getOrDefault(false)

    private fun agyFolders(openFiles: List<String>): List<String> =
        openFiles.filter { path -> "/.gemini/antigravity-cli/log/" in path && path.endsWith(".log") }
            .mapNotNull(::agyConversation)
            .distinct()
            .flatMap { id -> agyLastStepFolders(File(home, ".gemini/antigravity-cli/conversations/$id.db")) }

    /** The log only grows, so each read scans what was added since the last one. */
    private fun agyConversation(logPath: String): String? {
        val log = File(logPath)
        val known = agyLogs[logPath]?.takeIf { position -> position.offset <= log.length() } ?: AgyLogPosition(0, null)
        if (known.offset == log.length()) return known.conversation
        val added = runCatching {
            RandomAccessFile(log, "r").use { file ->
                file.seek(known.offset)
                val bytes = ByteArray((file.length() - known.offset).toInt())
                file.readFully(bytes)
                bytes
            }
        }.getOrNull() ?: return known.conversation
        // A line cut off at the end is read again next time, whole.
        val whole = added.lastIndexOf('\n'.code.toByte()) + 1
        val conversation = agyLogConversation(String(added, 0, whole, Charsets.UTF_8)) ?: known.conversation
        agyLogs[logPath] = AgyLogPosition(known.offset + whole, conversation)
        return conversation
    }

    /** The database is read again only when it or its write-ahead log changed. */
    private fun agyLastStepFolders(database: File): List<String> {
        if (!database.isFile) return emptyList()
        val wal = File(database.path + "-wal")
        val stamp = "${database.lastModified()}:${database.length()}:${wal.lastModified()}:${wal.length()}"
        agySteps[database.path]?.takeIf { reading -> reading.stamp == stamp }?.let { reading -> return reading.folders }
        val folders = runCatching { lastCommandStep(database, wal.isFile)?.let(::agyStepFolders) }.getOrNull().orEmpty()
        agySteps[database.path] = AgyStepReading(stamp, folders)
        return folders
    }

    // A read-only open cannot create the shared-memory file a write-ahead log needs, so a database AGY is not writing to
    // is opened as unchanging instead; while it writes, the log and its shared memory are there to read through.
    private fun lastCommandStep(database: File, writing: Boolean): String? {
        val connection = if (writing) {
            BundledSQLiteDriver().open(database.path, SQLITE_OPEN_READONLY)
        } else {
            BundledSQLiteDriver().open("file:${database.toURI().rawPath}?immutable=1", SQLITE_OPEN_READONLY or SQLITE_OPEN_URI)
        }
        return connection.use {
            it.prepare(LastCommandStep).use { statement ->
                if (statement.step()) String(statement.getBlob(0), Charsets.UTF_8) else null
            }
        }
    }

    private fun tail(file: File): String = runCatching {
        RandomAccessFile(file, "r").use { record ->
            val start = (record.length() - TailBytes).coerceAtLeast(0)
            record.seek(start)
            val bytes = ByteArray((record.length() - start).toInt())
            record.readFully(bytes)
            val text = String(bytes, Charsets.UTF_8)
            if (start > 0) text.substringAfter('\n') else text
        }
    }.getOrDefault("")

    private data class AgyLogPosition(val offset: Long, val conversation: String?)

    private data class AgyStepReading(val stamp: String, val folders: List<String>)

    private companion object {
        const val TailBytes = 256L * 1024
        const val FirstLineBytes = 64L * 1024
        val CodexSubagentSource = Regex("\"source\"\\s*:\\s*\\{\\s*\"subagent\"")
        const val LastCommandStep =
            "SELECT step_payload FROM steps WHERE instr(step_payload, CAST('\"Cwd\":\"' AS BLOB)) > 0 ORDER BY idx DESC LIMIT 1"
    }
}

/** Claude keeps a folder's conversations under the folder's path with every character but letters and digits as `-`. */
internal fun claudeProjectName(folder: String): String = folder.replace(Regex("[^A-Za-z0-9]"), "-")
