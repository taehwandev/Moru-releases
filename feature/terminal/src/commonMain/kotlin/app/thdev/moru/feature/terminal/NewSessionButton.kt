package app.thdev.moru.feature.terminal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** The way to add a session to the tabs, drawn as a real control with its shortcut beside it so it is found at a glance. */
@Composable
internal fun NewSessionButton(enabled: Boolean, onClick: () -> Unit) {
    // Disabled stays readable, only dimmer: the border colour it used before nearly vanished into the tab bar.
    val tint = if (enabled) MoruTheme.colors.textPrimary else MoruTheme.colors.textSecondary.copy(alpha = DisabledAlpha)
    Row(
        modifier = Modifier.height(26.dp)
            .ckSurface(MoruSurfaceRole.Control)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            // The shortcut label is for the eye; screen readers and tests hear the button's name alone.
            .clearAndSetSemantics {
                contentDescription = "New session"
                role = Role.Button
                if (!enabled) disabled()
            }
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoruIcon(name = MoruIconName.Plus, size = 16.dp, tint = tint)
        MoruText(
            text = "New session",
            color = tint,
            style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
        )
        MoruText(
            text = "⌘T",
            color = if (enabled) MoruTheme.colors.textSecondary else MoruTheme.colors.textSecondary.copy(alpha = DisabledAlpha),
            style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
        )
    }
}

private const val DisabledAlpha = 0.55f
