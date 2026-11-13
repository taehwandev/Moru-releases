package app.thdev.moru.feature.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruSettingRow
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.AppIcon
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.feature.settings.i18n.settingsStrings

/** Picks the app's Dock icon from the pictures [preview] draws; each choice shows its picture and its name. */
@Composable
internal fun SettingsAppIconRow(
    settings: AppSettings,
    onChange: (edit: (AppSettings) -> AppSettings) -> Unit,
    preview: @Composable (AppIcon, Modifier) -> Unit,
) {
    val strings = settingsStrings()
    val selected = AppIcon.fromId(settings.appearance.appIcon)
    MoruSettingRow(title = strings.appIconTitle, description = strings.appIconDescription) {
        FlowRow(
            modifier = Modifier.widthIn(max = 380.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppIcon.entries.forEach { icon ->
                val chosen = icon == selected
                Column(
                    modifier = Modifier
                        .selectable(selected = chosen, role = Role.RadioButton) {
                            onChange { s -> s.copy(appearance = s.appearance.copy(appIcon = icon.id)) }
                        }
                        .border(2.dp, if (chosen) MoruTheme.colors.primary else Color.Transparent, RoundedCornerShape(MoruTheme.radius.medium))
                        .padding(6.dp)
                        .testTag("settings-app-icon-${icon.id}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    preview(icon, Modifier.size(52.dp))
                    MoruText(
                        text = strings.appIconLabel(icon),
                        style = MoruTheme.typography.caption,
                        color = if (chosen) MoruTheme.colors.textPrimary else MoruTheme.colors.textSecondary,
                    )
                }
            }
        }
    }
}
