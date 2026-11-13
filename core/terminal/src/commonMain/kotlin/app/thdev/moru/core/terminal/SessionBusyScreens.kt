package app.thdev.moru.core.terminal

import app.thdev.moru.core.model.session.SessionActivity
import java.util.concurrent.ConcurrentHashMap

/**
 * The sessions whose screen says the program is at work ([screenBusy]). Updated when the screen changes, on the Swing
 * thread, together with [SessionQuestions]; the UI reads it on its own clock.
 */
object SessionBusyScreens {
    private val busy = ConcurrentHashMap.newKeySet<String>()

    fun record(sessionId: String, isBusy: Boolean) {
        if (isBusy) busy.add(sessionId) else busy.remove(sessionId)
        if (isBusy) SessionResponseAttention.observe(sessionId, SessionActivity.Working)
    }

    fun isBusy(sessionId: String): Boolean = sessionId in busy

    fun forget(sessionId: String) {
        busy.remove(sessionId)
    }
}
