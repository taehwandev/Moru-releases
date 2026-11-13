package app.thdev.moru.app

/**
 * The top-level screens. Their visible names come from the language tables, not from here. [Notifications] is not
 * on the rail: its bell sits at the right end of the title bar row and opens a panel instead of a screen.
 */
internal enum class MoruDestination {
    Workbench,
    Knowledge,
    Notifications,
    Settings,
    ;

    companion object {
        val activityBarDestinations = listOf(Workbench, Knowledge, Settings)
    }
}
