package app.thdev.moru.feature.notifications.components.feedback

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

@Composable
internal fun NotificationInboxProgress(loading: Boolean) {
    MoruText(
        text = if (loading) "Loading notifications…" else "Updating notifications…",
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp).testTag(if (loading) "notification-loading" else "notification-busy").semantics {
            liveRegion = LiveRegionMode.Polite
        },
        color = MoruTheme.colors.textSecondary,
        style = MoruTheme.typography.caption,
    )
}
