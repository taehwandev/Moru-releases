package app.thdev.moru.feature.workbench.resume

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AgentRunningStateTest {
    @Test
    fun anUnknownReadingCannotEnableResume() {
        assertNull(agentRunningState(listOf(null)))
        assertNull(agentRunningState(listOf(false, null)))
        assertTrue(agentRunningState(listOf(null, true)) == true)
    }

    @Test
    fun resumeCanBeConsideredOnlyWhenEveryObservedAgentIsIdle() {
        assertFalse(agentRunningState(emptyList())!!)
        assertFalse(agentRunningState(listOf(false, false))!!)
        assertTrue(agentRunningState(listOf(false, true))!!)
    }
}
