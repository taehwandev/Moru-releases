package app.thdev.moru.feature.sessions

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** The one quiet line an empty board column shows. */
@Composable
internal fun SessionBoardEmpty(text: String) {
    MoruText(text, Modifier.padding(horizontal = 12.dp, vertical = 2.dp), style = MoruTheme.typography.caption,
        color = MoruTheme.colors.textSecondary)
}
