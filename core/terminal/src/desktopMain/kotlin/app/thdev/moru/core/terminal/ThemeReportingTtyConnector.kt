package app.thdev.moru.core.terminal

import com.jediterm.terminal.TtyConnector

/** A terminal that can tell the program in it that Moru switched between dark and light. */
interface ThemeReportingTtyConnector : TtyConnector {
    /** Tells the program the theme changed, in whatever way it asked to hear it; nothing when it asked for none. */
    fun reportThemeChange(dark: Boolean)
}
