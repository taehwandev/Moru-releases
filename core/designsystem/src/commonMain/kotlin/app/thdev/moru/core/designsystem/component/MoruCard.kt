package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme

@Composable
fun MoruCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val style = MoruTheme.surfaceStyle(MoruSurfaceRole.Card)
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(style.radius),
        border = BorderStroke(style.borderWidth, style.border),
        colors = CardDefaults.cardColors(
            containerColor = style.background,
            contentColor = style.content
        ),
        content = content
    )
}
