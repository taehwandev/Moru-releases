package app.thdev.moru.core.terminal

/** Follows `ESC [ ? n h` / `ESC [ ? n l` in a program's output, across chunk boundaries, for the [tracked] modes. */
internal class PrivateModeTracker(private val tracked: Set<Int>) {
    private val modes = sortedMapOf<Int, Boolean>()
    private val pending = StringBuilder()

    /** The tracked modes the program has switched on or off so far, in mode order. */
    @Synchronized
    fun modes(): Map<Int, Boolean> = modes.toMap()

    @Synchronized
    fun isOn(mode: Int): Boolean = modes[mode] == true

    @Synchronized
    fun accept(text: CharSequence) {
        for (c in text) accept(c)
    }

    @Synchronized
    fun accept(c: Char) {
        if (pending.isEmpty()) {
            if (c == '\u001B') pending.append(c)
            return
        }
        pending.append(c)
        when {
            pending.length == 2 && c != '[' -> pending.clear()
            pending.length == 3 && c != '?' -> pending.clear()
            pending.length > 3 && (c == 'h' || c == 'l') -> {
                pending.substring(3, pending.length - 1).split(';').mapNotNull { it.toIntOrNull() }
                    .filter { it in tracked }
                    .forEach { mode -> modes[mode] = c == 'h' }
                pending.clear()
            }
            pending.length > 3 && !(c.isDigit() || c == ';') -> pending.clear()
            pending.length > 32 -> pending.clear()
        }
    }
}
