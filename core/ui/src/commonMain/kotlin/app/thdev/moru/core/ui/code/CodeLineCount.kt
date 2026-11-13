package app.thdev.moru.core.ui.code

/** Number of visual source lines, counting a trailing newline's empty line. */
fun String.codeLineCount(): Int = if (isEmpty()) 1 else count { char -> char == '\n' } + 1
