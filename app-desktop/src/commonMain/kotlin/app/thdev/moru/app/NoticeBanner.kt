package app.thdev.moru.app

import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruIcon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.app.i18n.appStrings
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** A one-line message the user can dismiss, for something that happened that they did not ask about directly. */
@Composable
internal fun NoticeBanner(
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().ckSurface(MoruSurfaceRole.Card).padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MoruIcon(name = MoruIconName.Flag, tint = MoruTheme.colors.warning)
            MoruText(text = text, style = MoruTheme.typography.caption, color = MoruTheme.colors.textPrimary)
        }
        MoruButton(text = appStrings().dismiss, onClick = onDismiss, type = MoruButtonType.GHOST)
    }
}
