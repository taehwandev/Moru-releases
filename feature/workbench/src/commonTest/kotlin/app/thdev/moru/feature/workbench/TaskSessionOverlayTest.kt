package app.thdev.moru.feature.workbench

import app.thdev.moru.core.model.TaskAttention
import app.thdev.moru.core.model.session.SessionActivity
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskSessionOverlayTest {
    private fun row(state: TaskDisplayState, id: String = "task-1"): TaskRow {
        val presentation = state.presentation()
        return TaskRow(
            id = id,
            projectId = "p1",
            projectName = "Moru",
            title = "Fix login",
            displayState = state,
            group = presentation.group,
            attention = TaskAttention.None,
            hasWorkContext = true,
            selected = false,
        )
    }

    @Test
    fun aWorkingSessionMovesAPreparingTaskToWorking() {
        val overlaid = row(TaskDisplayState.Preparing).withSessionActivity(SessionActivity.Working)

        assertEquals(TaskDisplayState.Working, overlaid.displayState)
        assertEquals(TaskGroup.Running, overlaid.group)
        assertEquals(TaskGroup.Running, overlaid.group)
    }

    @Test
    fun aTurnThatJustEndedReadsDoneLikeItsSessionAndStillNeedsTheUserNeverNeedsInput() {
        val overlaid = row(TaskDisplayState.Working).withSessionActivity(SessionActivity.Quiet)

        assertEquals(TaskDisplayState.Done, overlaid.displayState)
        assertEquals(TaskGroup.NeedsAttention, overlaid.group)
    }

    @Test
    fun anAgentOrAShellWaitingForTheUserIsWaitingNeverStopped() {
        listOf(SessionActivity.Waiting, SessionActivity.Idle).forEach { activity ->
            val overlaid = row(TaskDisplayState.Working).withSessionActivity(activity)
            assertEquals(TaskDisplayState.Waiting, overlaid.displayState, "$activity")
            assertEquals(TaskGroup.Done, overlaid.group, "$activity")
        }
    }

    @Test
    fun aFailedSessionMarksTheTaskFailed() {
        val overlaid = row(TaskDisplayState.Working).withSessionActivity(SessionActivity.Failed)

        assertEquals(TaskDisplayState.Failed, overlaid.displayState)
        assertEquals(TaskAttention.Failed, overlaid.attention)
    }

    @Test
    fun aCleanExitIsStoppedNeverDone() {
        val overlaid = row(TaskDisplayState.Working).withSessionActivity(SessionActivity.Exited)

        assertEquals(TaskDisplayState.Stopped, overlaid.displayState)
    }

    @Test
    fun decidedStatesAreNeverReplacedByTerminalOutput() {
        listOf(
            TaskDisplayState.Done,
            TaskDisplayState.NeedsInput,
            TaskDisplayState.NeedsApproval,
            TaskDisplayState.Failed,
        ).forEach { decided ->
            val original = row(decided)

            assertEquals(original, original.withSessionActivity(SessionActivity.Working))
        }
    }

    @Test
    fun onlyTasksWithASessionChange() {
        val state = ProjectTaskSidebarState(
            projects = listOf(
                ProjectRow(
                    id = "p1",
                    displayName = "Moru",
                    rootPath = null,
                    availabilityLabel = null,
                    isAvailable = true,
                    expanded = true,
                    selected = true,
                    tasks = listOf(
                        row(TaskDisplayState.Preparing, id = "with-session"),
                        row(TaskDisplayState.Preparing, id = "without-session"),
                    ),
                ),
            ),
            selectedProjectId = "p1",
            selectedTaskId = null,
        )

        val overlaid = state.withSessionActivities(mapOf("with-session" to SessionActivity.Working))
        val tasks = overlaid.projects.single().tasks.associateBy { task -> task.id }

        assertEquals(TaskDisplayState.Working, tasks.getValue("with-session").displayState)
        assertEquals(TaskDisplayState.Preparing, tasks.getValue("without-session").displayState)
    }

    @Test
    fun theLastPrintedLineIsLaidOverTheTaskThatOwnsTheSession() {
        val state = ProjectTaskSidebarState(
            projects = listOf(
                ProjectRow(
                    id = "p1", displayName = "Moru", rootPath = null, availabilityLabel = null,
                    isAvailable = true, expanded = true, selected = true,
                    tasks = listOf(row(TaskDisplayState.Working, id = "a"), row(TaskDisplayState.Working, id = "b")),
                ),
            ),
            selectedProjectId = "p1",
            selectedTaskId = null,
        )

        val tasks = state.withSessionActivities(emptyMap(), mapOf("a" to "Running tests"))
            .projects.single().tasks.associateBy { task -> task.id }

        assertEquals("Running tests", tasks.getValue("a").lastLine)
        assertEquals(null, tasks.getValue("b").lastLine)
    }
}
