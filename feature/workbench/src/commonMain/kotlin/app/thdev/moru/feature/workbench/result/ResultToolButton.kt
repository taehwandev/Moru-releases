package app.thdev.moru.feature.workbench.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.i18n.resultStrings

/**
 * One tool of the Result strip: its icon, filled while its view is open, with a dot when it has something to show
 * right now. It is named by its tab title for screen readers, since the icon alone carries no words.
 */
@Composable
internal fun ResultToolButton(tab: ResultTab, active: Boolean, hasContent: Boolean, onClick: () -> Unit) {
    val description = resultStrings().tab(tab)
    Box(
        Modifier.size(28.dp)
            .then(if (active) Modifier.ckSurface(MoruSurfaceRole.CardSelected) else Modifier)
            .selectable(active, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        MoruIcon(
            tab.icon(),
            size = 16.dp,
            tint = if (active) MoruTheme.colors.textPrimary else MoruTheme.colors.textSecondary,
        )
        if (hasContent) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(3.dp).size(5.dp).clip(CircleShape)
                    .background(MoruTheme.colors.primary),
            )
        }
    }
}

internal fun ResultTab.icon(): MoruIconName = when (this) {
    ResultTab.Code -> MoruIconName.Code
}
