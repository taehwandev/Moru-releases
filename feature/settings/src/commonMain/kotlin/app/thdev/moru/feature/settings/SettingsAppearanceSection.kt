package app.thdev.moru.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruSettingOptionSelector
import app.thdev.moru.core.designsystem.component.MoruSettingRow
import app.thdev.moru.core.designsystem.component.MoruSettingSection
import app.thdev.moru.core.designsystem.component.MoruSettingSwitchRow
import app.thdev.moru.core.model.AppIcon
import app.thdev.moru.core.model.AppLanguage
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.AppTheme
import app.thdev.moru.feature.settings.i18n.settingsStrings

@Composable
internal fun SettingsAppearanceSection(
    settings: AppSettings,
    onChange: (edit: (AppSettings) -> AppSettings) -> Unit,
    fontSizeText: String,
    onFontSizeTextChange: (String) -> Unit,
    fontFamilies: List<String>,
    /** Draws an app icon choice; null leaves the app icon row out, as where the app has no pictures for them. */
    appIconPreview: (@Composable (AppIcon, Modifier) -> Unit)? = null,
) {
    val strings = settingsStrings()
    MoruSettingSection(
        title = strings.appearanceTitle,
        description = strings.appearanceDescription,
        icon = MoruIconName.Palette,
    ) {
        MoruSettingRow(title = strings.themeTitle, description = strings.themeDescription) {
            MoruSettingOptionSelector(
                options = AppTheme.entries.map { it.name },
                selectedOption = settings.appearance.theme.name,
                onOptionSelected = {
                    val theme = AppTheme.valueOf(it)
                    onChange { s -> s.copy(appearance = s.appearance.copy(theme = theme)) }
                },
                optionLabel = { strings.themeLabel(AppTheme.valueOf(it)) },
                modifier = Modifier.widthIn(max = 360.dp),
                wrapInFlow = true,
            )
        }
        MoruSettingSwitchRow(
            title = strings.appleStyleTitle,
            description = strings.appleStyleDescription,
            checked = settings.appearance.appleStyle,
            onCheckedChange = { enabled ->
                onChange { it.copy(appearance = it.appearance.copy(appleStyle = enabled)) }
            },
            modifier = Modifier.testTag("settings-apple-style"),
        )
        appIconPreview?.let { preview -> SettingsAppIconRow(settings, onChange, preview) }
        MoruSettingRow(title = strings.languageTitle, description = strings.languageDescription) {
            MoruSettingOptionSelector(
                options = AppLanguage.entries.map { it.name },
                selectedOption = settings.appearance.language.name,
                onOptionSelected = {
                    val language = AppLanguage.valueOf(it)
                    onChange { s -> s.copy(appearance = s.appearance.copy(language = language)) }
                },
                optionLabel = { strings.languageLabel(AppLanguage.valueOf(it)) },
            )
        }
        MoruSettingRow(title = strings.uiFontTitle, description = strings.uiFontDescription, showDivider = false) {
            SettingsFontControls(
                family = settings.appearance.uiFontFamily, families = fontFamilies,
                size = settings.appearance.uiFontSize, sizeText = fontSizeText,
                label = strings.uiFontTitle, sizeLabel = strings.uiFontSizeTitle,
                tag = "settings-ui-font", defaultFamily = FontFamily.Default,
                onFamilyChange = { font -> onChange { it.copy(appearance = it.appearance.copy(uiFontFamily = font)) } },
                onSizeChange = onFontSizeTextChange,
                onReset = {
                    onChange { it.copy(appearance = it.appearance.copy(uiFontFamily = null, uiFontSize = 14)) }
                    onFontSizeTextChange("14")
                },
            )
        }
    }
}
