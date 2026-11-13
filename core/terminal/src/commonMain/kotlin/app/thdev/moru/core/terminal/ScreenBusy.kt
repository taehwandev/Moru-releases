package app.thdev.moru.core.terminal

/**
 * True while the program on screen says it is at work. Claude Code and Codex both keep "esc to interrupt" in their
 * status line for as long as a turn runs ("✻ Thinking… (esc to interrupt)", "Working (12s • esc to interrupt)"),
 * including while a long command they started prints nothing. Only the bottom of the screen counts, since that is
 * where the status line sits; [rows] are the visible rows, top to bottom.
 */
fun screenBusy(rows: List<String>): Boolean =
    rows.asReversed().asSequence()
        .filter { row -> row.isNotBlank() }
        .take(BusyRowLimit)
        .any { row -> row.contains(BusyMark, ignoreCase = true) }

private const val BusyRowLimit = 8
private const val BusyMark = "esc to interrupt"
