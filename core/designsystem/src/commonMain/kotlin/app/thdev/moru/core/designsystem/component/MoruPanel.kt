package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole

@Composable
fun MoruPanel(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    var colModifier = modifier
        .fillMaxHeight()
        .ckSurface(MoruSurfaceRole.Panel)
        .padding(padding)

    if (width != null) {
        colModifier = colModifier.width(width)
    }

    Column(modifier = colModifier, content = content)
}
