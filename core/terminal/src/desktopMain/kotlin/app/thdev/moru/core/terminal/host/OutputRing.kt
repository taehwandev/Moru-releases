package app.thdev.moru.core.terminal.host

import app.thdev.moru.core.terminal.PrivateModeTracker

/**
 * The most recent output of one terminal, so a terminal reopened after a restart shows what it last showed. Only
 * [capacity] bytes are kept; the screen modes switched on before that (the full-screen alternate buffer, a hidden
 * cursor, mouse reporting, bracketed paste) are remembered separately and restored ahead of and after the replay.
 */
internal class OutputRing(private val capacity: Int) {
    private val bytes = ByteArray(capacity)
    private var start = 0
    private var size = 0
    private val modes = PrivateModeTracker(Tracked)

    @Synchronized
    fun append(data: ByteArray, offset: Int = 0, length: Int = data.size) {
        for (i in offset until offset + length) modes.accept(data[i].toInt().toChar())
        if (length >= capacity) {
            System.arraycopy(data, offset + length - capacity, bytes, 0, capacity)
            start = 0
            size = capacity
            return
        }
        var written = 0
        while (written < length) {
            // Write after the newest byte; when full that is the oldest byte, which the write replaces.
            val tail = (start + size) % capacity
            val chunk = minOf(length - written, capacity - tail)
            System.arraycopy(data, offset + written, bytes, tail, chunk)
            written += chunk
            val overflow = size + chunk - capacity
            if (overflow > 0) {
                start = (start + overflow) % capacity
                size = capacity
            } else {
                size += chunk
            }
        }
    }

    /** What to feed a fresh terminal so it ends up looking like this one does now. */
    @Synchronized
    fun replay(): ByteArray {
        val kept = ByteArray(size)
        for (i in 0 until size) kept[i] = bytes[(start + i) % capacity]
        // Output that was cut off at the front may start inside a character or an escape sequence; begin at a line.
        val from = if (size == capacity) (kept.indexOf('\n'.code.toByte()) + 1).coerceAtLeast(0) else 0
        val before = if (modes.isOn(AlternateScreen) || modes.isOn(AlternateScreenOld)) "\u001B[?1049h" else ""
        val after = modes.modes().entries.filter { it.key !in ScreenModes }.joinToString("") { (mode, on) -> "\u001B[?$mode${if (on) 'h' else 'l'}" }
        return before.toByteArray() + kept.copyOfRange(from, kept.size) + after.toByteArray()
    }

    private companion object {
        const val AlternateScreen = 1049
        const val AlternateScreenOld = 47
        val ScreenModes = setOf(AlternateScreen, AlternateScreenOld, 1047)
        // 2031: the program asked to hear theme changes, which a reopened terminal must keep sending it.
        val Tracked = ScreenModes + setOf(1, 25, 1000, 1002, 1003, 1004, 1006, 2004, 2031)
    }
}
