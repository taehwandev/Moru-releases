package app.thdev.moru.app

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import app.thdev.moru.core.model.AppIcon

/** [icon]'s picture, for the app icon choice in Settings. */
@Composable
internal fun AppIconPreview(icon: AppIcon, modifier: Modifier) {
    val bitmap = remember(icon) { MoruDesktopRuntimeProperties.applicationIcon(icon).toComposeImageBitmap() }
    Image(bitmap = bitmap, contentDescription = null, modifier = modifier)
}
