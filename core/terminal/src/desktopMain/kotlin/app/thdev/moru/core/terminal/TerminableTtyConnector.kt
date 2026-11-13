package app.thdev.moru.core.terminal

import com.jediterm.terminal.TtyConnector

/**
 * A terminal whose program outlives the connection: [close] only lets go of it (Moru quitting), [terminate] ends the
 * program (the user closing the terminal).
 */
interface TerminableTtyConnector : TtyConnector {
    fun terminate()
}
