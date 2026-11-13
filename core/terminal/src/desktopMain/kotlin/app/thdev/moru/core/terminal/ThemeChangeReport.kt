package app.thdev.moru.core.terminal

/**
 * What to send a program so it redraws in a changed theme, read from the modes its output switched on.
 *
 * Only a program that turned on theme notifications (mode 2031, as Claude Code does with its "Auto" theme) can follow a
 * change while it runs: it is told dark or light, then asks the terminal for its colors again. Codex reads the colors
 * once when it starts, so it follows the theme only after a restart.
 */
internal class ThemeChangeReport {
    private val modes = PrivateModeTracker(setOf(ThemeNotify))

    fun accept(output: CharSequence) = modes.accept(output)

    fun sequenceFor(dark: Boolean): String? =
        if (modes.isOn(ThemeNotify)) "\u001B[?997;${if (dark) 1 else 2}n" else null

    private companion object {
        const val ThemeNotify = 2031
    }
}
