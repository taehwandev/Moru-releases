package app.thdev.moru.core.model

/** Every app command a shortcut can trigger, with its stable settings [id] and the chord it has until the user changes it. */
enum class AppShortcut(val id: String, val default: ShortcutChord?) {
    OpenProject("open_project", ShortcutChord("O")),
    NewTask("new_task", ShortcutChord("N")),
    NewSession("new_session", ShortcutChord("T")),
    SplitRight("split_right", ShortcutChord("D")),
    SplitDown("split_down", ShortcutChord("D", shift = true)),
    CloseTerminal("close_terminal", ShortcutChord("W")),
    ReopenClosedTerminal("reopen_closed_terminal", ShortcutChord("T", shift = true)),
    CloseProject("close_project", ShortcutChord("W", shift = true)),
    CheckForUpdates("check_for_updates", null),
    QuitEndingTerminals("quit_ending_terminals", ShortcutChord("Q", shift = true)),
    ShowWork("show_work", ShortcutChord("1", alt = true)),
    ShowCode("show_code", ShortcutChord("2", alt = true)),
    ShowGit("show_git", ShortcutChord("3", alt = true)),
    NextTask("next_task", ShortcutChord("]")),
    PreviousTask("previous_task", ShortcutChord("[")),
    NextSession("next_session", ShortcutChord("]", alt = true)),
    PreviousSession("previous_session", ShortcutChord("[", alt = true)),
    ShowKnowledge("show_knowledge", ShortcutChord("K", shift = true)),
    ShowSettings("show_settings", ShortcutChord(",")),
    NextProject("next_project", ShortcutChord("]", shift = true)),
    PreviousProject("previous_project", ShortcutChord("[", shift = true)),
    OpenProject1("open_project_1", ShortcutChord("1")),
    OpenProject2("open_project_2", ShortcutChord("2")),
    OpenProject3("open_project_3", ShortcutChord("3")),
    OpenProject4("open_project_4", ShortcutChord("4")),
    OpenProject5("open_project_5", ShortcutChord("5")),
    OpenProject6("open_project_6", ShortcutChord("6")),
    OpenProject7("open_project_7", ShortcutChord("7")),
    OpenProject8("open_project_8", ShortcutChord("8")),
    OpenProject9("open_project_9", ShortcutChord("9")),
    ;

    companion object {
        /** Opens the Projects in sidebar order, first to ninth. */
        val openProjectByPosition = listOf(
            OpenProject1, OpenProject2, OpenProject3, OpenProject4, OpenProject5,
            OpenProject6, OpenProject7, OpenProject8, OpenProject9,
        )
    }
}
