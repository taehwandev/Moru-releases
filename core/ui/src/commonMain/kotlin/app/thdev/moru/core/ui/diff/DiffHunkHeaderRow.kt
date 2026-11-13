package app.thdev.moru.core.ui.diff

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

private const val TINT_ALPHA = 0.10f

/** The `@@ -a,b +c,d @@ section` line that starts a hunk, so the reader knows where in the file they are. */
@Composable
internal fun DiffHunkHeaderRow(header: String, modifier: Modifier = Modifier) {
    MoruText(
        text = header,
        style = MoruTheme.typography.monospace,
        color = MoruTheme.colors.textSecondary,
        modifier = modifier
            .fillMaxWidth()
            .background(MoruTheme.colors.primary.copy(alpha = TINT_ALPHA))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
