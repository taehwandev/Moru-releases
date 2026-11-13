package app.thdev.moru.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * An opening is entering a session's focused Work view, including returning from another tab or window.
 * Staying in a running session when its turn ends is not an opening and does not acknowledge its new response.
 */
internal class ViewedSessionResponseObserver {
    private var visibleSession: String? = null
    private var openingRevision: Long = 0L

    fun openedSession(sessionId: String?, workVisible: Boolean, windowFocused: Boolean, explicitOpening: Long = 0L): String? {
        val visible = sessionId.takeIf { workVisible && windowFocused }
        val opened = visible?.takeIf { it != visibleSession || explicitOpening > openingRevision }
        visibleSession = visible
        openingRevision = maxOf(openingRevision, explicitOpening)
        return opened
    }

    companion object {
        internal data class Opening(val sessionId: String, val revision: Long)

        private val sequence = AtomicLong()
        private val latest = MutableStateFlow<Opening?>(null)
        val openings: StateFlow<Opening?> = latest.asStateFlow()

        /** Requests an opening; only an observer inside a rendered session surface may acknowledge it. */
        fun requestOpening(sessionId: String) {
            latest.value = Opening(sessionId, sequence.incrementAndGet())
        }
    }
}
