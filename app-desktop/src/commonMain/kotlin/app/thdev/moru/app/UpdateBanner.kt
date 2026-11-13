package app.thdev.moru.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import app.thdev.moru.app.i18n.AppStrings
import app.thdev.moru.app.i18n.appStrings

/**
 * Non-modal feedback above native terminals. The updater owns visibility and expiry;
 * this popup never takes keyboard focus or changes the workspace's dimensions.
 */
@Composable
internal fun UpdateBanner(
    status: UpdateStatus,
    onRestart: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (updateMessage(appStrings(), status) == null) return
    val windowWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    val width = (windowWidth - 32.dp).coerceIn(1.dp, 420.dp)
    Popup(
        alignment = Alignment.TopEnd,
        properties = PopupProperties(focusable = false, dismissOnBackPress = false, dismissOnClickOutside = false),
    ) { UpdateSnackbarContent(status, onRestart, onDismiss, modifier, width) }
}

/** The banner's sentence for [status]; null when there is nothing to say. */
internal fun updateMessage(strings: AppStrings, status: UpdateStatus): String? = when (status) {
    UpdateStatus.Idle -> null
    UpdateStatus.Checking -> strings.updateChecking
    is UpdateStatus.UpToDate -> strings.updateUpToDate(status.version)
    is UpdateStatus.Downloading -> strings.updateDownloading(status.version, status.percent)
    is UpdateStatus.Ready -> strings.updateReady(status.version)
    is UpdateStatus.Installing -> strings.updateInstalling(status.version)
    is UpdateStatus.Failed -> strings.updateFailed(status.message)
}
