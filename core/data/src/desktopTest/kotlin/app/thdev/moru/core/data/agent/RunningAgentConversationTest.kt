package app.thdev.moru.core.data.agent

import app.thdev.moru.core.model.AgentConversationRef
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RunningAgentConversationTest {
    private val home = Files.createTempDirectory("agent-home").toFile()

    @Test
    fun claudeIsReadFromItsProcessSessionFile() {
        File(home, ".claude/sessions").mkdirs()
        File(home, ".claude/sessions/42.json").writeText("""{"pid":42,"sessionId":"fbe9ba5d-9cd7-419f-8e3c-59e307690e15","cwd":"/p"}""")

        assertEquals(
            AgentConversationRef("claude", "fbe9ba5d-9cd7-419f-8e3c-59e307690e15"),
            runningAgentConversation(listOf(7, 42), emptyList(), home = home),
        )
    }

    @Test
    fun codexIsReadFromTheExactRecordItHoldsOpen() {
        val folder = File(home, ".codex/sessions/2026/10/11").apply { mkdirs() }
        val record = File(folder, "rollout-2026-10-11T20-37-09-01a12ac0-a220-7763-989c-9bcc2e0367aa.jsonl").apply { writeText("{}") }

        assertEquals(
            AgentConversationRef("codex", "01a12ac0-a220-7763-989c-9bcc2e0367aa"),
            runningAgentConversation(listOf(9), listOf("/tmp/${record.name}", record.path, record.path), home = home) { null },
        )
    }

    @Test
    fun competingOpenCodexRecordsAreNotAttributedByModificationTimeOrDatabaseFallback() {
        val folder = File(home, ".codex/sessions/2026/10/11").apply { mkdirs() }
        val older = File(folder, "rollout-2026-10-11T19-52-54-01a12a98-1d74-7502-bf5c-17cde0ba6ea5.jsonl").apply { writeText("{}"); setLastModified(1_000) }
        val newer = File(folder, "rollout-2026-10-11T20-37-09-01a12ac0-a220-7763-989c-9bcc2e0367aa.jsonl").apply { writeText("{}"); setLastModified(2_000) }
        codexThreads(Triple("database-thread", "/p", 9_000L))

        assertEquals(
            AgentConversationRef("codex", null),
            runningAgentConversation(listOf(9), listOf(older.path, newer.path), "codex", listOf("/p"), home) {
                CodexProcess(startedAtMillis = 8_000, namedThread = null)
            },
        )
    }

    @Test
    fun agyIsReadFromTheLastConversationItsLogNames() {
        val log = File(home, ".gemini/antigravity-cli/log/agy.log").apply { parentFile.mkdirs() }
        log.writeText("Created conversation 05446238-6cf7-4b4d-9876-894e6a2b70f0\nStreaming conversation 15446238-6cf7-4b4d-9876-894e6a2b70f1\n")

        assertEquals(
            AgentConversationRef("agy", "15446238-6cf7-4b4d-9876-894e6a2b70f1"),
            runningAgentConversation(listOf(9), listOf(log.path), home = home),
        )
    }

    @Test
    fun theAgentTheTerminalRunsIsPreferredAndNothingRunningIsNull() {
        File(home, ".claude/sessions").mkdirs()
        File(home, ".claude/sessions/42.json").writeText("""{"sessionId":"c1"}""")
        val codex = File(home, ".codex/sessions/rollout-2026-10-11T20-37-09-01a12ac0-a220-7763-989c-9bcc2e0367aa.jsonl").apply { parentFile.mkdirs(); writeText("{}") }

        assertEquals("codex", runningAgentConversation(listOf(42), listOf(codex.path), program = "codex", home = home)?.program)
        assertEquals("claude", runningAgentConversation(listOf(42), listOf(codex.path), home = home)?.program)
        assertNull(runningAgentConversation(listOf(1), listOf("/tmp/x"), home = home))
    }

    @Test
    fun aKnownProviderWithoutAnIdentityNeverBorrowsAnotherProvidersConversation() {
        File(home, ".claude/sessions").mkdirs()
        File(home, ".claude/sessions/42.json").writeText("""{"sessionId":"claude-session"}""")
        val codex = File(home, ".codex/sessions/rollout-2026-10-11T20-37-09-01a12ac0-a220-7763-989c-9bcc2e0367aa.jsonl").apply { parentFile.mkdirs(); writeText("{}") }

        assertEquals(
            AgentConversationRef("codex", null),
            runningAgentConversation(listOf(42), emptyList(), "codex", home = home) { null },
        )
        assertEquals(
            AgentConversationRef("claude", null),
            runningAgentConversation(listOf(9), listOf(codex.path), "claude", home = home) { null },
        )
        assertEquals(
            AgentConversationRef("agy", null),
            runningAgentConversation(listOf(42), listOf(codex.path), "agy", home = home) { null },
        )
        assertNull(runningAgentConversation(listOf(42), listOf(codex.path), "unknown", home = home) { null })
    }

    @Test
    fun codexWhoseDaemonHoldsTheRecordIsFoundInItsThreadListByFolderAndStart() {
        codexThreads(
            Triple("old", "/p", 1_000L),
            Triple("mine", "/p", 9_000L),
            Triple("elsewhere", "/q", 9_500L),
        )
        val started = { pid: Long -> if (pid == 9L) CodexProcess(startedAtMillis = 8_000, namedThread = null) else null }

        assertEquals(
            AgentConversationRef("codex", "mine"),
            runningAgentConversation(listOf(1, 9), emptyList(), "codex", listOf("/p"), home, started),
        )
    }

    @Test
    fun exactCheckpointDoesNotInferEvenAUniqueCodexDatabaseThread() {
        codexThreads(Triple("another-terminal", "/p", 9_000L))
        val started = { _: Long -> CodexProcess(startedAtMillis = 8_000, namedThread = null) }

        assertEquals(
            AgentConversationRef("codex", "another-terminal"),
            runningAgentConversation(listOf(9), emptyList(), "codex", listOf("/p"), home, started),
        )
        assertEquals(
            AgentConversationRef("codex", null),
            exactRunningAgentConversation(listOf(9), emptyList(), "codex", home, started),
        )
        assertNull(exactRunningAgentConversation(listOf(9), emptyList(), home = home, codexStartedAt = started))
    }

    @Test
    fun exactCheckpointCapturesTheCodexThreadNamedByItsProcess() {
        assertEquals(
            AgentConversationRef("codex", "named"),
            exactRunningAgentConversation(listOf(9), emptyList(), "codex", home) {
                CodexProcess(startedAtMillis = 8_000, namedThread = "named")
            },
        )
    }

    @Test
    fun exactOuterProcessReadsNativeIdentityFlagsBeforeThePrompt() {
        val id = "01a15cdd-1811-7021-8f06-bee1874ef4c9"
        val invocations = listOf(
            "codex" to listOf("--no-daemon", "exec", "resume", "--skip-git-repo-check", id, "sleep then reply"),
            "codex" to listOf("/opt/codex/bin/codex.js", "-c", "model=example", "resume", id),
            "claude" to listOf("--session-id", id, "--permission-mode", "acceptEdits", "--print", "reply"),
            "claude" to listOf("--resume=$id", "--print", "reply"),
            "agy" to listOf("--dangerously-skip-permissions", "--conversation", id, "--mode", "accept-edits", "--print", "reply"),
        )
        invocations.forEach { (program, arguments) ->
            assertEquals(
                AgentConversationRef(program, id),
                exactRunningAgentConversation(9, emptyList(), program, arguments, home),
                "native identity for $program",
            )
        }
    }

    @Test
    fun exactOuterProcessRejectsPromptTextSelectorsAndCompetingNativeIds() {
        val first = "01a15cdd-1811-7021-8f06-bee1874ef4c9"
        val second = "14bacb82-bdbf-4c4a-af3f-6e0293cf12b4"
        val invocations = listOf(
            "codex" to listOf("exec", "please resume", first),
            "codex" to listOf("exec", "reply", "resume", first),
            "codex" to listOf("resume", "--last"),
            "codex" to listOf("resume", first, "--last"),
            "codex" to listOf("--unknown-prompt", "resume", first),
            "agy" to listOf("--print", "reply", "--conversation", first),
            "agy" to listOf("--conversation", first, "--conversation", second),
            "agy" to listOf("--conversation", "../unsafe"),
            "agy" to listOf("--conversation", first, "--continue"),
            "claude" to listOf("--session-id", first, "--resume", second),
            "claude" to listOf("--session-id", first, "--fork-session"),
            "claude" to listOf("--system-prompt", "--resume", first),
        )
        invocations.forEach { (program, arguments) ->
            assertEquals(AgentConversationRef(program, null), exactRunningAgentConversation(9, emptyList(), program, arguments, home))
        }
    }

    @Test
    fun exactOuterProcessStillPrefersItsHeldRecordAndRejectsCompetingRecords() {
        val named = "01a15cdd-1811-7021-8f06-bee1874ef4c9"
        val held = "01a12ac0-a220-7763-989c-9bcc2e0367aa"
        val record = File(home, ".codex/sessions/rollout-2026-10-11T20-37-09-$held.jsonl").apply { parentFile.mkdirs(); writeText("{}") }
        val another = File(home, ".codex/sessions/rollout-2026-10-11T20-37-09-$named.jsonl").apply { writeText("{}") }
        assertEquals(
            AgentConversationRef("codex", held),
            exactRunningAgentConversation(9, listOf(record.path), "codex", listOf("exec", "resume", named), home),
        )
        assertEquals(
            AgentConversationRef("codex", null),
            exactRunningAgentConversation(9, listOf(record.path, another.path), "codex", listOf("exec", "resume", named), home),
        )

        val first = File(home, ".gemini/antigravity-cli/log/first.log").apply {
            parentFile.mkdirs(); writeText("Created conversation $held\n")
        }
        val second = File(home, ".gemini/antigravity-cli/log/second.log").apply { writeText("Created conversation $named\n") }
        assertEquals(
            AgentConversationRef("agy", null),
            exactRunningAgentConversation(9, listOf(first.path, second.path), "agy", listOf("--conversation", named), home),
        )
    }

    @Test
    fun exactCheckpointCapturesTheUniqueCodexRecordHeldOpen() {
        val record = File(home, ".codex/sessions/rollout-2026-10-11T20-37-09-01a12ac0-a220-7763-989c-9bcc2e0367aa.jsonl").apply { parentFile.mkdirs(); writeText("{}") }

        assertEquals(
            AgentConversationRef("codex", "01a12ac0-a220-7763-989c-9bcc2e0367aa"),
            exactRunningAgentConversation(listOf(9), listOf(record.path), "codex", home) { null },
        )
    }

    @Test
    fun exactCheckpointRejectsCompetingClaudeProcessSessionIds() {
        File(home, ".claude/sessions").mkdirs()
        File(home, ".claude/sessions/42.json").writeText("""{"sessionId":"first"}""")
        File(home, ".claude/sessions/43.json").writeText("""{"sessionId":"second"}""")

        assertEquals(
            AgentConversationRef("claude", null),
            exactRunningAgentConversation(listOf(42, 43), emptyList(), "claude", home),
        )
        assertEquals(
            AgentConversationRef("claude", "first"),
            runningAgentConversation(listOf(42, 43), emptyList(), "claude", home = home),
        )
    }

    @Test
    fun exactCheckpointAcceptsDuplicateClaudeProcessSessionIds() {
        File(home, ".claude/sessions").mkdirs()
        File(home, ".claude/sessions/42.json").writeText("""{"sessionId":"same"}""")
        File(home, ".claude/sessions/43.json").writeText("""{"sessionId":"same"}""")

        assertEquals(
            AgentConversationRef("claude", "same"),
            exactRunningAgentConversation(listOf(42, 43), emptyList(), "claude", home),
        )
    }

    @Test
    fun exactCheckpointRejectsCompetingAgyLogConversationIds() {
        val folder = File(home, ".gemini/antigravity-cli/log").apply { mkdirs() }
        val older = File(folder, "older.log").apply {
            writeText("Created conversation 05446238-6cf7-4b4d-9876-894e6a2b70f0\n")
            setLastModified(1_000)
        }
        val newer = File(folder, "newer.log").apply {
            writeText("Created conversation 15446238-6cf7-4b4d-9876-894e6a2b70f1\n")
            setLastModified(2_000)
        }

        assertEquals(
            AgentConversationRef("agy", null),
            exactRunningAgentConversation(listOf(9), listOf(older.path, newer.path), "agy", home),
        )
        assertEquals(
            AgentConversationRef("agy", "15446238-6cf7-4b4d-9876-894e6a2b70f1"),
            runningAgentConversation(listOf(9), listOf(older.path, newer.path), "agy", home = home),
        )
    }

    @Test
    fun exactCheckpointAcceptsDuplicateAgyLogConversationIds() {
        val folder = File(home, ".gemini/antigravity-cli/log").apply { mkdirs() }
        val first = File(folder, "first.log").apply {
            writeText("Created conversation 05446238-6cf7-4b4d-9876-894e6a2b70f0\n")
        }
        val second = File(folder, "second.log").apply {
            writeText("Streaming conversation 05446238-6cf7-4b4d-9876-894e6a2b70f0\n")
        }

        assertEquals(
            AgentConversationRef("agy", "05446238-6cf7-4b4d-9876-894e6a2b70f0"),
            exactRunningAgentConversation(listOf(9), listOf(first.path, second.path), "agy", home),
        )
    }

    @Test
    fun competingCodexThreadsInTheSameFolderAreNotAttributedToTheLatestOne() {
        codexThreads(
            Triple("first", "/p", 8_000L),
            Triple("latest", "/p", 9_000L),
        )

        assertEquals(
            AgentConversationRef("codex", null),
            runningAgentConversation(listOf(9), emptyList(), "codex", listOf("/p"), home) {
                CodexProcess(startedAtMillis = 8_000, namedThread = null)
            },
        )
    }

    @Test
    fun codexResumedByIdNamesItsThreadOnItsCommandLine() {
        val started = { _: Long -> CodexProcess(startedAtMillis = 8_000, namedThread = "named") }
        codexThreads(Triple("first", "/p", 8_000L), Triple("latest", "/p", 9_000L))

        assertEquals(AgentConversationRef("codex", "named"), runningAgentConversation(listOf(9), emptyList(), "codex", listOf("/p"), home, started))
    }

    @Test
    fun anOpenedCodexRecordTakesPrecedenceOverTheThreadNamedAtStartup() {
        val record = File(home, ".codex/sessions/rollout-2026-10-11T20-37-09-01a12ac0-a220-7763-989c-9bcc2e0367aa.jsonl").apply { parentFile.mkdirs(); writeText("{}") }

        assertEquals(
            AgentConversationRef("codex", "01a12ac0-a220-7763-989c-9bcc2e0367aa"),
            runningAgentConversation(listOf(9), listOf(record.path), "codex", home = home) {
                CodexProcess(startedAtMillis = 8_000, namedThread = "named-at-startup")
            },
        )
    }

    @Test
    fun competingNamedCodexProcessesAreNotAttributedToTheFirstProcess() {
        assertEquals(
            AgentConversationRef("codex", null),
            runningAgentConversation(listOf(9, 10), emptyList(), "codex", home = home) { pid ->
                CodexProcess(startedAtMillis = 8_000, namedThread = "thread-$pid")
            },
        )
    }

    @Test
    fun anUnnamedCodexProcessCannotBeAttributedToAnotherProcessesNamedThread() {
        assertEquals(
            AgentConversationRef("codex", null),
            runningAgentConversation(listOf(9, 10), emptyList(), "codex", home = home) { pid ->
                CodexProcess(startedAtMillis = 8_000, namedThread = "named".takeIf { pid == 9L })
            },
        )
    }

    @Test
    fun multipleUnnamedCodexProcessesCannotShareOneInferredDatabaseThread() {
        codexThreads(Triple("one-thread", "/p", 9_000L))

        assertEquals(
            AgentConversationRef("codex", null),
            runningAgentConversation(listOf(9, 10), emptyList(), "codex", listOf("/p"), home) {
                CodexProcess(startedAtMillis = 8_000, namedThread = null)
            },
        )
    }

    @Test
    fun anAgentWhoseConversationCannotBeReadIsStillNamed() {
        assertEquals(AgentConversationRef("agy", null), runningAgentConversation(listOf(9), emptyList(), "agy", home = home) { null })
    }

    private fun codexThreads(vararg threads: Triple<String, String, Long>) {
        val database = File(home, ".codex/state_5.sqlite").apply { parentFile.mkdirs() }
        BundledSQLiteDriver().open(database.path).use { connection ->
            connection.execSQL("CREATE TABLE threads (id TEXT PRIMARY KEY, cwd TEXT, archived INTEGER, thread_source TEXT, updated_at_ms INTEGER)")
            threads.forEach { (id, cwd, updated) ->
                connection.execSQL("INSERT INTO threads VALUES ('$id', '$cwd', 0, 'user', $updated)")
            }
            // A review Codex runs inside the conversation is newer, but not what the terminal shows.
            connection.execSQL("INSERT INTO threads VALUES ('review', '/p', 0, 'guardian_review', 9900)")
            connection.execSQL("INSERT INTO threads VALUES ('archived', '/p', 1, 'user', 9999)")
        }
    }
}
