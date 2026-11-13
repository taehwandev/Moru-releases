package app.thdev.moru.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonSize
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruSettingRow
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.terminal.TerminalProfileImport
import app.thdev.moru.feature.settings.i18n.settingsStrings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Imports the iTerm2 default profile once and says what was taken and what Moru cannot draw. */
@Composable
internal fun SettingsTerminalImportRow(
    settings: AppSettings,
    onChange: (edit: (AppSettings) -> AppSettings) -> Unit,
    importTerminalProfile: suspend () -> TerminalProfileImport,
) {
    val strings = settingsStrings()
    val scope = rememberCoroutineScope()
    var importing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<TerminalProfileImport?>(null) }
    val profile = settings.terminal.profile
    MoruSettingRow(title = strings.terminalImportTitle, description = strings.terminalImportDescription) {
        Column(modifier = Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MoruButton(
                text = if (importing) strings.terminalImporting else strings.terminalImportAction,
                onClick = {
                    importing = true
                    scope.launch {
                        val imported = try {
                            importTerminalProfile()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            TerminalProfileImport.Failed(error.message ?: strings.unknownError)
                        } finally {
                            importing = false
                        }
                        result = imported
                        if (imported is TerminalProfileImport.Imported) onChange { it.withTerminalImport(imported) }
                    }
                },
                enabled = !importing,
                type = MoruButtonType.SECONDARY, size = MoruButtonSize.Small,
                modifier = Modifier.testTag("settings-terminal-import"),
            )
            val lines = when (val shown = result) {
                is TerminalProfileImport.Imported -> listOfNotNull(
                    shown.profile.source?.let(strings::terminalImportUsing),
                    shown.applied.takeIf { it.isNotEmpty() }
                        ?.let { items -> strings.terminalImportApplied(items.joinToString { strings.terminalImportItem(it) }) },
                    shown.skipped.takeIf { it.isNotEmpty() }
                        ?.let { items -> strings.terminalImportSkipped(items.joinToString { strings.terminalImportItem(it) }) },
                )
                TerminalProfileImport.NotFound -> listOf(strings.terminalImportNotFound)
                is TerminalProfileImport.Failed -> listOf(strings.terminalImportFailed(shown.reason))
                null -> listOfNotNull(profile?.source?.let(strings::terminalImportUsing))
            }
            lines.forEach { line ->
                MoruText(
                    line, color = MoruTheme.colors.textSecondary, style = MoruTheme.typography.caption,
                    modifier = Modifier.testTag("settings-terminal-import-report"),
                )
            }
        }
    }
}
