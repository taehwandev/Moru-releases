package app.thdev.moru.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruSettingRow
import app.thdev.moru.core.designsystem.component.MoruSettingSection
import app.thdev.moru.feature.settings.i18n.settingsStrings

/** The agent CLIs Moru found on this machine, each with its version and an update that keeps its install's channel. */
@Composable
internal fun SettingsAgentCliSection(state: AgentCliState) {
    val strings = settingsStrings()
    val clis = state.clis
    MoruSettingSection(
        title = strings.agentClisTitle,
        description = strings.agentClisDescription,
        icon = MoruIconName.Bot,
        modifier = Modifier.testTag("settings-agent-clis"),
    ) {
        when {
            clis == null -> MoruSettingRow(title = strings.agentClisLooking, showDivider = false)
            clis.isEmpty() -> MoruSettingRow(
                title = strings.noAgentCliTitle,
                description = strings.noAgentCliDescription,
                showDivider = false,
            )
            else -> clis.forEachIndexed { index, cli ->
                AgentCliRow(
                    cli = cli,
                    view = state.views[cli.provider],
                    onUpdate = { state.update(cli.provider) },
                    showDivider = index < clis.lastIndex,
                )
            }
        }
    }
}
