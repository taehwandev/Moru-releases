package app.thdev.moru.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.moruGlassSurface
import app.thdev.moru.core.designsystem.theme.MoruGlassRole

internal object MoruChrome {
    @Composable
    fun PrimaryRail(
        currentScreen: MoruDestination,
        onSelect: (MoruDestination) -> Unit,
        topInset: Dp = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight()
                .moruGlassSurface(MoruGlassRole.Navigation)
                .padding(top = topInset + 6.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MoruDestination.activityBarDestinations.forEach { destination ->
                RailItem(
                    destination = destination,
                    selected = destination == currentScreen,
                    onClick = { onSelect(destination) },
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
