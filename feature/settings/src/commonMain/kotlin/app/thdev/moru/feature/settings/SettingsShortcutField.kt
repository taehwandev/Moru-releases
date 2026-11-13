package app.thdev.moru.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.AppShortcut
import app.thdev.moru.core.model.ShortcutChord
import app.thdev.moru.core.ui.shortcut.ShortcutKeys
import app.thdev.moru.core.ui.shortcut.label
import app.thdev.moru.feature.settings.i18n.settingsStrings

/**
 * The keys of one shortcut, drawn like a key cap. Clicked, it waits for a new chord: the platform modifier (Command
 * on macOS, Control elsewhere) with a key, optionally with Shift or Option. Delete clears it; Escape or clicking away
 * keeps the old one.
 */
@Composable
internal fun SettingsShortcutField(
    shortcut: AppShortcut,
    chord: ShortcutChord?,
    recording: Boolean,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onRecorded: (ShortcutChord?) -> Unit,
) {
    val strings = settingsStrings()
    val colors = MoruTheme.colors
    val focus = remember { FocusRequester() }
    // A chord the terminal or the system keeps; shown until other keys are pressed.
    var refused by remember { mutableStateOf<ShortcutChord?>(null) }
    LaunchedEffect(recording) {
        refused = null
        if (recording) focus.requestFocus()
    }
    val shape = RoundedCornerShape(MoruTheme.radius.small)
    val text = when {
        recording -> refused?.let { strings.shortcutReserved(it.label()) } ?: strings.shortcutWaiting
        chord != null -> chord.label()
        else -> strings.shortcutNone
    }
    Box(
        modifier = Modifier.widthIn(min = 96.dp).heightIn(min = 26.dp)
            .background(if (recording) colors.primary.copy(alpha = 0.14f) else colors.surface, shape)
            .border(MoruTheme.stroke.thin, if (recording) colors.primary else colors.border, shape)
            .focusRequester(focus)
            .onFocusChanged { state -> if (recording && !state.hasFocus) onCancel() }
            .onPreviewKeyEvent { event ->
                if (!recording) return@onPreviewKeyEvent false
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent true
                when (event.key) {
                    Key.Escape -> {
                        onCancel()
                        return@onPreviewKeyEvent true
                    }
                    Key.Backspace, Key.Delete -> {
                        onRecorded(null)
                        return@onPreviewKeyEvent true
                    }
                }
                val name = ShortcutKeys.name(event.key)
                val platform = if (ShortcutKeys.macOs) event.isMetaPressed else event.isCtrlPressed
                if (name != null && platform) {
                    val pressed = ShortcutChord(name, shift = event.isShiftPressed, alt = event.isAltPressed)
                    if (pressed in ShortcutChord.Reserved) refused = pressed else onRecorded(pressed)
                }
                true
            }
            .clickable(role = Role.Button) { if (recording) onCancel() else onStart() }
            .semantics { contentDescription = strings.shortcutLabel(shortcut) }
            .testTag("settings-shortcut-${shortcut.id}")
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        MoruText(
            text,
            color = if (chord == null && !recording) colors.textSecondary else colors.textPrimary,
            style = MoruTheme.typography.body,
        )
    }
}
