package app.thdev.moru.feature.workbench

import app.thdev.moru.core.domain.TaskSummary
import app.thdev.moru.core.model.TaskAttention
import app.thdev.moru.core.model.TaskStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AgentNamedTaskTitlesTest {
    private val agents = listOf("Claude", "Codex", "AGY", "Terminal")

    private fun task(id: String, title: String, createdAt: Long) =
        TaskSummary(id, "p1", title, TaskStatus.Understanding, TaskAttention.None, updatedAtEpochMillis = 0, createdAtEpochMillis = createdAt)

    @Test
    fun agentNamedTasksAreNumberedOldestFirstAfterTheNumbersInUse() {
        val tasks = listOf(
            task("newest", "Claude session", createdAt = 30),
            task("kept", "Fix login", createdAt = 5),
            task("numbered", "Moru task 1", createdAt = 10),
            task("oldest", "Codex session", createdAt = 20),
        )

        assertEquals(
            mapOf("oldest" to "Moru task 2", "newest" to "Moru task 3"),
            agentNamedTaskTitles("Moru", tasks, agents),
        )
    }

    @Test
    fun titlesThatOnlyLookSimilarAreLeftAloneSoApplyingItAgainChangesNothing() {
        val tasks = listOf(task("a", "Login session", 1), task("b", "codex session", 2), task("c", "Moru task 1", 3))

        assertTrue(agentNamedTaskTitles("Moru", tasks, agents).isEmpty())
    }
}
