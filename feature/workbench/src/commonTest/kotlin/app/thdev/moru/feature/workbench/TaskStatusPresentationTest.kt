package app.thdev.moru.feature.workbench

import app.thdev.moru.core.designsystem.theme.UiLanguage
import app.thdev.moru.core.model.TaskStatus
import app.thdev.moru.feature.workbench.i18n.workbenchStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TaskStatusPresentationTest {
    @Test
    fun everyTaskStatusMapsToTheDisplayStateWithTheSameName() {
        TaskStatus.entries.forEach { status ->
            assertEquals(status.name, status.toDisplayState().name)
        }
    }

    @Test
    fun displayStatesAreTaskStatusesPlusUnknownAndWaiting() {
        assertEquals(TaskStatus.entries.size + 2, TaskDisplayState.entries.size)
        assertTrue(TaskDisplayState.Unknown in TaskDisplayState.entries)
        assertTrue(TaskDisplayState.Waiting in TaskDisplayState.entries)
    }

    @Test
    fun everyStateHasNonBlankLabelAndIconInEveryLanguage() {
        TaskDisplayState.entries.forEach { state ->
            val presentation = state.presentation()
            UiLanguage.entries.forEach { language ->
                assertTrue(workbenchStrings(language).statusLabel(state).isNotBlank(), "label for $state in $language")
            }
            assertNotNull(presentation.icon, "icon for $state")
        }
    }

    @Test
    fun statesAreNeverDistinguishedByColorAlone() {
        val icons = TaskDisplayState.entries.map { it.presentation().icon }
        assertEquals(icons.size, icons.toSet().size, "each state needs its own icon")
        UiLanguage.entries.forEach { language ->
            val labels = TaskDisplayState.entries.map { workbenchStrings(language).statusLabel(it) }
            assertEquals(labels.size, labels.toSet().size, "each state needs its own label in $language")
        }
    }

    @Test
    fun stoppedIsNeverShownAsDone() {
        val stopped = TaskDisplayState.Stopped.presentation()
        val done = TaskDisplayState.Done.presentation()
        assertNotEquals(done.group, stopped.group)
        UiLanguage.entries.forEach { language ->
            val strings = workbenchStrings(language)
            assertNotEquals(strings.statusLabel(TaskDisplayState.Done), strings.statusLabel(TaskDisplayState.Stopped))
        }
    }

    @Test
    fun unobservableTaskIsNeedsAttentionAndNeverFinishedOrFailed() {
        val unknown = TaskDisplayState.Unknown.presentation()
        assertEquals(TaskGroup.NeedsAttention, unknown.group)
        assertTrue(unknown.dashedOutline)
        UiLanguage.entries.forEach { language ->
            val strings = workbenchStrings(language)
            val unknownLabel = strings.statusLabel(TaskDisplayState.Unknown)
            assertNotEquals(strings.statusLabel(TaskDisplayState.Done), unknownLabel)
            assertNotEquals(strings.statusLabel(TaskDisplayState.Failed), unknownLabel)
        }
    }

    @Test
    fun statesThatNeedTheUserGroupFirst() {
        listOf(
            TaskDisplayState.NeedsInput,
            TaskDisplayState.NeedsApproval,
            TaskDisplayState.Failed,
            TaskDisplayState.Unknown,
        ).forEach { state ->
            assertEquals(TaskGroup.NeedsAttention, state.presentation().group, "$state")
        }
    }

    @Test
    fun onlyActiveWorkPulsesAndItGroupsAsRunning() {
        val pulsing = TaskDisplayState.entries.filter { it.presentation().pulse }
        assertEquals(setOf(TaskDisplayState.Working, TaskDisplayState.Checking), pulsing.toSet())
        pulsing.forEach { assertEquals(TaskGroup.Running, it.presentation().group) }
    }

    @Test
    fun groupDeclarationOrderPutsAttentionFirstAndDoneLast() {
        assertEquals(
            listOf(TaskGroup.NeedsAttention, TaskGroup.Running, TaskGroup.Review, TaskGroup.Done),
            TaskGroup.entries,
        )
    }
}
