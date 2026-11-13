package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme

@Composable
fun MoruTerminalCursor(
    modifier: Modifier = Modifier,
    width: Dp = 8.dp,
    height: Dp = 18.dp,
) {
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .background(MoruTheme.colors.primary),
    )
}
