package app.thdev.moru.feature.workbench.beside

import app.thdev.moru.core.model.TaskAttention
import app.thdev.moru.feature.workbench.ProjectRow
import app.thdev.moru.feature.workbench.TaskDisplayState
import app.thdev.moru.feature.workbench.TaskGroup
import app.thdev.moru.feature.workbench.TaskRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BesideTaskIdsTest {
    private fun project(id: String, vararg tasks: String) = ProjectRow(
        id = id, displayName = id, rootPath = "/work/$id", availabilityLabel = null, isAvailable = true,
        expanded = true, selected = false,
        tasks = tasks.map { task ->
            TaskRow(
                id = task, projectId = id, projectName = id, title = task, displayState = TaskDisplayState.Working,
                group = TaskGroup.Running, attention = TaskAttention.None, hasWorkContext = false, selected = false,
            )
        },
    )

    private val projects = listOf(project("a", "a1", "a2", "a3"), project("b", "b1", "b2"))

    @Test
    fun openingATaskBesideReplacesTheOneOfItsProjectAndKeepsOtherProjects() {
        val saved = listOf("b2", "a2")
        assertEquals(listOf("b2", "a3"), BesideTaskIds.toggle(saved, "a3", projects))
        assertEquals(listOf("b2"), BesideTaskIds.toggle(saved, "a2", projects))
    }

    @Test
    fun onlyATaskOfTheSelectedTasksProjectIsShownBeside() {
        assertEquals("a2", BesideTaskIds.shown(listOf("b2", "a2"), "a1", projects))
        assertEquals("b2", BesideTaskIds.shown(listOf("b2", "a2"), "b1", projects))
        assertNull(BesideTaskIds.shown(listOf("b2"), "a1", projects))
        assertNull(BesideTaskIds.shown(listOf("a1"), "a1", projects), "the selected Task is never beside itself")
    }

    @Test
    fun aSavedWholeProjectFromTheReplacedFeatureIsNeverShownAndIsDroppedOnChange() {
        val saved = listOf("project:b", "a2")
        assertEquals("a2", BesideTaskIds.shown(saved, "a1", projects))
        assertEquals(listOf("a3"), BesideTaskIds.toggle(saved, "a3", projects))
    }
}
