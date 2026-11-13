package app.thdev.moru.feature.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonSize
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruSettingRow
import app.thdev.moru.core.designsystem.component.MoruSettingSection
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.AppShortcut
import app.thdev.moru.core.model.KeyboardSettings
import app.thdev.moru.core.model.ShortcutChord
import app.thdev.moru.feature.settings.i18n.settingsStrings

/**
 * Every app shortcut with the keys that trigger it. Clicking one waits for new keys; a chord another shortcut held
 * moves here and the move is named, so nothing is lost silently. [onRecording] is true while keys are awaited, so the
 * app's own shortcuts stand aside.
 */
@Composable
internal fun SettingsShortcutsSection(
    keyboard: KeyboardSettings,
    onChange: (edit: (AppSettings) -> AppSettings) -> Unit,
    onRecording: (Boolean) -> Unit,
) {
    val strings = settingsStrings()
    var recording by remember { mutableStateOf<AppShortcut?>(null) }
    var moved by remember { mutableStateOf<Pair<AppShortcut, AppShortcut>?>(null) }
    val waiting = recording != null
    DisposableEffect(waiting) {
        onRecording(waiting)
        onDispose { if (waiting) onRecording(false) }
    }
    fun assign(shortcut: AppShortcut, chord: ShortcutChord?) {
        val from = chord?.let { AppShortcut.entries.firstOrNull { other -> other != shortcut && keyboard.chordFor(other) == chord } }
        moved = from?.let { it to shortcut }
        onChange { settings -> settings.copy(keyboard = settings.keyboard.assign(shortcut, chord)) }
    }
    MoruSettingSection(
        title = strings.shortcutsTitle,
        description = strings.shortcutsDescription,
        icon = SettingsPage.Shortcuts.icon,
    ) {
        moved?.let { (from, to) ->
            MoruText(
                strings.shortcutMoved(strings.shortcutLabel(from), strings.shortcutLabel(to)),
                color = MoruTheme.colors.textSecondary,
                style = MoruTheme.typography.caption,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp).testTag("settings-shortcut-moved"),
            )
        }
        AppShortcut.entries.forEachIndexed { index, shortcut ->
            val chord = keyboard.chordFor(shortcut)
            MoruSettingRow(
                title = strings.shortcutLabel(shortcut),
                showDivider = index < AppShortcut.entries.lastIndex,
            ) {
                if (shortcut.id in keyboard.shortcuts) {
                    MoruButton(
                        strings.shortcutReset,
                        onClick = { assign(shortcut, shortcut.default) },
                        modifier = Modifier.testTag("settings-shortcut-reset-${shortcut.id}"),
                        type = MoruButtonType.GHOST,
                        size = MoruButtonSize.Small,
                    )
                }
                SettingsShortcutField(
                    shortcut = shortcut,
                    chord = chord,
                    recording = recording == shortcut,
                    onStart = { recording = shortcut },
                    onCancel = { recording = null },
                    onRecorded = { recorded -> recording = null; assign(shortcut, recorded) },
                )
            }
        }
    }
}
