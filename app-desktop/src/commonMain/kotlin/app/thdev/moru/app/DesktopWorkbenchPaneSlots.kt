package app.thdev.moru.app

import androidx.compose.runtime.CompositionLocalProvider
import app.thdev.moru.feature.terminal.LocalTerminalSessionLabel
import app.thdev.moru.feature.terminal.MultiTerminalPane
import app.thdev.moru.feature.terminal.submitLineToTerminalSession
import app.thdev.moru.feature.terminal.typeIntoTerminalSession
import app.thdev.moru.feature.workbench.slots.WorkbenchPaneSlots
import app.thdev.moru.feature.workbench.slots.WorkbenchTerminalSlots


/**
 * The real terminal and session views the workbench draws, composed here so features never depend on each
 * other. The Code panel slot needs the code reader and the saved checkouts, so [rememberWorkbenchCodeSlots] adds it
 * per screen.
 */
internal val DesktopWorkbenchPaneSlots = WorkbenchPaneSlots(
    terminal = WorkbenchTerminalSlots(
        pane = { spec, modifier ->
            // Tabs and pane headers read like the sidebar's session rows instead of a dot that is green for all.
            CompositionLocalProvider(LocalTerminalSessionLabel provides rememberSessionLabel(spec.sessionManager)) {
                WindowOverlayRegion(modifier) { workspaceModifier ->
                    MultiTerminalPane(
                        sessionManager = spec.sessionManager,
                        defaultWorkingDirectory = spec.defaultWorkingDirectory,
                        defaultShell = spec.defaultShell,
                        workspaceId = spec.workspaceId,
                        workspaceName = spec.workspaceName,
                        terminalFontFamily = spec.terminalFontFamily,
                        terminalFontSize = spec.terminalFontSize,
                        terminalScrollbackLines = spec.terminalScrollbackLines,
                        onOpenExternalTerminal = spec.onOpenExternalTerminal,
                        modifier = workspaceModifier,
                        onNewTerminal = spec.onNewTerminal,
                        sessionBranch = spec.sessionBranch,
                        split = spec.split,
                        onSplitChange = spec.onSplitChange,
                        publishesSelection = spec.publishesSelection,
                        panels = spec.panels,
                        onActivate = spec.onActivate,
                        canOpenPanels = spec.canOpenPanels,
                    )
                }
            }
        },
        typeInto = ::typeIntoTerminalSession,
        submitLine = ::submitLineToTerminalSession,
    ),
    sessions = DesktopWorkbenchSessionSlots,
)
