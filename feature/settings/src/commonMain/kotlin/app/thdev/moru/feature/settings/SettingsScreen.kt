package app.thdev.moru.feature.settings

import app.thdev.moru.core.model.AppIcon
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.installedSystemFontFamilies
import app.thdev.moru.core.domain.SettingsRepository
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.terminal.TerminalProfileImport
import app.thdev.moru.core.runtime.RuntimeStatusRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Composable
fun SettingsScreen(
    runtimeStatusRepository: RuntimeStatusRepository,
    settingsRepository: SettingsRepository,
    onSettingsSaved: (AppSettings) -> Unit = {},
    editor: SettingsEditorState = rememberSettingsEditorState(settingsRepository, onSettingsSaved),
    fontFamilies: List<String> = remember { installedSystemFontFamilies() },
    updateState: SettingsUpdateState = SettingsUpdateState(),
    onCheckForUpdates: () -> Unit = {},
    onRestartForUpdate: () -> Unit = {},
    /** Draws each app icon the user can pick; null where the app has none to show, which hides the choice. */
    appIconPreview: (@Composable (AppIcon, Modifier) -> Unit)? = null,
    /** Opens the first-run welcome again from Settings › General; null hides the button. */
    onShowOnboarding: (() -> Unit)? = null,
    /** Where agent CLIs are found, their versions read and updates run; tests pass one that stays on the UI thread. */
    cliUpdateDispatcher: CoroutineDispatcher = Dispatchers.IO,
    /** The page to open, by [SettingsPage] name; the first page for null. */
    page: String? = null,
    onPageChange: (String) -> Unit = {},
    /** True while a shortcut waits for new keys, so the app's own shortcuts stand aside. */
    onRecordingShortcut: (Boolean) -> Unit = {},
    /** Reads the iTerm2 default profile; off the UI thread in the app. */
    importTerminalProfile: suspend () -> TerminalProfileImport = { TerminalProfileImport.NotFound },
) {
    var shownPage by remember { mutableStateOf(SettingsPage.named(page)) }
    LaunchedEffect(editor) { editor.refresh() }
    val agentClis = rememberAgentCliState(runtimeStatusRepository, cliUpdateDispatcher)
    LaunchedEffect(agentClis) { agentClis.load() }
    Box(
        modifier = Modifier.fillMaxSize().ckSurface(MoruSurfaceRole.Page).padding(16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        SettingsFormPanel(
            editor = editor,
            page = shownPage,
            onPageChange = { next -> shownPage = next; onPageChange(next.name) },
            fontFamilies = fontFamilies,
            updateState = updateState,
            onCheckForUpdates = onCheckForUpdates,
            onRestartForUpdate = onRestartForUpdate,
            agentClis = agentClis,
            appIconPreview = appIconPreview,
            onShowOnboarding = onShowOnboarding,
            onRecordingShortcut = onRecordingShortcut,
            importTerminalProfile = importTerminalProfile,
            modifier = Modifier.widthIn(max = 1120.dp).fillMaxWidth().fillMaxHeight(),
        )
    }
}
