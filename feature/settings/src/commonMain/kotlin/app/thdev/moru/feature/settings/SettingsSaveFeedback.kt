package app.thdev.moru.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.settings.i18n.settingsStrings

@Composable
fun SettingsSaveFeedback(status: SettingsSaveStatus, error: String?, invalidNumbers: Boolean, onRetry: () -> Unit) {
    val strings = settingsStrings()
    val message = if (invalidNumbers && status != SettingsSaveStatus.Failed) strings.invalidNumbersHint else when (status) {
        SettingsSaveStatus.Ready -> strings.automaticSaveHint
        SettingsSaveStatus.Saving -> strings.savingMessage
        SettingsSaveStatus.Saved -> strings.savedMessage
        SettingsSaveStatus.Failed -> strings.saveFailed(error ?: strings.unknownError)
    }
    val icon = when {
        status == SettingsSaveStatus.Failed -> MoruIconName.Close
        invalidNumbers && status != SettingsSaveStatus.Failed -> MoruIconName.Question
        status == SettingsSaveStatus.Saving -> MoruIconName.Loader
        status == SettingsSaveStatus.Saved -> MoruIconName.Check
        else -> null
    }
    val contentColor = when {
        status == SettingsSaveStatus.Failed -> MoruTheme.colors.error
        status == SettingsSaveStatus.Saved -> MoruTheme.colors.success
        else -> MoruTheme.colors.textSecondary
    }
    val bgColor = when {
        status == SettingsSaveStatus.Failed -> MoruTheme.colors.error.copy(alpha = 0.08f)
        status == SettingsSaveStatus.Saved -> MoruTheme.colors.success.copy(alpha = 0.08f)
        else -> Color.Transparent
    }
    val shape = RoundedCornerShape(MoruTheme.radius.small)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bgColor)
            .padding(horizontal = if (bgColor != Color.Transparent) 8.dp else 0.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            MoruIcon(name = icon, size = 14.dp, tint = contentColor)
        }
        MoruText(
            text = message,
            modifier = Modifier.weight(1f).testTag("settings-save-status"),
            color = contentColor,
            style = MoruTheme.typography.caption,
        )
        if (status == SettingsSaveStatus.Failed) {
            MoruButton(strings.retryButton, onRetry, Modifier.testTag("settings-save-retry"), MoruButtonType.SECONDARY)
        }
    }
}
