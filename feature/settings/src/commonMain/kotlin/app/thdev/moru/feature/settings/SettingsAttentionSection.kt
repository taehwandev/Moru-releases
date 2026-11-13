package app.thdev.moru.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruSettingSection
import app.thdev.moru.core.designsystem.component.MoruSettingSwitchRow
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.feature.settings.i18n.settingsStrings

@Composable
internal fun SettingsAttentionSection(
    settings: AppSettings,
    onChange: (edit: (AppSettings) -> AppSettings) -> Unit,
) {
    val strings = settingsStrings()
    MoruSettingSection(
        title = strings.attentionTitle,
        description = strings.attentionDescription,
        icon = MoruIconName.Bell,
    ) {
        MoruSettingSwitchRow(
            title = strings.desktopNotificationsTitle,
            description = strings.desktopNotificationsDescription,
            checked = settings.attention.desktopNotifications,
            onCheckedChange = { onChange { s -> s.copy(attention = s.attention.copy(desktopNotifications = it)) } },
            modifier = Modifier.testTag("settings-desktop-notifications"),
            showDivider = false,
        )
    }
}
