package app.thdev.moru.feature.workbench

import app.thdev.moru.core.domain.TaskSummary
import app.thdev.moru.core.model.TaskAttention
import app.thdev.moru.core.model.TaskStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionNamedTaskTitlesTest {
    private fun task(id: String, title: String) =
        TaskSummary(id, "p1", title, TaskStatus.Understanding, TaskAttention.None, updatedAtEpochMillis = 0, createdAtEpochMillis = 0)

    @Test
    fun aNumberedTaskTakesItsFirstSessionsSummary() {
        val tasks = listOf(task("a", "Moru task 1"), task("b", "Moru task 2"))

        assertEquals(
            mapOf("a" to "서비스 안정성 문제"),
            sessionNamedTaskTitles("Moru", tasks, mapOf("a" to "  서비스 안정성 문제 ")),
        )
    }

    @Test
    fun aTaskWithItsOwnNameKeepsIt() {
        val tasks = listOf(task("a", "Fix login"), task("b", "KeyFlow task 1"), task("c", "Moru task 1b"))
        val summaries = mapOf("a" to "Something else", "b" to "Other project", "c" to "Looks numbered")

        assertTrue(sessionNamedTaskTitles("Moru", tasks, summaries).isEmpty())
    }

    @Test
    fun aLongSummaryIsCutLikeADescriptionsFirstLine() {
        val title = sessionNamedTaskTitles("Moru", listOf(task("a", "Moru task 1")), mapOf("a" to "x".repeat(120)))["a"]

        assertEquals(80, title?.length)
    }

    @Test
    fun onlyTheProjectsOwnNumbersCount() {
        assertTrue(isNumberedTaskTitle("Moru", "Moru task 12"))
        assertFalse(isNumberedTaskTitle("Moru", "Moru task"))
        assertFalse(isNumberedTaskTitle("Moru", "Mor task 1"))
    }
}
