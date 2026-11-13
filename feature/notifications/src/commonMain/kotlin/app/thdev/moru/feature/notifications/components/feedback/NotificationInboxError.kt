package app.thdev.moru.feature.notifications.components.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruSurfaceColumn
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme

@Composable
internal fun NotificationInboxError(message: String, retryEnabled: Boolean, onRetry: () -> Unit) {
    MoruSurfaceColumn(
        role = MoruSurfaceRole.Warning,
        modifier = Modifier.fillMaxWidth().testTag("notification-error").semantics {
            liveRegion = LiveRegionMode.Polite
        },
        verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small),
    ) {
        MoruText("Could not update notification history", style = MoruTheme.typography.h2)
        MoruText(message, modifier = Modifier.testTag("notification-error-message"))
        MoruButton(
            text = "Retry",
            onClick = onRetry,
            modifier = Modifier.testTag("notification-retry"),
            type = MoruButtonType.SECONDARY,
            enabled = retryEnabled,
        )
    }
}
