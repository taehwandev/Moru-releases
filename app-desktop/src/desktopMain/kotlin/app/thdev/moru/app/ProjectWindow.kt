package app.thdev.moru.app

import app.thdev.moru.core.model.AppIcon
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.awt.SwingWindow
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import app.thdev.moru.core.designsystem.component.MoruDesktopPopupHost
import app.thdev.moru.core.designsystem.material.prepareMoruNativeWindow
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import app.thdev.moru.core.model.SavedProjectWindow
import java.awt.EventQueue
import java.awt.GraphicsEnvironment
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop

/**
 * A Project in a window of its own: the same workbench as the main window, showing only that Project, with its own menu
 * and shortcuts ([commands] are this window's). It opens where it was last; closing it leaves the Project in the main
 * window. The app's quit and update commands are the main window's ([appCommands]).
 */
@OptIn(FlowPreview::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
internal fun ProjectWindow(
    saved: SavedProjectWindow,
    dependencies: MoruDesktopDependencies,
    layout: WorkbenchLayoutKeeper,
    projectWindows: ProjectWindows,
    appCommands: MoruCommands,
) {
    val projectId = saved.projectId
    val commands = remember(projectId) { MoruCommands() }
    DisposableEffect(commands, appCommands.quitEndingTerminals, appCommands.checkForUpdates) {
        commands.quitEndingTerminals = appCommands.quitEndingTerminals
        commands.checkForUpdates = appCommands.checkForUpdates
        onDispose { }
    }
    val state = rememberWindowState(
        position = savedPosition(saved),
        size = DpSize((saved.width ?: DefaultWidth).dp, (saved.height ?: DefaultHeight).dp),
    )
    val name = projectWindows.names[projectId]
    val iconChoice = AppIcon.fromId(dependencies.appState.settings.appearance.appIcon)
    val icon = remember(iconChoice) { BitmapPainter(MoruDesktopRuntimeProperties.applicationIcon(iconChoice).toComposeImageBitmap()) }
    SwingWindow(
        onCloseRequest = { projectWindows.close(projectId) },
        state = state,
        init = ::prepareMoruNativeWindow,
        title = listOfNotNull(name, MoruDesktopRuntimeProperties.applicationName).joinToString(" — "),
        icon = icon,
    ) {
        LaunchedEffect(state) {
            snapshotFlow { state.position to state.size }
                .drop(1)
                .debounce(WindowSaveDelayMillis)
                .collect { (position, size) ->
                    val placed = position as? WindowPosition.Absolute
                    projectWindows.moved(projectId, placed?.x?.value, placed?.y?.value, size.width.value, size.height.value)
                }
        }
        LaunchedEffect(Unit) { MoruWindowChrome.apply(window.rootPane) }
        val forward = projectWindows.forwardRequest
        LaunchedEffect(forward) {
            if (forward == projectId) {
                EventQueue.invokeLater {
                    window.toFront()
                    window.requestFocus()
                }
                projectWindows.forwarded(projectId)
            }
        }
        val appearance = dependencies.appState.settings.appearance
        MoruDesktopPopupHost(
            window,
            darkTheme = !appearance.theme.resolveColors(isSystemInDarkTheme()).isLight,
            materialStyle = if (appearance.appleStyle) MoruMaterialStyle.Apple else MoruMaterialStyle.Standard,
        ) {
            MoruMenuBar(dependencies, commands)
            MoruWindowContent(dependencies, commands, layout, windowProjectId = projectId, projectWindows = projectWindows)
        }
    }
}

/** Every Project open in a window of its own (Experimental), each where it was; none while the switch is off. */
@Composable
internal fun OpenProjectWindows(
    dependencies: MoruDesktopDependencies,
    layout: WorkbenchLayoutKeeper,
    projectWindows: ProjectWindows,
    appCommands: MoruCommands,
) {
    if (!dependencies.appState.settings.experimental.projectWindows) return
    val revision = CatalogChanges.revision
    LaunchedEffect(revision) { projectWindows.refresh() }
    projectWindows.windows.forEach { saved ->
        key(saved.projectId) { ProjectWindow(saved, dependencies, layout, projectWindows, appCommands) }
    }
}

/** Where the window was, or the screen's middle when it was never placed or its monitor is gone. */
private fun savedPosition(saved: SavedProjectWindow): WindowPosition {
    val x = saved.x ?: return WindowPosition.Aligned(Alignment.Center)
    val y = saved.y ?: return WindowPosition.Aligned(Alignment.Center)
    val onScreen = runCatching {
        GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.any { device ->
            device.defaultConfiguration.bounds.contains(x.toInt() + 40, y.toInt() + 20)
        }
    }.getOrDefault(true)
    return if (onScreen) WindowPosition(x.dp, y.dp) else WindowPosition.Aligned(Alignment.Center)
}

private const val DefaultWidth = 1280f
private const val DefaultHeight = 820f
