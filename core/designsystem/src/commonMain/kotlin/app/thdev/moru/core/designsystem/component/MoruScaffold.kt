package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme

@Composable
fun MoruScaffold(
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit)? = null,
    sideBar: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    MoruTheme {
        Column(modifier = modifier.fillMaxSize().ckSurface(MoruSurfaceRole.Page)) {
            if (topBar != null) {
                Box(modifier = Modifier) {
                    topBar()
                }
            }
            Row(modifier = Modifier.weight(1f)) {
                if (sideBar != null) {
                    Box(modifier = Modifier.ckSurface(MoruSurfaceRole.Panel)) {
                        sideBar()
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    content()
                }
            }
        }
    }
}
