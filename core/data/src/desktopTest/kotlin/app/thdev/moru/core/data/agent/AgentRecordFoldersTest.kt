package app.thdev.moru.core.data.agent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AgentRecordFoldersTest {
    @Test
    fun claudeRecordNamesTheFolderOfItsLastLine() {
        val record = """
            {"type":"user","cwd":"/repo","sessionId":"s"}
            {"type":"assistant","cwd":"/repo/.worktrees/a","sessionId":"s"}
            {"type":"assistant","cwd":"/repo/.worktrees/a/desktop-kmp","sessionId":"s"}
        """.trimIndent()

        assertEquals("/repo/.worktrees/a/desktop-kmp", claudeRecordFolder(record))
        assertNull(claudeRecordFolder("{\"type\":\"summary\"}"))
    }

    @Test
    fun claudeRecordFolderReadsEscapedCharacters() {
        assertEquals("/repo/작업", claudeRecordFolder("{\"cwd\":\"/repo/\\uc791\\uc5c5\"}"))
    }

    @Test
    fun codexRecordNamesTheFoldersOfItsLastCommand() {
        val record = listOf(
            """{"type":"turn_context","payload":{"cwd":"/repo"}}""",
            codexCall("""text(await tools.exec_command({cmd:\"cd /repo/.worktrees/old && ls\"}));"""),
            codexCall("""text(await tools.exec_command({cmd:\"cd '/repo/.worktrees/new' && git status\"}));"""),
            """{"type":"response_item","payload":{"type":"custom_tool_call_output","output":"cd /elsewhere"}}""",
        ).joinToString("\n")

        assertEquals(listOf("/repo/.worktrees/new"), codexRecordFolders(record))
    }

    @Test
    fun codexRecordReadsWorkdirAndRelativeMoves() {
        val record = listOf(
            """{"type":"turn_context","payload":{"cwd":"/repo"}}""",
            codexCall("""text(await tools.exec_command({cmd:\"cd .worktrees/b && ls\"}));"""),
        ).joinToString("\n")
        val withWorkdir = codexCall("""text(await tools.exec_command({cmd:\"ls\", workdir:\"/repo/.worktrees/c\"}));""")

        assertEquals(listOf("/repo/.worktrees/b"), codexRecordFolders(record))
        assertEquals(listOf("/repo/.worktrees/c"), codexRecordFolders(withWorkdir))
    }

    @Test
    fun codexCommandWithoutAFolderLeavesTheEarlierOne() {
        val record = listOf(
            codexCall("""text(await tools.exec_command({cmd:\"cd /repo/.worktrees/a && ls\"}));"""),
            codexCall("""text(await tools.exec_command({cmd:\"git status\"}));"""),
        ).joinToString("\n")

        assertEquals(listOf("/repo/.worktrees/a"), codexRecordFolders(record))
    }

    @Test
    fun codexRecordReadsGitDirectoriesAndPatchedFiles() {
        val record = listOf(
            """{"type":"turn_context","payload":{"cwd":"/repo"}}""",
            codexCall("""text(await tools.exec_command({cmd:\"git -C /tmp/wt add a.kt && git -C /tmp/wt commit -m x\"}));"""),
        ).joinToString("\n")
        val patch = """{"type":"response_item","payload":{"type":"custom_tool_call","name":"apply_patch",""" +
            """"input":"*** Begin Patch\\n*** Update File: /tmp/wt2/src/A.kt\\n@@\\n-a\\n+b\\n*** End Patch"}}"""

        assertEquals(listOf("/tmp/wt"), codexRecordFolders(record))
        assertEquals(listOf("/tmp/wt2/src"), codexRecordFolders(patch))
    }

    @Test
    fun claudeRecordNamesFoldersItsLastToolCallReaches() {
        val record = listOf(
            """{"type":"assistant","cwd":"/repo","message":{"content":[{"type":"tool_use","name":"Bash","input":{"command":"git -C /tmp/wt status"}}]}}""",
            """{"type":"user","cwd":"/repo","message":{"content":[{"type":"tool_result","content":"ok"}]}}""",
        ).joinToString("\n")
        val edit = """{"type":"assistant","cwd":"/repo","message":{"content":[""" +
            """{"type":"tool_use","name":"Read","input":{"file_path":"/elsewhere/B.kt"}},""" +
            """{"type":"tool_use","name":"Edit","input":{"file_path":"/tmp/wt/src/A.kt"}}]}}"""

        assertEquals(listOf("/tmp/wt", "/repo"), claudeRecordFolders(record))
        assertEquals(listOf("/tmp/wt/src", "/repo"), claudeRecordFolders(edit))
        assertEquals(listOf("/repo"), claudeRecordFolders("""{"type":"user","cwd":"/repo"}"""))
    }

    @Test
    fun agyLogNamesItsLastConversation() {
        val log = """
            I1009 23:08:07.525249    1647 server.go:1274] Created conversation 268b766a-d978-4590-b331-2bfdff0cff19
            I1009 23:08:07.526625    1647 conversation_manager.go:967] Streaming conversation 05446238-6cf7-4b4d-9876-894e6a2b70f0
            I1009 23:08:07.712793     415 manager.go:972] Full redraw completed (RerenderAll) for conversation  (epoch 0, items 1)
        """.trimIndent()

        assertEquals("05446238-6cf7-4b4d-9876-894e6a2b70f0", agyLogConversation(log))
        assertNull(agyLogConversation("nothing here"))
    }

    @Test
    fun agyStepNamesWhereItsLastCommandRan() {
        val payload = "\u000bcall_1\u0012\u000brun_command\u001a{\"CommandLine\":\"git status\",\"Cwd\":\"/repo\"}" +
            "\u000bcall_2\u0012\u000brun_command\u001a{\"CommandLine\":\"cd desktop-kmp && ./gradlew test\"," +
            "\"Cwd\":\"/repo/.worktrees/a\",\"WaitMsBeforeAsync\":10000}"

        assertEquals(listOf("/repo/.worktrees/a/desktop-kmp", "/repo/.worktrees/a"), agyStepFolders(payload))
        assertEquals(emptyList(), agyStepFolders("no command"))
    }

    @Test
    fun shellExpandedMovesAreSkipped() {
        assertEquals(emptyList(), commandFolders("cd ~ && cd \$HOME && cd -", "/repo"))
        assertEquals(listOf("/repo/b", "/a"), commandFolders("cd /a && ls; cd ../repo/b", "/repo"))
        assertEquals(listOf("/repo/c", "/a"), commandFolders("cd /a && git -C c status", "/repo"))
    }

    private fun codexCall(input: String): String =
        """{"type":"response_item","payload":{"type":"custom_tool_call","name":"exec","input":"$input"}}"""
}
