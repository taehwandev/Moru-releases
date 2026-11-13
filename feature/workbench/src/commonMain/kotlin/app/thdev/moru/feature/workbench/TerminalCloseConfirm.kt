package app.thdev.moru.feature.workbench

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruAlertDialog
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.i18n.workbenchStrings

/**
 * Asked before a terminal is closed from the keyboard, so a stray Cmd+W never stops a running agent: the window dims
 * behind a card in its middle that names [title]'s terminal and says how to bring it back. Pressing the [shortcut]
 * that asked once more, Enter or the button closes it; Escape, the other button or a click beside the card keeps it.
 */
@Composable
fun TerminalCloseConfirm(
    onConfirm: () -> Unit,
    onKeepOpen: () -> Unit,
    shortcut: String?,
    modifier: Modifier = Modifier,
    title: String? = null,
    reopenShortcut: String? = null,
) {
    val strings = workbenchStrings()
    MoruAlertDialog(
        onDismiss = onKeepOpen,
        modifier = modifier.testTag("terminal-close-alert"),
        onKey = { event ->
            if (event.type != KeyEventType.KeyDown) false else when (event.key) {
                Key.Escape -> { onKeepOpen(); true }
                Key.Enter, Key.NumPadEnter -> { onConfirm(); true }
                else -> false
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(MoruTheme.radius.medium)).background(MoruTheme.colors.warning.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                MoruIcon(MoruIconName.Terminal, size = 18.dp, tint = MoruTheme.colors.warning)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                MoruText(
                    strings.closeTerminalTitle(title),
                    style = MoruTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                    color = MoruTheme.colors.textPrimary,
                )
                MoruText(strings.closeTerminalPrompt, style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary)
            }
        }
        val hints = listOfNotNull(shortcut?.let(strings::pressAgainToClose), reopenShortcut?.let(strings::reopenAfterClose))
        if (hints.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            MoruText(
                hints.joinToString("  ·  "),
                Modifier.clip(RoundedCornerShape(MoruTheme.radius.medium)).background(MoruTheme.colors.textPrimary.copy(alpha = 0.05f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                style = MoruTheme.typography.caption,
                color = MoruTheme.colors.textSecondary,
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MoruButton(strings.keepOpen, onClick = onKeepOpen, type = MoruButtonType.GHOST)
            MoruButton(strings.closeAndStop, onClick = onConfirm, type = MoruButtonType.DANGER,
                modifier = Modifier.testTag("terminal-close-confirm"))
        }
    }
}
