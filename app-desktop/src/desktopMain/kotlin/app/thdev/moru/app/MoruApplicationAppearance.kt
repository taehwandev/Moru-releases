package app.thdev.moru.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.thdev.moru.core.model.AppTheme

/**
 * Keeps the macOS application's look in step with the theme chosen in settings, including when it is changed. A theme
 * that is dark makes the whole application dark and a light one makes it light, whatever the computer is set to;
 * only "System" leaves it to the computer.
 */
@Composable
internal fun MoruApplicationAppearance(dependencies: MoruDesktopDependencies) {
    val theme = dependencies.appState.settings.appearance.theme
    val systemDark = isSystemInDarkTheme()
    LaunchedEffect(theme, systemDark) {
        MacAppearance.apply(macAppearanceFor(theme, systemDark))
    }
}

/** The AppKit look for a theme setting: System follows the computer, every other theme by whether its colors are light. */
internal fun macAppearanceFor(theme: AppTheme, systemDark: Boolean): MacAppearanceLook =
    when {
        theme == AppTheme.System -> MacAppearanceLook.System
        theme.resolveColors(systemDark).isLight -> MacAppearanceLook.Light
        else -> MacAppearanceLook.Dark
    }
