package app.thdev.moru.app

import app.thdev.moru.app.i18n.AppStrings
import app.thdev.moru.core.model.AppShortcut
import app.thdev.moru.core.ui.shortcut.ShortcutKeys

/**
 * The menu bar's content in the language in use, each item on the shortcut the user's keyboard settings give it. An
 * entry is enabled only while its command is registered and no shortcut is being recorded in Settings.
 */
internal fun appMenu(strings: AppStrings, commands: MoruCommands): List<AppMenuGroup> {
    fun entry(label: String, shortcut: AppShortcut, command: (() -> Unit)?): AppMenuEntry {
        val chord = commands.keyboard.chordFor(shortcut)
        return AppMenuEntry(
            label = label,
            key = chord?.let { ShortcutKeys.key(it.key) },
            shift = chord?.shift == true,
            alt = chord?.alt == true,
            enabled = command != null && !commands.recordingShortcut,
            onClick = { command?.invoke() },
        )
    }
    return listOf(
        AppMenuGroup(
            strings.menuFile,
            listOf(
                entry(strings.menuOpenProject, AppShortcut.OpenProject, commands.openProject),
                entry(strings.menuNewTask, AppShortcut.NewTask, commands.newTask),
                entry(strings.menuNewSession, AppShortcut.NewSession, commands.newSession),
                entry(strings.menuSplitRight, AppShortcut.SplitRight, commands.splitRight),
                entry(strings.menuSplitDown, AppShortcut.SplitDown, commands.splitDown),
                entry(strings.menuCloseTerminal, AppShortcut.CloseTerminal, commands.closeTerminal),
                entry(strings.menuReopenClosedTerminal, AppShortcut.ReopenClosedTerminal, commands.reopenClosedTerminal),
                entry(strings.menuCloseProject, AppShortcut.CloseProject, commands.closeProject),
                entry(strings.menuCheckForUpdates, AppShortcut.CheckForUpdates, commands.checkForUpdates),
                entry(strings.menuQuitEndingTerminals, AppShortcut.QuitEndingTerminals, commands.quitEndingTerminals),
            ),
        ),
        AppMenuGroup(
            strings.menuView,
            listOf(
                entry(strings.menuWork, AppShortcut.ShowWork, commands.showWork),
                entry(strings.menuCode, AppShortcut.ShowCode, commands.showCode),
                entry(strings.menuGit, AppShortcut.ShowGit, commands.showGit),
                entry(strings.menuNextTask, AppShortcut.NextTask, commands.nextTask),
                entry(strings.menuPreviousTask, AppShortcut.PreviousTask, commands.previousTask),
                entry(strings.menuNextSession, AppShortcut.NextSession, commands.nextSession),
                entry(strings.menuPreviousSession, AppShortcut.PreviousSession, commands.previousSession),
                entry(strings.menuKnowledge, AppShortcut.ShowKnowledge, commands.showKnowledge),
                entry(strings.menuSettings, AppShortcut.ShowSettings, commands.showSettings),
            ),
        ),
        AppMenuGroup(
            strings.menuProject,
            listOf(
                entry(strings.menuNextProject, AppShortcut.NextProject, commands.nextProject),
                entry(strings.menuPreviousProject, AppShortcut.PreviousProject, commands.previousProject),
            ) + commands.projects.zip(AppShortcut.openProjectByPosition) { project, shortcut ->
                entry(project.name, shortcut, project.open)
            },
        ),
    )
}
