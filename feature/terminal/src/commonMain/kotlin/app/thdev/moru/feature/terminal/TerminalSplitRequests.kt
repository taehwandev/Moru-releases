package app.thdev.moru.feature.terminal

import app.thdev.moru.core.model.panel.PanelEdge

/**
 * Where a terminal opened by a split goes: the app names the new session, the terminal it splits and the side, right
 * after creating it and before the pane draws it; the pane takes it once and then makes every terminal the same size.
 * Without a request a new terminal goes into the largest place. Used on the UI thread only.
 */
object TerminalSplitRequests {
    private val pending = mutableMapOf<String, Pair<String, PanelEdge>>()

    fun place(newSessionId: String, besideSessionId: String, edge: PanelEdge) {
        pending[newSessionId] = besideSessionId to edge
    }

    /** The terminal [newSessionId] splits and on which side, once; null when it was not opened by a split. */
    fun take(newSessionId: String): Pair<String, PanelEdge>? = pending.remove(newSessionId)
}
