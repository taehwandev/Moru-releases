package app.thdev.moru.feature.workbench.restore

import app.thdev.moru.core.model.SavedTerminal
import app.thdev.moru.core.model.WorkbenchLayout
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TerminalRestoreTest {
    @Test
    fun anAgentPicksItsOwnConversationUpInsteadOfGettingItsFirstRequestAgain() {
        assertEquals("claude --resume c1", terminalRestoreCommand("claude --session-id c1 'fix the login bug'"))
        assertEquals("codex --no-daemon resume x9", terminalRestoreCommand("codex --no-daemon resume x9"))
        assertEquals("agy --conversation a7", terminalRestoreCommand("agy --conversation a7 || agy"))
        // A terminal that was already brought back keeps coming back to the same conversation.
        assertEquals("claude --resume c1", terminalRestoreCommand("claude --resume c1 || claude --session-id c1"))
    }

    @Test
    fun anAgentWithoutAKnownConversationStartsEmptyWithoutAPickerOrReplayingThePrompt() {
        assertEquals("claude", terminalRestoreCommand("claude 'fix the login bug'"))
        assertEquals("claude", terminalRestoreCommand("claude --continue || claude"))
        listOf("codex", "codex --no-daemon", "codex 'add billing'", "codex --no-daemon resume || codex --no-daemon").forEach {
            assertEquals("codex --no-daemon", terminalRestoreCommand(it))
        }
        assertEquals("agy", terminalRestoreCommand("agy --continue || agy"))
    }

    @Test
    fun aBuildOrAShellIsNotRunAgainOnItsOwn() {
        assertNull(terminalRestoreCommand(null))
        assertNull(terminalRestoreCommand("   "))
        assertNull(terminalRestoreCommand("./gradlew installDebug"))
        assertNull(terminalRestoreCommand("npm run dev"))
    }

    @Test
    fun legacySavedResumeFallbacksAreRemovedBeforeATerminalCanStart() {
        val legacyClaude = "claude --resume c1 || claude --session-id c1"
        val layout = WorkbenchLayout(terminals = listOf(
            SavedTerminal(title = "Claude", startupCommand = legacyClaude, hostKey = "moru-c"),
            SavedTerminal(title = "Codex", startupCommand = "codex --no-daemon resume x9 || codex --no-daemon", hostKey = "moru-x"),
            SavedTerminal(title = "AGY", startupCommand = "agy --conversation a7 || agy", hostKey = "moru-a"),
            SavedTerminal(title = "Lab", startupCommand = legacyClaude, hostKey = "lab-c", lab = true),
        ))
        val after = InMemoryTerminalSessionManager()

        restoreTerminals(after, layout)

        val restored = after.listSessions()
        assertEquals(listOf("claude --resume c1", "codex --no-daemon resume x9", "agy --conversation a7", legacyClaude), restored.map { it.spec.startupCommand })
        assertEquals(listOf("moru-c", "moru-x", "moru-a", "lab-c"), restored.map { it.spec.hostKey })
        assertEquals(TerminalSessionKind.LAB, restored.last().spec.kind)
    }

    @Test
    fun malformedSavedConversationIdsAndNonAgentCommandsComeBackAsPlainShells() {
        val commands = listOf("claude --resume c1;touch", "codex --no-daemon resume x9$(touch file)", "agy --conversation 'a7'", "npm run dev", null)
        val layout = WorkbenchLayout(terminals = commands.map { SavedTerminal(startupCommand = it) })
        val after = InMemoryTerminalSessionManager()

        restoreTerminals(after, layout)

        assertEquals(List(commands.size) { null }, after.listSessions().map { it.spec.startupCommand })
    }

    @Test
    fun theTerminalsOfEveryTaskComeBackInOrderWithTheChosenOneChosen() {
        val before = InMemoryTerminalSessionManager()
        before.createSession(TerminalSessionSpec(title = "Claude", workingDirectory = "/work/a", attachedItemId = "t1", startupCommand = "claude --session-id c1 'fix it'"))
        val chosen = before.createSession(TerminalSessionSpec(title = "Terminal 2", workingDirectory = "/work/a", attachedItemId = "t1")).id
        before.createSession(TerminalSessionSpec(title = "Page", kind = TerminalSessionKind.WEB_PREVIEW, url = "http://localhost:5173/", attachedItemId = "t2"))
        before.selectSession(chosen)
        val sessions = before.listSessions()
        val layout = WorkbenchLayout(terminals = savedTerminalsOf(sessions), selectedTerminal = sessions.indexOfFirst { it.id == chosen })

        val after = InMemoryTerminalSessionManager()
        restoreTerminals(after, layout)

        val restored = after.listSessions()
        assertEquals(listOf("Claude", "Terminal 2", "Page"), restored.map { it.title })
        assertEquals(listOf("t1", "t1", "t2"), restored.map { it.spec.attachedItemId })
        assertEquals(listOf("claude --resume c1", null, null), restored.map { it.spec.startupCommand })
        assertEquals(TerminalSessionKind.WEB_PREVIEW, restored[2].spec.kind)
        assertEquals("/work/a", restored[0].spec.workingDirectory)
        assertEquals(restored[1].id, after.selectedSessionId())
    }

    @Test
    fun aRunningTerminalKeepsItsHostNameSoMoruReattachesButAnEndedOneStartsAfresh() {
        val before = InMemoryTerminalSessionManager()
        before.createSession(TerminalSessionSpec(title = "Claude", attachedItemId = "t1", startupCommand = "claude", hostKey = "moru-a"))
        val ended = before.createSession(TerminalSessionSpec(title = "Build", attachedItemId = "t1", hostKey = "moru-b")).id
        before.reportExit(ended, 0)
        val layout = WorkbenchLayout(terminals = savedTerminalsOf(before.listSessions()))

        assertEquals(listOf("moru-a", null), layout.terminals.map { it.hostKey })

        val after = InMemoryTerminalSessionManager()
        restoreTerminals(after, layout)
        assertEquals("moru-a", after.listSessions().first { it.title == "Claude" }.spec.hostKey)
        assertNull(after.listSessions().first { it.title == "Build" }.spec.hostKey)
    }

    @Test
    fun aTerminalWhoseLinkToTheHostBrokeKeepsItsHostNameAndConversationForEitherRecoveryPath() {
        val before = InMemoryTerminalSessionManager()
        val commands = listOf("claude --session-id c1 'fix it'", "codex --no-daemon resume x9", "agy --conversation a7 || agy", "npm run dev")
        commands.forEachIndexed { index, command ->
            val cut = before.createSession(TerminalSessionSpec(attachedItemId = "t1", startupCommand = command, hostKey = "moru-$index")).id
            // The host went away or another client took the terminal: its program may still run.
            before.reportExit(cut, null)
        }

        val layout = WorkbenchLayout(terminals = savedTerminalsOf(before.listSessions()))

        assertEquals(listOf("moru-0", "moru-1", "moru-2", "moru-3"), layout.terminals.map { it.hostKey })
        assertEquals(listOf("claude --resume c1", "codex --no-daemon resume x9", "agy --conversation a7", null), layout.terminals.map { it.startupCommand })

        val after = InMemoryTerminalSessionManager()
        restoreTerminals(after, layout)
        assertEquals(layout.terminals.map { it.hostKey }, after.listSessions().map { it.spec.hostKey })
        assertEquals(layout.terminals.map { it.startupCommand }, after.listSessions().map { it.spec.startupCommand })
    }

    @Test
    fun aLabSessionKeepsItsConversationKeyEvenWhenItsHostLinkWasLost() {
        val before = InMemoryTerminalSessionManager()
        val labId = before.createSession(
            TerminalSessionSpec(title = "Claude Lab", kind = TerminalSessionKind.LAB, attachedItemId = "t1", startupCommand = "claude", hostKey = "lab-a"),
        ).id
        before.reportExit(labId, null)
        val layout = WorkbenchLayout(terminals = savedTerminalsOf(before.listSessions()))

        assertEquals(listOf(true), layout.terminals.map { it.lab })

        val after = InMemoryTerminalSessionManager()
        restoreTerminals(after, layout)
        val lab = after.listSessions().single()
        assertEquals(TerminalSessionKind.LAB, lab.spec.kind)
        assertEquals("lab-a", lab.spec.hostKey)
        assertEquals("claude", lab.spec.startupCommand)
    }
}
