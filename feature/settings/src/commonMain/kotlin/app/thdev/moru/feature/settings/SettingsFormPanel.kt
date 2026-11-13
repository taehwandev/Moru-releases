package app.thdev.moru.feature.settings

import app.thdev.moru.core.model.AppIcon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruPanel
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.MoruVerticalDivider
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.settings.i18n.settingsStrings
import app.thdev.moru.core.model.terminal.TerminalProfileImport

/** Settings as a list of pages on the left and the chosen page on the right, under one save status. */
@Composable
internal fun SettingsFormPanel(
    editor: SettingsEditorState,
    page: SettingsPage,
    onPageChange: (SettingsPage) -> Unit,
    fontFamilies: List<String>,
    updateState: SettingsUpdateState,
    onCheckForUpdates: () -> Unit,
    onRestartForUpdate: () -> Unit,
    agentClis: AgentCliState,
    modifier: Modifier = Modifier,
    appIconPreview: (@Composable (AppIcon, Modifier) -> Unit)? = null,
    onShowOnboarding: (() -> Unit)? = null,
    onRecordingShortcut: (Boolean) -> Unit = {},
    importTerminalProfile: suspend () -> TerminalProfileImport = { TerminalProfileImport.NotFound },
) {
    val strings = settingsStrings()
    MoruPanel(modifier = modifier, padding = 16.dp) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            MoruText(strings.title, style = MoruTheme.typography.h1)
            MoruText(strings.subtitle, color = MoruTheme.colors.textSecondary, style = MoruTheme.typography.caption)
            SettingsSaveFeedback(editor.saveStatus, editor.saveError, editor.hasInvalidNumbers, editor::retry)
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(
                    modifier = Modifier.width(196.dp).fillMaxHeight().verticalScroll(rememberScrollState()).testTag("settings-pages"),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    SettingsPage.entries.forEach { entry ->
                        SettingsPageRow(
                            entry.title(strings), entry.icon, selected = entry == page, onClick = { onPageChange(entry) },
                            modifier = Modifier.testTag("settings-page-${entry.name}"),
                        )
                    }
                }
                MoruVerticalDivider()
                // Each page starts at its top rather than at the scroll position the previous page left.
                key(page) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(end = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        when (page) {
                            SettingsPage.General -> SettingsGeneralSection(editor.settings, editor::change, onShowOnboarding)
                            SettingsPage.Appearance -> SettingsAppearanceSection(
                                editor.settings, editor::change, editor.uiFontSizeText, editor::changeUiFontSize, fontFamilies,
                                appIconPreview,
                            )
                            SettingsPage.Terminal -> {
                                SettingsTerminalSection(
                                    editor.settings, editor::change, editor.scrollbackText, editor::changeScrollback,
                                    editor.fontSizeText, editor::changeFontSize, fontFamilies,
                                )
                                SettingsTerminalLookSection(editor.settings, editor::change, importTerminalProfile)
                            }
                            SettingsPage.Attention -> SettingsAttentionSection(editor.settings, editor::change)
                            SettingsPage.Shortcuts -> SettingsShortcutsSection(editor.settings.keyboard, editor::change, onRecordingShortcut)
                            SettingsPage.Updates -> {
                                SettingsAgentCliSection(agentClis)
                            }
                        }
                    }
                }
            }
        }
    }
}
