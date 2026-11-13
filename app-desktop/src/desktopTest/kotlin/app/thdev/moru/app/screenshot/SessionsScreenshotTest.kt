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
import app.thdev.moru.feature.workbench.ProjectTaskSidebar
import kotlin.test.Test

/** Pins the sessions as the sidebar draws them under their Task, with every session state, in both themes. */
class SessionsScreenshotTest {
    private val comparator = GoldenImageComparator()

    @Test
    fun sessionsUnderTheirTaskInLightTheme() = assertTree("sessions_tree_light", lightCkColors)

    @Test
    fun sessionsUnderTheirTaskInDarkTheme() = assertTree("sessions_tree_dark", darkCkColors)

    @Test
    fun sessionsUnderTheirTaskInKorean() = assertTree("sessions_tree_ko_light", lightCkColors, UiLanguage.Korean)

    private fun assertTree(name: String, colors: MoruColors, language: UiLanguage = UiLanguage.English) =
        assertThemedScreenshot(comparator, name, colors, windowWidth = 300, windowHeight = 900, language = language) {
            ProjectTaskSidebar(
                state = AttentionFixtures.sidebarState(),
                onOpenProject = {},
                onSelectProject = {},
                onToggleProject = {},
                onSelectTask = { _, _ -> },
                onNewTask = {},
                modifier = Modifier.width(260.dp).height(860.dp),
                sessions = SessionsFixtures.rows(),
                sessionSlots = DesktopWorkbenchSessionSlots,
            )
        }
}
