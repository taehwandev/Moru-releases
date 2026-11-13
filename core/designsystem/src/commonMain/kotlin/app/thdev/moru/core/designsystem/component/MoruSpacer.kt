package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MoruSpacer(size: Dp, horizontal: Boolean = false) {
    if (horizontal) {
        Spacer(modifier = Modifier.width(size))
    } else {
        Spacer(modifier = Modifier.height(size))
    }
}

object MoruSpacers {
    val Tiny = 4.dp
    val Small = 8.dp
    val Medium = 16.dp
    val Large = 24.dp
    val XLarge = 32.dp
}
