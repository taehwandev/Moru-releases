package app.thdev.moru.app.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import app.thdev.moru.app.DesktopWorkbenchSessionSlots
import app.thdev.moru.core.designsystem.theme.MoruColors
import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.designsystem.theme.lightCkColors
import app.thdev.moru.feature.workbench.ProjectTaskSidebar
import kotlin.test.Test

class TaskGroupingScreenshotTest {
    private val comparator = GoldenImageComparator()

    @Test fun repeatedTaskTitlesInDarkTheme() = assertGroups("task_identity_dark", darkCkColors, 300)
    @Test fun repeatedTaskTitlesInLightTheme() = assertGroups("task_identity_light", lightCkColors, 300)
    @Test fun longMixedLanguageTitlesInNarrowSidebar() = assertGroups("task_identity_narrow", darkCkColors, 236)

    private fun assertGroups(name: String, colors: MoruColors, width: Int) =
        assertThemedScreenshot(comparator, name, colors, windowWidth = width, windowHeight = 780) {
            ProjectTaskSidebar(
                state = TaskGroupingFixtures.state(), onOpenProject = {}, onSelectProject = {}, onToggleProject = {},
                onSelectTask = { _, _ -> }, onNewTask = {}, modifier = Modifier.fillMaxSize(),
                sessions = TaskGroupingFixtures.sessions(),
                sessionSlots = DesktopWorkbenchSessionSlots,
            )
        }
}
