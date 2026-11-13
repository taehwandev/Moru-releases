package app.thdev.moru.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruDivider
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonSize
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruSettingRow
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.runtime.AgentCli
import app.thdev.moru.core.runtime.CliUpdateOutcome
import app.thdev.moru.feature.settings.i18n.settingsStrings

/**
 * One installed agent CLI: its name and version, where it was found, the line Moru runs to update it the way it was
 * installed with a button for it, and what the last update did. [view] is null until its version has been read.
 */
@Composable
internal fun AgentCliRow(cli: AgentCli, view: CliUpdateView?, onUpdate: () -> Unit, showDivider: Boolean) {
    val strings = settingsStrings()
    val name = agentCliName(cli.provider)
    Column(modifier = Modifier.testTag("settings-agent-cli-${cli.provider}")) {
        MoruSettingRow(
            title = view?.installed?.let { version -> "$name $version" } ?: name,
            description = cli.path,
            showDivider = false,
        ) {
            if (view?.command != null) {
                MoruButton(
                    text = if (view.running) strings.cliUpdating else strings.cliUpdate,
                    onClick = onUpdate,
                    type = MoruButtonType.SECONDARY,
                    enabled = !view.running,
                    size = MoruButtonSize.Small,
                )
            }
        }
        if (view != null) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp)) {
                MoruText(
                    text = view.command ?: strings.cliUpdatedOutsideMoru,
                    color = MoruTheme.colors.textSecondary,
                    style = if (view.command != null) MoruTheme.typography.caption.copy(fontFamily = FontFamily.Monospace)
                        else MoruTheme.typography.caption,
                )
                view.outcome?.let { outcome ->
                    MoruText(
                        text = when (outcome) {
                            is CliUpdateOutcome.Updated -> strings.cliUpdated(outcome.to)
                            is CliUpdateOutcome.Unchanged -> strings.cliAlreadyLatest(outcome.version ?: "—")
                            is CliUpdateOutcome.Failed -> strings.cliUpdateFailed(outcome.detail, outcome.command)
                        },
                        color = if (outcome is CliUpdateOutcome.Failed) MoruTheme.colors.error else MoruTheme.colors.textSecondary,
                        style = MoruTheme.typography.caption,
                    )
                }
            }
        }
        if (showDivider) {
            MoruDivider(modifier = Modifier.padding(horizontal = 14.dp))
        }
    }
}
