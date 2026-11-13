package app.thdev.moru.feature.workbench.launch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruMenuRow
import app.thdev.moru.core.designsystem.component.MoruPopover
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.i18n.composerStrings

/**
 * What to start in another session of the same Task, as a small glass popover under the + button instead of a
 * screen that replaces the terminals. Picking one starts it at once; a launch error keeps the popover open beside
 * the choices so the user can retry. The displayed number keys work only in this popover, without modifiers. An
 * outside click or Escape closes it. Place it inside the anchor's `Box`.
 */
@Composable
fun NewSessionPopover(
    expanded: Boolean,
    errorMessage: String?,
    onStart: (AgentRuntime) -> Unit,
    onDismiss: () -> Unit,
    /** Opens towards the start of the anchor, for a + at the end of a bar; otherwise it opens from the anchor's start. */
    alignEnd: Boolean = false,
) {
    val strings = composerStrings()
    MoruPopover(
        expanded = expanded,
        onDismiss = onDismiss,
        modifier = Modifier.width(240.dp),
        onPreviewKeyEvent = newSessionShortcuts(expanded, onStart),
        alignEnd = alignEnd,
    ) {
        // The native popup window ignores padding outside its surface, so the insets live inside it.
        Column(Modifier.padding(6.dp)) {
            MoruText(
                strings.headingTitle,
                // Starts where the row labels do, past the rows' leading icon column.
                modifier = Modifier.padding(start = 30.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                style = MoruTheme.typography.caption,
                color = MoruTheme.colors.textSecondary,
                maxLines = 1,
            )
            AgentRuntime.entries.forEachIndexed { index, runtime ->
                Box(Modifier.fillMaxWidth()) {
                    MoruMenuRow(runtime.displayName, onClick = { onStart(runtime) })
                    MoruText(
                        (index + 1).toString(),
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                        style = MoruTheme.typography.caption,
                        color = MoruTheme.colors.textSecondary,
                    )
                }
            }
            errorMessage?.let {
                MoruText(
                    it,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    color = MoruTheme.colors.error,
                    style = MoruTheme.typography.caption,
                )
            }
        }
    }
}

private fun newSessionShortcuts(
    expanded: Boolean,
    onStart: (AgentRuntime) -> Unit,
): (KeyEvent) -> Boolean =
    { event ->
        val start: (() -> Unit)? = when (event.key) {
            Key.One, Key.NumPad1 -> { { onStart(AgentRuntime.Claude) } }
            Key.Two, Key.NumPad2 -> { { onStart(AgentRuntime.Codex) } }
            Key.Three, Key.NumPad3 -> { { onStart(AgentRuntime.Agy) } }
            Key.Four, Key.NumPad4 -> { { onStart(AgentRuntime.Terminal) } }
            else -> null
        }
        if (!expanded || start == null || event.isAltPressed || event.isCtrlPressed ||
            event.isMetaPressed || event.isShiftPressed
        ) {
            false
        } else {
            // Consume both halves; release launches once even if a held key repeats while showing a launch error.
            if (event.type == KeyEventType.KeyUp) start()
            true
        }
    }
