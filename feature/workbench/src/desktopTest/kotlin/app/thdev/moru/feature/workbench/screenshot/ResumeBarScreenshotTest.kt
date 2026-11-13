package app.thdev.moru.feature.workbench.screenshot

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.UiLanguage
import app.thdev.moru.core.designsystem.theme.lightCkColors
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import app.thdev.moru.feature.workbench.resume.AgentExit
import app.thdev.moru.feature.workbench.resume.AgentExitBar
import app.thdev.moru.feature.workbench.resume.ResumeBar
import kotlin.test.Test

/** Pins the one-tap resume row in both languages. */
class ResumeBarScreenshotTest {
    private val comparator = GoldenImageComparator()

    @Test
    fun resumeBarInEnglish() = assertBar("resume_bar_light", UiLanguage.English)

    @Test
    fun resumeBarInKorean() = assertBar("resume_bar_ko_light", UiLanguage.Korean)

    @Test
    fun resumeBarWithTheTasksOwnAgentFirst() =
        assertThemedScreenshot(comparator, "resume_bar_codex_light", lightCkColors, windowWidth = 520, windowHeight = 64) {
            ResumeBar(runtimes = listOf(AgentRuntime.Claude, AgentRuntime.Codex, AgentRuntime.Agy), onResume = {}, modifier = Modifier.width(520.dp), preferred = AgentRuntime.Codex)
        }

    @Test
    fun agentExitBarInEnglish() = assertExitBar("agent_exit_bar_light", UiLanguage.English)

    @Test
    fun agentExitBarInKorean() = assertExitBar("agent_exit_bar_ko_light", UiLanguage.Korean)

    private fun assertExitBar(name: String, language: UiLanguage) =
        assertThemedScreenshot(comparator, name, lightCkColors, windowWidth = 560, windowHeight = 72, language = language) {
            AgentExitBar(exit = AgentExit("s1", AgentRuntime.Codex, "codex --no-daemon resume x9"), onResume = {}, onDismiss = {}, modifier = Modifier.width(560.dp))
        }

    private fun assertBar(name: String, language: UiLanguage) =
        assertThemedScreenshot(comparator, name, lightCkColors, windowWidth = 520, windowHeight = 64, language = language) {
            ResumeBar(runtimes = listOf(AgentRuntime.Claude, AgentRuntime.Codex, AgentRuntime.Agy), onResume = {}, modifier = Modifier.width(520.dp))
        }
}
