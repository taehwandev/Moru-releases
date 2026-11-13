package app.thdev.moru.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.AppTheme
import app.thdev.moru.core.model.TaskSortOrder
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SettingsTaskSortOrderInteractionTest {
    @Test
    fun everyTaskOrderSavesAndReloadsWithoutOverwritingOtherPreferences() =
        runDesktopComposeUiTest(width = 1300, height = 900) {
            val repository = UiSettingsRepository().apply { stored = settingsWithOtherPreferences() }
            var applied by mutableStateOf(repository.stored)
            var generation by mutableStateOf(0)
            setContent {
                MoruTheme(darkTheme = false) {
                    key(generation) {
                        SettingsScreen(EmptyRuntimeStatus, repository, onSettingsSaved = { applied = it }, fontFamilies = emptyList())
                    }
                }
            }
            awaitSettingsText("Changes save automatically.")
            onNodeWithTag("settings-task-sort-order").performScrollTo()
                .assertContentDescriptionEquals("Task order")
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Oldest first"))
            onNodeWithText(
                "Order Tasks by creation time in all Projects. Oldest first puts new Tasks at the bottom. Dragging switches to Manual.",
            ).assertExists()
            val latest = repository.stored.let { initial ->
                initial.copy(
                    general = initial.general.copy(
                        rememberLastState = true,
                        projectOrder = listOf("project-b", "project-a"),
                        taskOrder = mapOf("project-a" to listOf("task-b", "task-a")),
                    ),
                    terminal = initial.terminal.copy(scrollbackLines = 23456),
                )
            }
            runOnIdle {
                assertEquals(TaskSortOrder.OldestFirst, repository.stored.general.taskSortOrder)
                assertEquals(0, repository.saveCount.get())
                repository.stored = latest
            }
            listOf(
                TaskSortOrder.NewestFirst to "Newest first",
                TaskSortOrder.Manual to "Manual",
                TaskSortOrder.OldestFirst to "Oldest first",
            ).forEach { (order, label) ->
                onNode(hasText(label) and hasAnyAncestor(hasTestTag("settings-task-sort-order"))).performScrollTo().performClick()
                val expected = latest.copy(general = latest.general.copy(taskSortOrder = order))
                waitUntil(timeoutMillis = 10_000) { applied == expected }
                awaitSettingsText("All changes saved.")
                runOnIdle {
                    assertEquals(expected, repository.loadSettings())
                    generation++
                }
                awaitSettingsText("Changes save automatically.")
                onNodeWithTag("settings-task-sort-order").performScrollTo()
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, label))
            }
            runOnIdle { assertEquals(3, repository.saveCount.get()) }
            captureSettings("settings-task-sort-order-light")
        }

    @Test
    fun failedTaskOrderSaveKeepsTheAppliedOrderAndRetryPreservesLatestPreferences() =
        runDesktopComposeUiTest(width = 1300, height = 900) {
            val initial = settingsWithOtherPreferences()
            val repository = UiSettingsRepository().apply {
                stored = initial
                failWrites = true
            }
            var applied by mutableStateOf(initial)
            setContent {
                MoruTheme(darkTheme = true) {
                    SettingsScreen(EmptyRuntimeStatus, repository, onSettingsSaved = { applied = it }, fontFamilies = emptyList())
                }
            }
            onNodeWithText("Newest first").performScrollTo().performClick()
            awaitSettingsText("Could not save changes: Disk unavailable")
            onNodeWithTag("settings-task-sort-order")
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Newest first"))
            runOnIdle {
                assertEquals(initial, applied)
                assertEquals(initial, repository.loadSettings())
                assertEquals(0, repository.saveCount.get())
            }
            captureSettings("settings-task-sort-order-failed-dark")
            val latest = initial.copy(
                general = initial.general.copy(
                    projectOrder = listOf("project-c", "project-a"),
                    taskOrder = mapOf("project-c" to listOf("task-c")),
                ),
                window = initial.window.copy(widthDp = 1730),
            )
            runOnIdle {
                repository.stored = latest
                repository.failWrites = false
            }
            onNodeWithTag("settings-save-retry").performClick()
            val expected = latest.copy(general = latest.general.copy(taskSortOrder = TaskSortOrder.NewestFirst))
            waitUntil(timeoutMillis = 10_000) { applied == expected }
            awaitSettingsText("All changes saved.")
            onNodeWithTag("settings-save-retry").assertDoesNotExist()
            runOnIdle {
                assertEquals(expected, repository.loadSettings())
                assertEquals(1, repository.saveCount.get())
            }
        }

    @Test
    fun autoCollapseInactiveSidebarDefaultsToOffAndTogglesInGeneralSettings() =
        runDesktopComposeUiTest(width = 1300, height = 900) {
            val repository = UiSettingsRepository()
            var applied by mutableStateOf(repository.stored)
            setContent {
                MoruTheme(darkTheme = false) {
                    SettingsScreen(EmptyRuntimeStatus, repository, onSettingsSaved = { applied = it }, fontFamilies = emptyList())
                }
            }
            awaitSettingsText("Changes save automatically.")
            onNodeWithTag("settings-auto-collapse-sidebar").performScrollTo().assertExists()
            runOnIdle {
                assertEquals(false, repository.stored.general.autoCollapseInactiveSidebar)
            }
            onNodeWithTag("settings-auto-collapse-sidebar").performClick()
            waitUntil(timeoutMillis = 10_000) { applied.general.autoCollapseInactiveSidebar }
            awaitSettingsText("All changes saved.")
            runOnIdle {
                assertEquals(true, repository.loadSettings().general.autoCollapseInactiveSidebar)
            }
            onNodeWithTag("settings-auto-collapse-sidebar").performClick()
            waitUntil(timeoutMillis = 10_000) { !applied.general.autoCollapseInactiveSidebar }
            awaitSettingsText("All changes saved.")
            runOnIdle {
                assertEquals(false, repository.loadSettings().general.autoCollapseInactiveSidebar)
            }
        }
}

private fun settingsWithOtherPreferences(): AppSettings = AppSettings().let { initial ->
    initial.copy(
        general = initial.general.copy(
            rememberLastState = false,
            projectOrder = listOf("project-a"),
            taskOrder = mapOf("project-a" to listOf("task-a")),
        ),
        appearance = initial.appearance.copy(theme = AppTheme.Dark, appleStyle = true),
        attention = initial.attention.copy(desktopNotifications = true),
        terminal = initial.terminal.copy(scrollbackLines = 12345),
    )
}
