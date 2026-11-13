package app.thdev.moru.feature.workbench.screenshot

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruColors
import app.thdev.moru.core.designsystem.theme.UiLanguage
import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.designsystem.theme.lightCkColors
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import app.thdev.moru.feature.workbench.launch.NewTaskComposer
import kotlin.test.Test

/** Pins the one-step task entry: four launch buttons, with the last used agent marked. */
class NewTaskComposerScreenshotTest {
    private val comparator = GoldenImageComparator()

    @Test
    fun composerInLightTheme() = assertComposer("new_task_composer_light", lightCkColors)

    @Test
    fun composerInDarkTheme() = assertComposer("new_task_composer_dark", darkCkColors)

    @Test
    fun composerInKorean() = assertComposer("new_task_composer_ko_light", lightCkColors, language = UiLanguage.Korean)

    @Test
    fun composerMarksTheAgentUsedLastAndStillStartsAtOnePress() =
        assertThemedScreenshot(comparator, "new_task_composer_last_agent_light", lightCkColors, windowWidth = 800, windowHeight = 300) {
            NewTaskComposer(
                onStart = {}, onCancel = {},
                modifier = Modifier.width(800.dp).height(300.dp),
                lastRuntime = AgentRuntime.Codex,
            )
        }

    @Test
    fun composerInANarrowWindowKeepsEveryButtonReachable() =
        assertThemedScreenshot(comparator, "new_task_composer_narrow_light", lightCkColors, windowWidth = 380, windowHeight = 740) {
            NewTaskComposer(
                onStart = {}, onCancel = {},
                modifier = Modifier.width(380.dp).height(740.dp),
            )
        }

    @Test
    fun composerShowsPreparationFailureWithoutDiscardingContext() =
        assertThemedScreenshot(comparator, "new_task_composer_error_light", lightCkColors, windowWidth = 800, windowHeight = 650) {
            NewTaskComposer(
                onStart = {}, onCancel = {},
                modifier = Modifier.width(800.dp).height(650.dp),
                errorMessage = "Unable to start the terminal. Check the shell setting and try again.",
            )
        }

    private fun assertComposer(
        name: String,
        colors: MoruColors,
        language: UiLanguage = UiLanguage.English,
    ) = assertThemedScreenshot(comparator, name, colors, windowWidth = 800, windowHeight = 650, language = language) {
        NewTaskComposer(
            onStart = {},
            onCancel = {},
            modifier = Modifier.width(800.dp).height(650.dp),
        )
    }
}
