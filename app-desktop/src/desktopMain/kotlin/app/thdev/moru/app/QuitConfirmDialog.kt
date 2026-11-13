package app.thdev.moru.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import app.thdev.moru.app.i18n.appStrings
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** The quit question in its own small window, in the app's theme and language. Closing it cancels. */
@Composable
internal fun QuitConfirmDialog(
    dependencies: MoruDesktopDependencies,
    runningTerminals: Int,
    onQuit: (keepTerminals: Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    val appearance = dependencies.appState.settings.appearance
    val language = resolveUiLanguage(appearance.language, java.util.Locale.getDefault().toLanguageTag())
    DialogWindow(
        onCloseRequest = onCancel,
        state = rememberDialogState(size = DpSize(440.dp, 250.dp)),
        title = appStrings(language).quitTitle,
        resizable = false,
    ) {
        CompositionLocalProvider(LocalUiLanguage provides language) {
            MoruTheme(colors = appearance.theme.resolveColors(isSystemInDarkTheme())) {
                QuitConfirmContent(runningTerminals, onQuit, onCancel)
            }
        }
    }
}
