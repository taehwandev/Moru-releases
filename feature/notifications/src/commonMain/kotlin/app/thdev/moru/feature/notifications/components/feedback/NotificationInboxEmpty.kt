package app.thdev.moru.feature.notifications.components.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

@Composable
internal fun NotificationInboxEmpty() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp).testTag("notification-empty"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MoruIcon(name = MoruIconName.Bell, size = 18.dp, tint = MoruTheme.colors.textSecondary)
        MoruText("No notifications yet", style = MoruTheme.typography.body.copy(fontWeight = FontWeight.Medium))
        MoruText("New task updates will appear here.", style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary)
    }
}
