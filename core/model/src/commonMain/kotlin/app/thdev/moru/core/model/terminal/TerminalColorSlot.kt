package app.thdev.moru.core.model.terminal

/** One color a user can set: the text, the canvas, the selection, or one of the 16 ANSI colors. */
enum class TerminalColorSlot(val ansiIndex: Int = -1) {
    Foreground,
    Background,
    Selection,
    Black(0), Red(1), Green(2), Yellow(3), Blue(4), Magenta(5), Cyan(6), White(7),
    BrightBlack(8), BrightRed(9), BrightGreen(10), BrightYellow(11),
    BrightBlue(12), BrightMagenta(13), BrightCyan(14), BrightWhite(15),
    ;

    companion object {
        val ansiSlots: List<TerminalColorSlot> = entries.filter { it.ansiIndex >= 0 }
    }
}
