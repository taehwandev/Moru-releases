package app.thdev.moru.feature.terminal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

@Composable
internal fun TerminalAction(text: String, enabled: Boolean, onClick: () -> Unit, description: String = text) {
    Box(
        modifier = Modifier.height(22.dp)
            .semantics { contentDescription = description }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        MoruText(
            text = text,
            color = if (enabled) MoruTheme.colors.textSecondary else MoruTheme.colors.border,
            style = MoruTheme.typography.micro.copy(fontFamily = FontFamily.Monospace),
            maxLines = 1,
        )
    }
}
