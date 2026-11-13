package app.thdev.moru.feature.workbench

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme

@Composable
internal fun QuickOpenRow(
    path: String,
    selected: Boolean,
    onOpenFile: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .ckSurface(if (selected) MoruSurfaceRole.RowSelected else MoruSurfaceRole.Row)
            .clickable { onOpenFile(path) }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoruText(
            text = path.substringAfterLast('/'),
            color = MoruTheme.colors.textPrimary,
            style = MoruTheme.typography.caption.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
        )
        MoruText(
            text = path,
            modifier = Modifier.weight(1f),
            color = MoruTheme.colors.textSecondary,
            style = MoruTheme.typography.caption.copy(fontFamily = FontFamily.Monospace),
            maxLines = 1,
        )
    }
}
