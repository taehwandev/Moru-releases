package app.thdev.moru.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.FrameWindowScope
import app.thdev.moru.app.i18n.appStrings

/**
 * The window's menu bar. Each item carries its shortcut, so the keys work wherever the focus is, including
 * the terminal, and the menu shows what they are. The platform modifier is Command on macOS and Control elsewhere.
 */
@Composable
internal fun FrameWindowScope.MoruMenuBar(dependencies: MoruDesktopDependencies, commands: MoruCommands) {
    val language = resolveUiLanguage(
        dependencies.appState.settings.appearance.language,
        java.util.Locale.getDefault().toLanguageTag(),
    )
    MoruCommandMenuBar(appStrings(language), commands)
}
