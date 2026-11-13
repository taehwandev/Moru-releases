package app.thdev.moru.core.terminal

import app.thdev.moru.core.model.session.SessionActivity
import java.util.concurrent.ConcurrentHashMap

/** Structured runtimes report their actual turn state instead of relying on terminal output timing. */
object SessionReportedActivities {
    private val activities = ConcurrentHashMap<String, SessionActivity>()

    fun record(sessionId: String, activity: SessionActivity) {
        activities[sessionId] = activity
        SessionResponseAttention.observe(sessionId, activity)
    }

    fun activityFor(sessionId: String): SessionActivity? = activities[sessionId]

    fun forget(sessionId: String) { activities.remove(sessionId) }
}
