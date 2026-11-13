package app.thdev.moru.feature.workbench.screenshot

import app.thdev.moru.core.model.TaskAttention
import app.thdev.moru.feature.workbench.ProjectRow
import app.thdev.moru.feature.workbench.ProjectTaskSidebarState
import app.thdev.moru.feature.workbench.TaskDisplayState
import app.thdev.moru.feature.workbench.TaskRow
import app.thdev.moru.feature.workbench.presentation

/**
 * Deterministic sample state for screenshot tests. It is test-only on purpose: fake
 * Task data must never be reachable from the shipped app.
 */
internal object AttentionFixtures {
    fun card(
        id: String,
        projectName: String,
        title: String,
        state: TaskDisplayState,
        selected: Boolean = false,
        hasWorkContext: Boolean = false,
    ): TaskRow {
        val presentation = state.presentation()
        return TaskRow(
            id = id,
            projectId = "project-$projectName",
            projectName = projectName,
            title = title,
            displayState = state,
            group = presentation.group,
            attention = state.attention(),
            hasWorkContext = hasWorkContext,
            selected = selected,
        )
    }

    /** One card per [TaskDisplayState], so a single image shows every state. */
    fun allStateCards(): List<TaskRow> =
        TaskDisplayState.entries.map { state ->
            card(
                id = "state-${state.name}",
                projectName = "sample-project",
                title = "Task in the ${state.name} state",
                state = state,
            )
        }

    fun longTextCard(): TaskRow =
        card(
            id = "long",
            projectName = "monorepo-with-an-extremely-long-project-name",
            title = "Verify payment event migrations, snapshot compatibility, rollback scenarios and legacy adapter removal",
            state = TaskDisplayState.NeedsInput,
            selected = true,
            hasWorkContext = true,
        )

    fun sidebarState(): ProjectTaskSidebarState {
        val billing = listOf(
            card("t1", "moru-billing-api", "Review cleanup before publishing", TaskDisplayState.NeedsApproval, selected = true),
        )
        val ide = listOf(
            card("t2", "Moru", "Two desktop tests failed", TaskDisplayState.Failed),
        )
        val mobile = listOf(
            card("t3", "mobile-checkout", "Session state could not be observed", TaskDisplayState.Unknown),
        )
        val docs = listOf(
            card("t4", "docs-site", "Repair API reference links", TaskDisplayState.Working, hasWorkContext = true),
        )
        val infra = listOf(
            card("t5", "infra-terraform", "Run terraform validate and tflint", TaskDisplayState.Checking),
        )
        val agentOs = listOf(
            card("t6", "moru-core", "Refine verification evidence", TaskDisplayState.Stopped),
        )
        val landing = listOf(
            card("t7", "landing-page", "Improve hero section accessibility", TaskDisplayState.Done),
        )
        return ProjectTaskSidebarState(
            projects = listOf(billing, ide, mobile, docs, infra, agentOs, landing).mapIndexed { index, tasks ->
                ProjectRow(
                    id = tasks.single().projectId,
                    displayName = tasks.single().projectName,
                    rootPath = "/work/${tasks.single().projectName}",
                    availabilityLabel = null,
                    isAvailable = true,
                    expanded = index == 0,
                    selected = index == 0,
                    tasks = tasks,
                )
            },
            selectedProjectId = billing.single().projectId,
            selectedTaskId = "t1",
        )
    }

    private fun TaskDisplayState.attention(): TaskAttention =
        when (this) {
            TaskDisplayState.NeedsApproval -> TaskAttention.NeedsApproval
            TaskDisplayState.NeedsInput -> TaskAttention.NeedsInput
            TaskDisplayState.Failed -> TaskAttention.Failed
            else -> TaskAttention.None
        }
}
