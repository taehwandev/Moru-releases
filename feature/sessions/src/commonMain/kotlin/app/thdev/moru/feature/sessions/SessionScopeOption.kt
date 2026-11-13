package app.thdev.moru.feature.sessions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme

@Composable
internal fun SessionScopeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .ckSurface(if (selected) MoruSurfaceRole.CardSelected else MoruSurfaceRole.Control)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = MoruTheme.spacing.small, vertical = MoruTheme.spacing.xSmall),
    ) {
        MoruText(text = label, style = MoruTheme.typography.caption, maxLines = 1)
    }
}
