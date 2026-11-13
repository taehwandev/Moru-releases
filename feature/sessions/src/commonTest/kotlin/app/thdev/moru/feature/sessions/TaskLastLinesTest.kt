package app.thdev.moru.feature.sessions

import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.terminal.TerminalSessionStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskLastLinesTest {
    @Test
    fun addingAnEmptySessionKeepsObservedWorkAndOtherTasksStaySeparate() {
        val snapshots = listOf(
            session("claude", "task-a", 100),
            session("codex", "task-a", 200),
            session("new-agy", "task-a", 300),
            session("other", "task-b", 400),
            session("unowned", null, 500),
        )
        val lines = mapOf("claude" to "Reading files", "codex" to "Checking tests", "other" to "Other work", "unowned" to "Unowned work")
        assertEquals(mapOf("task-a" to "Checking tests", "task-b" to "Other work"), taskLastLines(snapshots, lines::get))
    }

    @Test
    fun newOutputTakesOverOnlyItsOwningTasksHint() {
        val snapshots = listOf(session("first", "task-a", 200), session("added", "task-a", 300))
        assertEquals(mapOf("task-a" to "Writing the change"), taskLastLines(snapshots) { id ->
            if (id == "added") "Writing the change" else "Reading files"
        })
        assertEquals(emptyMap(), taskLastLines(snapshots) { null })
    }

    @Test
    fun severalSessionsOnOneTaskShowTheLineOfTheOneThatPrintedMostRecently() {
        fun session(id: String, task: String?, at: Long?) = TerminalSessionSnapshot(
            id = id,
            spec = TerminalSessionSpec(title = id, attachedItemId = task),
            status = TerminalSessionStatus.READY,
            lastOutputAtMillis = at,
        )
        val lines = mapOf("old" to "Reading files", "new" to "Editing Main.kt", "loose" to "ls", "silent" to null)

        val result = taskLastLines(
            listOf(session("old", "t1", 1_000), session("new", "t1", 9_000), session("loose", null, 5_000), session("silent", "t2", 3_000)),
        ) { id -> lines[id] }

        assertEquals(mapOf("t1" to "Editing Main.kt"), result)
    }

    private fun session(id: String, task: String?, at: Long) = TerminalSessionSnapshot(
        id, TerminalSessionSpec(attachedItemId = task), TerminalSessionStatus.READY, lastOutputAtMillis = at,
    )
}
