package app.thdev.moru.feature.workbench

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberedTaskTitleTest {
    @Test
    fun theFirstTaskOfAProjectIsNumberOne() {
        assertEquals("Moru task 1", nextNumberedTaskTitle("Moru", emptyList()))
    }

    @Test
    fun theNextNumberFollowsTheHighestNumberedTaskAndIgnoresOtherTitles() {
        val existing = listOf("Moru task 1", "Codex session", "Moru task 4", "Fix login", "Moru task x", "Other task 9")

        assertEquals("Moru task 5", nextNumberedTaskTitle("Moru", existing))
    }
}
