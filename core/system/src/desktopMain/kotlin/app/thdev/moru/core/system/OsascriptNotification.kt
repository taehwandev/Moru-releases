package app.thdev.moru.core.system

/**
 * The command that shows a macOS notification. The title and body travel as arguments, never inside the script, so
 * quotes or backslashes in a Task's name cannot break out of it.
 */
internal fun osascriptNotificationCommand(title: String, body: String): List<String> =
    listOf(
        "osascript",
        "-e", "on run argv",
        "-e", "display notification (item 1 of argv) with title (item 2 of argv)",
        "-e", "end run",
        body, title,
    )
