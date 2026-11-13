package app.thdev.moru.core.designsystem.component

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** A thin bar showing how far something is, from 0 to 1, such as a download. */
@Composable
fun MoruLinearProgress(progress: Float, modifier: Modifier = Modifier, color: Color = MoruTheme.colors.primary) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = color,
    )
}
