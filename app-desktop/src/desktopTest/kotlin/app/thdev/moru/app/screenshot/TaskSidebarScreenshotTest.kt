package app.thdev.moru.app.screenshot

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.app.DesktopWorkbenchSessionSlots
import app.thdev.moru.core.designsystem.theme.MoruColors
import app.thdev.moru.core.designsystem.theme.UiLanguage
import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.designsystem.theme.lightCkColors
import app.thdev.moru.feature.workbench.ClosedProjectRow
import app.thdev.moru.feature.workbench.ProjectTaskSidebar
import kotlin.test.Test

/** Pins project-first navigation alongside task states and long content. */
class TaskSidebarScreenshotTest {
    private val comparator = GoldenImageComparator()

    @Test
    fun projectSidebarInLightTheme() = assertSidebar("sidebar_groups_light", lightCkColors)

    @Test
    fun projectSidebarInDarkTheme() = assertSidebar("sidebar_groups_dark", darkCkColors)

    @Test
    fun projectSidebarOffersRecentlyClosedProjectsInKorean() =
        assertThemedScreenshot(comparator, "sidebar_closed_ko_light", lightCkColors, windowWidth = 300, windowHeight = 520, language = UiLanguage.Korean) {
            val base = AttentionFixtures.sidebarState()
            ProjectTaskSidebar(
                state = base.copy(
                    projects = base.projects.take(2).map { it.copy(expanded = false) },
                    closedProjects = listOf(ClosedProjectRow("n1", "notmid", "/work/notmid"), ClosedProjectRow("n2", "NotmidIosSample", "/work/NotmidIosSample")),
                ),
                onOpenProject = {}, onSelectProject = {}, onToggleProject = {}, onSelectTask = { _, _ -> }, onNewTask = {},
                modifier = Modifier.width(260.dp).height(520.dp),
                sessionSlots = DesktopWorkbenchSessionSlots,
            )
        }


    private fun assertSidebar(name: String, colors: MoruColors) =
        assertThemedScreenshot(comparator, name, colors) {
            ProjectTaskSidebar(
                state = AttentionFixtures.sidebarState(),
                onOpenProject = {},
                onSelectProject = {},
                onToggleProject = {},
                onSelectTask = { _, _ -> },
                onNewTask = {},
                modifier = Modifier.width(260.dp).height(760.dp),
                sessionSlots = DesktopWorkbenchSessionSlots,
            )
        }
}
