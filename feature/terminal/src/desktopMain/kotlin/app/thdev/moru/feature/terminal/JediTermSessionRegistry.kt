package app.thdev.moru.feature.terminal

import app.thdev.moru.core.terminal.ListeningServers
import app.thdev.moru.core.terminal.LocalServerUrls
import app.thdev.moru.core.terminal.SessionLastLines
import app.thdev.moru.core.terminal.SessionBusyScreens
import app.thdev.moru.core.terminal.SessionQuestions
import app.thdev.moru.core.terminal.SessionPermissions
import app.thdev.moru.core.terminal.SessionRunningAgents
import app.thdev.moru.core.terminal.SessionTitles
import app.thdev.moru.core.terminal.SessionShellProcesses
import app.thdev.moru.core.terminal.SessionThemeColors
import app.thdev.moru.core.terminal.TerminalTeardown
import javax.swing.SwingUtilities
import app.thdev.moru.core.terminal.SessionReplyQuestions
import app.thdev.moru.core.terminal.SessionResults

/**
 * Keeps each PTY-backed terminal alive outside Compose. A screen switch or layout change
 * disposes the composable, but the agent running in the terminal must keep going, so the
 * widget lives here until its tab is closed or the app exits; its program may live on in the terminal host.
 */
internal object JediTermSessionRegistry {
    /** The current owner of a terminal's events. A new composition replaces the callbacks. */
    class Callbacks {
        @Volatile var onCommandCaptured: (String) -> Unit = {}
        @Volatile var onOutput: (atMillis: Long) -> Unit = {}
        @Volatile var onExit: (exitCode: Int?) -> Unit = {}
    }

    class Entry(
        val session: JediTermSession,
        val callbacks: Callbacks,
        val paletteHolder: TerminalPalette.Holder,
    )

    private val entries = mutableMapOf<String, Entry>()

    @Synchronized
    fun acquire(key: String, create: (Callbacks, TerminalPalette.Holder) -> JediTermSession, initialPalette: TerminalPalette): Entry =
        entries.getOrPut(key) {
            val callbacks = Callbacks()
            val paletteHolder = TerminalPalette.Holder(initialPalette)
            val session = try {
                create(callbacks, paletteHolder)
            } catch (error: Throwable) {
                SessionThemeColors.forget(key)
                throw error
            }
            Entry(session, callbacks, paletteHolder)
        }

    @Synchronized
    fun find(key: String): Entry? = entries[key]

    fun release(key: String, dispose: (JediTermSession) -> Unit = { it.close() }) {
        forget(key, synchronized(this) { entries.remove(key) }, dispose)
    }

    /** Forgets [key]'s terminal if it never got going, so opening the same key again starts it afresh. */
    fun releaseIfFailed(key: String) {
        val removed = synchronized(this) { entries[key]?.takeIf { it.session.failedToStart }?.also { entries.remove(key) } } ?: return
        forget(key, removed) { it.close() }
    }

    private fun forget(key: String, removed: Entry?, dispose: (JediTermSession) -> Unit) {
        LocalServerUrls.forget(key)
        SessionLastLines.forget(key)
        SessionQuestions.forget(key)
        SessionReplyQuestions.forget(key)
        SessionResults.forget(key)
        SessionPermissions.forget(key)
        SessionBusyScreens.forget(key)
        SessionTitles.forget(key)
        SessionShellProcesses.forget(key)
        SessionThemeColors.forget(key)
        ListeningServers.forget(key)
        SessionRunningAgents.forget(key)
        if (removed != null) closeAwayFromUi { dispose(removed.session) }
    }

    /** Lets go of every terminal as Moru quits; programs in the terminal host keep running unless [terminate]. */
    fun releaseAll(terminate: Boolean = false) {
        val removed = synchronized(this) { entries.toMap().also { entries.clear() } }
        removed.forEach { (key, entry) ->
            SessionThemeColors.forget(key)
            // One terminal that fails to close must not leave the rest of them open.
            closeAwayFromUi { runCatching { entry.session.close(terminate) } }
        }
    }

    private fun closeAwayFromUi(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) TerminalTeardown.execute(action) else action()
    }
}
