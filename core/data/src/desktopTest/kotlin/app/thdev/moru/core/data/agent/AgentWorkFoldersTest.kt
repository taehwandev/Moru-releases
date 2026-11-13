package app.thdev.moru.core.data.agent

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AgentWorkFoldersTest {
    private val home = Files.createTempDirectory("agent-home").toFile()
    private val folders = AgentWorkFolders(home)

    @AfterTest
    fun cleanUp() {
        home.deleteRecursively()
    }

    @Test
    fun claudeIsFoundByItsProcessAndReadFromItsRecord() {
        write(".claude/sessions/4242.json", """{"pid":4242,"sessionId":"abc","cwd":"/repo"}""")
        write(
            ".claude/projects/-repo/abc.jsonl",
            "{\"cwd\":\"/repo\"}\n{\"cwd\":\"/repo/.worktrees/a\"}\n",
        )

        assertEquals(listOf("/repo/.worktrees/a"), folders.read(listOf(1L, 4242L), emptyList()))
        assertEquals(emptyList(), folders.read(listOf(1L), emptyList()))
    }

    @Test
    fun claudeRecordIsFoundWhenItLivesUnderAnotherFolder() {
        write(".claude/sessions/7.json", """{"pid":7,"sessionId":"moved","cwd":"/repo"}""")
        write(".claude/projects/-other/moved.jsonl", "{\"cwd\":\"/repo/.worktrees/b\"}\n")

        assertEquals(listOf("/repo/.worktrees/b"), folders.read(listOf(7L), emptyList()))
    }

    @Test
    fun codexIsReadFromTheRecordItHoldsOpen() {
        val record = write(
            ".codex/sessions/2026/10/10/rollout-2026-10-10T11-14-09-x.jsonl",
            """{"type":"response_item","payload":{"type":"custom_tool_call","name":"exec","input":"tools.exec_command({cmd:\"cd /repo/.worktrees/c && ls\"})"}}""" + "\n",
        )

        assertEquals(listOf("/repo/.worktrees/c"), folders.read(emptyList(), listOf("/usr/lib/libc.dylib", record.path)))
    }

    @Test
    fun codexIsReadFromItsOwnThreadNotTheGuardianItAlsoHoldsOpen() {
        val own = write(
            ".codex/sessions/2026/10/15/rollout-2026-10-15T17-40-34-own.jsonl",
            """{"type":"session_meta","payload":{"id":"own","source":"cli","cwd":"/repo"}}""" + "\n" +
                """{"type":"response_item","payload":{"type":"custom_tool_call","name":"exec","input":"tools.exec_command({cmd:\"cd /repo/.worktrees/c && ls\"})"}}""" + "\n",
        )
        val guardian = write(
            ".codex/sessions/2026/10/18/rollout-2026-10-18T10-59-13-guardian.jsonl",
            """{"type":"session_meta","payload":{"id":"guardian","source":{"subagent":{"other":"guardian"}},"cwd":"/repo"}}""" + "\n" +
                """{"type":"response_item","payload":{"type":"custom_tool_call","name":"exec","input":"tools.exec_command({cmd:\"cd /elsewhere && ls\"})"}}""" + "\n",
        )
        // The guardian wrote last, as it does while reviewing each command.
        guardian.setLastModified(own.lastModified() + 10_000)

        assertEquals(listOf("/repo/.worktrees/c"), folders.read(emptyList(), listOf(own.path, guardian.path)))
    }

    @Test
    fun agyIsReadFromTheConversationItsLogNames() {
        val id = "268b766a-d978-4590-b331-2bfdff0cff19"
        val log = write(".gemini/antigravity-cli/log/cli-20261010_090239.log", "I1009 server.go:1274] Created conversation $id\n")
        val database = File(home, ".gemini/antigravity-cli/conversations/$id.db").apply { parentFile.mkdirs() }
        BundledSQLiteDriver().open(database.path).use { connection ->
            connection.execSQL("CREATE TABLE steps (idx integer PRIMARY KEY, step_payload blob)")
            connection.execSQL(
                "INSERT INTO steps VALUES (1, CAST('{\"CommandLine\":\"ls\",\"Cwd\":\"/repo\"}' AS BLOB))," +
                    " (2, CAST('{\"CommandLine\":\"git status\",\"Cwd\":\"/repo/.worktrees/d\"}' AS BLOB))," +
                    " (3, CAST('no command here' AS BLOB))",
            )
        }

        assertEquals(listOf("/repo/.worktrees/d"), folders.read(emptyList(), listOf(log.path)))
    }

    @Test
    fun agyLogIsFollowedAsItGrows() {
        val first = "11111111-1111-1111-1111-111111111111"
        val second = "22222222-2222-2222-2222-222222222222"
        val log = write(".gemini/antigravity-cli/log/cli-1.log", "Created conversation $first\n")
        stepDatabase(first, "/repo/.worktrees/first")
        stepDatabase(second, "/repo/.worktrees/second")

        assertEquals(listOf("/repo/.worktrees/first"), folders.read(emptyList(), listOf(log.path)))
        log.appendText("Streaming conversation $second\n")
        assertEquals(listOf("/repo/.worktrees/second"), folders.read(emptyList(), listOf(log.path)))
    }

    private fun stepDatabase(id: String, cwd: String) {
        val database = File(home, ".gemini/antigravity-cli/conversations/$id.db").apply { parentFile.mkdirs() }
        BundledSQLiteDriver().open(database.path).use { connection ->
            connection.execSQL("CREATE TABLE steps (idx integer PRIMARY KEY, step_payload blob)")
            connection.execSQL("INSERT INTO steps VALUES (1, CAST('{\"CommandLine\":\"ls\",\"Cwd\":\"$cwd\"}' AS BLOB))")
        }
    }

    private fun write(path: String, text: String): File =
        File(home, path).apply {
            parentFile.mkdirs()
            writeText(text)
        }
}
