package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** A thin horizontal rule in the border colour that separates rows in a list. */
@Composable
fun MoruDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(MoruTheme.stroke.thin)
            .background(MoruTheme.colors.border),
    )
}
