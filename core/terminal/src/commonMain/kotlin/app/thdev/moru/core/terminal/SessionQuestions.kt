package app.thdev.moru.core.terminal

import java.util.concurrent.ConcurrentHashMap

/**
 * The question each terminal session's screen is asking now, found by [screenQuestion]. The screen is read on the
 * Swing thread and the UI reads this on its own timer, like [SessionLastLines].
 */
object SessionQuestions {
    private val questions = ConcurrentHashMap<String, String>()

    fun record(sessionId: String, question: String?) {
        if (question == null) questions.remove(sessionId) else questions[sessionId] = question
    }

    fun questionFor(sessionId: String): String? = questions[sessionId]

    fun forget(sessionId: String) {
        questions.remove(sessionId)
    }
}
