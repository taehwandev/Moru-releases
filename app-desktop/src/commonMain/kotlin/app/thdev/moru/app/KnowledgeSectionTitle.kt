package app.thdev.moru.app

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

@Composable
internal fun KnowledgeSectionTitle(text: String) {
    MoruText(
        text = text,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
        color = MoruTheme.colors.textSecondary,
        style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
    )
}
