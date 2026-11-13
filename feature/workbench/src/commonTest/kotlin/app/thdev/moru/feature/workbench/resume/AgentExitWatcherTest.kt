package app.thdev.moru.feature.workbench.resume

import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AgentExitWatcherTest {
    @Test
    fun aSessionNeverSeenRunningIsNotExited() {
        val watcher = AgentExitWatcher()

        repeat(5) { assertFalse(watcher.observe("s1", false)) }
    }

    @Test
    fun itCountsAsExitedOnlyAfterTheShellStaysAtItsPromptForTheConfirmingReadings() {
        val watcher = AgentExitWatcher(confirmations = 2)
        watcher.observe("s1", true)

        assertFalse(watcher.observe("s1", false))
        assertTrue(watcher.observe("s1", false))
    }

    @Test
    fun aFlickerBetweenProgramsIsNotAnExit() {
        val watcher = AgentExitWatcher(confirmations = 2)
        watcher.observe("s1", true)
        watcher.observe("s1", false)
        assertFalse(watcher.observe("s1", true))

        assertFalse(watcher.observe("s1", false))
    }

    @Test
    fun anUnknownReadingChangesNothing() {
        val watcher = AgentExitWatcher(confirmations = 2)
        watcher.observe("s1", true)
        watcher.observe("s1", false)

        assertFalse(watcher.observe("s1", null))
        assertTrue(watcher.observe("s1", false))
        assertTrue(watcher.observe("s1", null))
    }

    @Test
    fun resumingOrDismissingMeansItMustBeSeenRunningAgainBeforeItCanExitAgain() {
        val watcher = AgentExitWatcher(confirmations = 1)
        watcher.observe("s1", true)
        assertTrue(watcher.observe("s1", false))

        watcher.acknowledge("s1")

        assertFalse(watcher.observe("s1", false))
        watcher.observe("s1", true)
        assertTrue(watcher.observe("s1", false))
    }

    @Test
    fun sessionsAreJudgedIndependentlyAndAClosedOneIsForgotten() {
        val watcher = AgentExitWatcher(confirmations = 1)
        watcher.observe("a", true)
        watcher.observe("b", true)
        watcher.observe("a", false)

        assertEquals(setOf("a"), watcher.exitedSessions())
        watcher.retainOnly(setOf("b"))
        assertEquals(emptySet(), watcher.exitedSessions())
    }

    @Test
    fun onlyAnAgentThatCanResumeIsRecognisedFromItsStartupCommand() {
        fun runtime(command: String?) = launchRuntimeOf(TerminalSessionSpec(title = "t", startupCommand = command))

        assertEquals(AgentRuntime.Claude, runtime("claude 'fix it'"))
        assertEquals(AgentRuntime.Claude, runtime("/usr/local/bin/claude"))
        assertEquals(AgentRuntime.Codex, runtime("codex resume --last"))
        assertEquals(AgentRuntime.Agy, runtime("agy --continue"))
        assertNull(runtime("npm run dev"))
        assertNull(runtime(null))
    }
}
