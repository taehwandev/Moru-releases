package app.thdev.moru.core.terminal

import kotlin.test.Test
import kotlin.test.assertEquals

class ProcessFilesParserTest {
    @Test
    fun readsEachProcessFolderOnceInOrder() {
        val output = "p100\nfcwd\nn/repo\np200\nfcwd\nn/repo/.claude/worktrees/a\np300\nfcwd\nn/repo\n"

        assertEquals(listOf("/repo", "/repo/.claude/worktrees/a"), parseLsofProcessFiles(output).workingFolders)
    }

    @Test
    fun keepsNumberedDescriptorsAsOpenFilesAndSkipsTheRest() {
        val output = "p100\nfcwd\nn/repo\nftxt\nn/bin/codex\nf3\nn/home/.codex/sessions/rollout-a.jsonl\n" +
            "p200\nfcwd\nn/repo\nf12\nn/home/.codex/sessions/rollout-a.jsonl\nf13\nn/home/log/cli.log\n"

        val files = parseLsofProcessFiles(output)

        assertEquals(listOf("/repo"), files.workingFolders)
        assertEquals(listOf("/home/.codex/sessions/rollout-a.jsonl", "/home/log/cli.log"), files.openFiles)
    }

    @Test
    fun emptyOutputHasNoFolders() {
        assertEquals(ProcessFiles(emptyList(), emptyList()), parseLsofProcessFiles(""))
    }
}
