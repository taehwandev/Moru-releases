package app.thdev.moru.feature.settings.i18n

import app.thdev.moru.core.model.AppLanguage
import app.thdev.moru.core.model.AppShortcut
import app.thdev.moru.core.model.AppIcon
import app.thdev.moru.core.model.AppTheme
import app.thdev.moru.core.model.TaskSortOrder
import app.thdev.moru.core.model.terminal.TerminalColorSlot
import app.thdev.moru.core.model.terminal.TerminalCursorStyle
import app.thdev.moru.core.model.terminal.TerminalImportItem

internal object EnglishSettingsStrings : SettingsStrings {
    override val title = "Settings"
    override val subtitle = "Pick a page on the left."
    override val automaticSaveHint = "Changes save automatically."
    override val savingMessage = "Saving changes…"
    override val savedMessage = "All changes saved."
    override fun saveFailed(reason: String) = "Could not save changes: $reason"
    override val unknownError = "unknown error"
    override val retryButton = "Retry"
    override val invalidNumbersHint = "Correct the highlighted numbers to save those changes."
    override fun numberRange(min: Int, max: Int) = "Enter a whole number from $min to $max."

    override val updatesTitle = "Updates"
    override val appUpdatesTitle = "Moru app"
    override val labSessionsTitle = "Lab sessions"
    override val labSessionsDescription = "Adds Claude, Codex and AGY Lab to New Session, with shared conversation, copy, share, links, file changes, images and model selection."
    override val tasksBesideTitle = "Tasks beside"
    override val tasksBesideDescription = "Adds Open beside to a Task's menu: its terminals open next to the selected Task, its approvals can be answered there, and the Result can show its Preview, devices and Code."
    override val devicesAmongTerminalsTitle = "Device tools beside terminals"
    override val devicesAmongTerminalsDescription = "Adds Android Emulator, iOS Simulator and Compare Devices to the New session chooser (Command-T): the tool opens beside the Task's terminals, as a cell you move and resize like a terminal."
    override val projectWindowsTitle = "Project windows"
    override val projectWindowsDescription = "Adds Open in New Window to a Project's menu: the Project gets a window of its own, which you can put on another screen. It comes back where it was when Moru starts again."
    override val updatesDescription = "Keep Moru up to date."
    override fun currentVersion(version: String) = "Current version: $version"
    override val versionUnavailable = "App version unavailable"
    override val automaticUpdates = "Moru checks and downloads updates automatically. Restart when an update is ready."
    override val updatesUnavailable = "Updates are available in the installed macOS app."
    override val checkForUpdates = "Check for Updates…"
    override val restartToUpdate = "Restart to Update"

    override val generalTitle = "General"
    override val generalDescription = "State restoration and Task ordering."
    override val rememberStateTitle = "Remember Last State"
    override val rememberStateDescription =
        "On: quitting Moru leaves terminals and agents running, and the next launch picks up the window, workspace " +
            "and terminals where you left them. Off: quitting Moru ends every terminal; when some are running, " +
            "Moru asks first and lets you keep them that time."
    override val autoCollapseSidebarTitle = "Auto-collapse inactive Projects & Sessions"
    override val autoCollapseSidebarDescription =
        "On: selecting a Project or Task automatically collapses other Projects and folds idle session lists in the sidebar. " +
            "Off: Projects and session lists stay open or closed where you left them."
    override val taskSortOrderTitle = "Task order"
    override val taskSortOrderDescription =
        "Order Tasks by creation time in all Projects. Oldest first puts new Tasks at the bottom. Dragging switches to Manual."
    override fun taskSortOrderLabel(order: TaskSortOrder) = when (order) {
        TaskSortOrder.OldestFirst -> "Oldest first"
        TaskSortOrder.NewestFirst -> "Newest first"
        TaskSortOrder.Manual -> "Manual"
    }
    override val attentionTitle = "Alerts"
    override val attentionDescription = "How Moru tells you a Task is waiting for you or failed."
    override val desktopNotificationsTitle = "System Notifications"
    override val desktopNotificationsDescription = "Send a macOS notification when a Task is waiting for you or failed."

    override val appearanceTitle = "Appearance & Fonts"
    override val appearanceDescription = "Theme, language, and IDE readability."
    override val themeTitle = "Theme"
    override val themeDescription = "Choose the app color mode."
    override val appleStyleTitle = "Apple style"
    override val appleStyleDescription = "Use Liquid Glass surfaces and rounded controls. Applies after saving; your color theme stays the same."
    override val appIconTitle = "App icon"
    override val onboardingTitle = "Welcome and workflow setup"
    override val onboardingDescription = "Go through the first-run steps again: the look."
    override val onboardingOpen = "Show again"
    override val appIconDescription = "The icon in the Dock while Moru runs; it changes as you pick. Finder and Launchpad keep the Moru icon."
    override fun appIconLabel(icon: AppIcon) = when (icon) {
        AppIcon.Moru -> "Moru"
        AppIcon.Classic -> "Classic"
        AppIcon.Anvil -> "Anvil"
        AppIcon.M -> "m"
        AppIcon.Mo -> "Mo"
    }
    override val languageTitle = "Language"
    override val languageDescription = "Language of the app's own text."
    override val fontTitle = "Terminal Font"
    override val fontSizeTitle = "Terminal Font Size"
    override val fontDescription = "All embedded terminals, including Task panes. Applies to new sessions; IDE text is unchanged."
    override val fontPlaceholder = "System default"
    override val uiFontTitle = "IDE Font"
    override val uiFontSizeTitle = "IDE Font Size"
    override val uiFontDescription = "App text and code views. Applies after saving; terminal text is unchanged."
    override val searchFonts = "Search installed fonts"
    override val noFontsFound = "No matching fonts"
    override val fontPreview = "Aa Bb 0123 — Preview"
    override val resetFont = "Reset font"
    override fun decreaseFontSize(label: String) = "Decrease $label"
    override fun increaseFontSize(label: String) = "Increase $label"
    override fun themeLabel(theme: AppTheme) = when (theme) {
        AppTheme.System -> "System"
        AppTheme.Light -> "Light"
        AppTheme.Dark -> "Dark"
        AppTheme.FreshMint -> "Fresh Mint"
        AppTheme.GitHubLight -> "GitHub Light"
        AppTheme.GitHubDark -> "GitHub Dark"
        AppTheme.CatppuccinLatte -> "Catppuccin Latte"
        AppTheme.CatppuccinMocha -> "Catppuccin Mocha"
        AppTheme.TokyoNight -> "Tokyo Night"
    }
    override fun languageLabel(language: AppLanguage) = languageName(language, system = "System")

    override val terminalTitle = "Terminal"
    override val terminalDescription = "Defaults for new native PTY sessions."
    override val shellTitle = "Default Shell"
    override val shellDescription = "Empty uses the detected platform shell."
    override val scrollbackTitle = "Scrollback Lines"
    override val scrollbackDescription = "Maximum lines kept for each terminal buffer."
    override val optionKeyTitle = "Option Sends Escape"
    override val optionKeyDescription = "Option+key reaches the shell as Esc+key, for word moves. Turn off to type characters like å or ∑."
    override val terminalImportTitle = "iTerm2 Settings"
    override val terminalImportDescription = "Bring over the colors, font, cursor and scrollback of your iTerm2 default profile."
    override val terminalImportAction = "Import from iTerm2"
    override val terminalImporting = "Importing…"
    override val terminalLookTitle = "Terminal Look"
    override val terminalLookDescription = "Colors, cursor and line spacing for every terminal. Anything you leave alone follows the Moru theme."
    override val terminalSchemeTitle = "Color Scheme"
    override val terminalSchemeDescription = "One for the light Moru theme and one for the dark."
    override val terminalSchemeLight = "Light"
    override val terminalSchemeDark = "Dark"
    override val terminalSchemeMoru = "Moru"
    override val terminalSchemeCustom = "Custom"
    override val terminalColorsTitle = "Colors"
    override val terminalColorsDescription = "Pick a color and type #rrggbb. A dot marks your own; the rest follow Moru."
    override val terminalColorUseMoru = "Use Moru"
    override fun terminalColorSlot(slot: TerminalColorSlot) = when (slot) {
        TerminalColorSlot.Foreground -> "Text"
        TerminalColorSlot.Background -> "Background"
        TerminalColorSlot.Selection -> "Selection"
        else -> (if (slot.ansiIndex >= 8) "Bright " else "") + listOf("black", "red", "green", "yellow", "blue", "magenta", "cyan", "white")[slot.ansiIndex % 8]
            .let { if (slot.ansiIndex >= 8) it else it.replaceFirstChar(Char::uppercase) }
    }
    override val terminalCursorTitle = "Cursor"
    override val terminalCursorDescription = "The cursor shape until a program asks for another."
    override fun terminalCursorStyle(style: TerminalCursorStyle) = when (style) {
        TerminalCursorStyle.Block -> "Block"
        TerminalCursorStyle.Underline -> "Underline"
        TerminalCursorStyle.Bar -> "Bar"
    }
    override val terminalCursorBlinkTitle = "Blinking Cursor"
    override val terminalCursorBlinkDescription = "The cursor blinks while the terminal waits for input."
    override val terminalLineSpacingTitle = "Line Spacing (%)"
    override val terminalLineSpacingDescription = "Line height as a percent of the font's own. Moru uses 106."
    override val terminalLookResetTitle = "Reset Terminal Look"
    override val terminalLookResetDescription = "Removes imported and custom colors, cursor and spacing, and the imported font and scrollback."
    override val terminalLookResetAction = "Reset to Moru"
    override fun terminalImportUsing(source: String) = "Using $source."
    override val terminalImportNotFound = "No iTerm2 settings were found on this Mac."
    override fun terminalImportFailed(reason: String) = "Could not read the iTerm2 settings: $reason"
    override fun terminalImportApplied(items: String) = "Applied: $items"
    override fun terminalImportSkipped(items: String) = "Not supported: $items"
    override fun terminalImportItem(item: TerminalImportItem) = when (item) {
        TerminalImportItem.Colors -> "colors"
        TerminalImportItem.LightDarkColors -> "light and dark colors"
        TerminalImportItem.Font -> "font"
        TerminalImportItem.LineSpacing -> "line spacing"
        TerminalImportItem.Cursor -> "cursor"
        TerminalImportItem.Scrollback -> "scrollback"
        TerminalImportItem.Transparency -> "transparency"
        TerminalImportItem.Blur -> "blur"
        TerminalImportItem.BackgroundImage -> "background image"
        TerminalImportItem.ColumnSpacing -> "column spacing"
        TerminalImportItem.CursorColor -> "cursor color"
        TerminalImportItem.NonAsciiFont -> "non-ASCII font"
        TerminalImportItem.Ligatures -> "ligatures"
        TerminalImportItem.KeyMappings -> "key mappings"
        TerminalImportItem.Triggers -> "triggers"
    }

    override val agentClisTitle = "Agent CLIs"
    override val agentClisDescription =
        "The CLIs Moru starts in Task terminals. Update follows how each was installed and the release channel you chose."
    override val agentClisLooking = "Looking for installed CLIs…"
    override val noAgentCliTitle = "No agent CLI found"
    override val noAgentCliDescription = "Install Claude Code, Codex or Antigravity (agy), then open Settings again."
    override val cliUpdatedOutsideMoru =
        "Installed with npm, so Moru cannot tell its release channel. Update it with npm the way you installed it."
    override val cliUpdate = "Update"
    override val cliUpdating = "Updating…"
    override fun cliUpdated(version: String) =
        "Updated to $version. Idle sessions restart now; busy ones restart when their turn ends."
    override fun cliAlreadyLatest(version: String) = "Already the newest version for this install ($version)."
    override fun cliUpdateFailed(detail: String?, command: String) =
        "Update failed${detail?.let { ": $it" } ?: "."} Run it in a terminal: $command"

    override val shortcutsTitle = "Shortcuts"
    override val shortcutsDescription = "Click a shortcut, then press the new keys. Delete clears it; Escape keeps it."
    override fun shortcutLabel(shortcut: AppShortcut) = when (shortcut) {
        AppShortcut.OpenProject -> "Open Project"
        AppShortcut.NewTask -> "New Task"
        AppShortcut.NewSession -> "New Session"
        AppShortcut.SplitRight -> "Split Terminal Right"
        AppShortcut.SplitDown -> "Split Terminal Down"
        AppShortcut.CloseTerminal -> "Close Terminal"
        AppShortcut.ReopenClosedTerminal -> "Reopen Closed Terminal"
        AppShortcut.CloseProject -> "Close Project"
        AppShortcut.CheckForUpdates -> "Check for Updates"
        AppShortcut.QuitEndingTerminals -> "Quit and End All Terminals"
        AppShortcut.ShowWork -> "Show Work"
        AppShortcut.ShowCode -> "Show Code"
        AppShortcut.ShowGit -> "Show Git"
        AppShortcut.NextTask -> "Next Task"
        AppShortcut.PreviousTask -> "Previous Task"
        AppShortcut.NextSession -> "Next Session"
        AppShortcut.PreviousSession -> "Previous Session"
        AppShortcut.ShowKnowledge -> "Show Knowledge"
        AppShortcut.ShowSettings -> "Show Settings"
        AppShortcut.NextProject -> "Next Project"
        AppShortcut.PreviousProject -> "Previous Project"
        else -> "Open Project ${AppShortcut.openProjectByPosition.indexOf(shortcut) + 1}"
    }
    override val shortcutNone = "None"
    override val shortcutWaiting = "Press keys…"
    override fun shortcutReserved(chord: String) = "$chord is kept for copy, paste or the system"
    override val shortcutReset = "Default"
    override fun shortcutMoved(from: String, to: String) = "Moved from “$from” to “$to”."
}
