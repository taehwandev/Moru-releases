package app.thdev.moru.feature.settings.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruSettingSwitchRow
import app.thdev.moru.core.model.AppTheme
import app.thdev.moru.feature.settings.SettingsEditorState
import app.thdev.moru.feature.settings.i18n.onboardingStrings
import app.thdev.moru.feature.settings.i18n.settingsStrings

/** Light, Dark or System as window previews, and Apple style; the other themes stay in Settings. Each choice shows at once. */
@Composable
internal fun OnboardingThemeStep(editor: SettingsEditorState) {
    val strings = onboardingStrings()
    val settingsText = settingsStrings()
    val appearance = editor.settings.appearance
    OnboardingPageHeader(strings.themeTitle, strings.themeBody)
    Row(modifier = Modifier.selectableGroup().testTag("onboarding-theme"), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        listOf(AppTheme.Light, AppTheme.Dark, AppTheme.System).forEach { theme ->
            OnboardingThemeCard(
                theme = theme, label = settingsText.themeLabel(theme), selected = appearance.theme == theme,
                onSelect = { editor.change { s -> s.copy(appearance = s.appearance.copy(theme = theme)) } },
            )
        }
    }
    MoruSettingSwitchRow(
        title = strings.appleStyleTitle, description = strings.appleStyleBody,
        checked = appearance.appleStyle,
        onCheckedChange = { on -> editor.change { s -> s.copy(appearance = s.appearance.copy(appleStyle = on)) } },
        showDivider = false,
        modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(top = 8.dp).testTag("onboarding-apple-style"),
    )
}
