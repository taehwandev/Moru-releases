package app.thdev.moru.core.terminal

import java.util.concurrent.ConcurrentHashMap

/**
 * The question each session's agent ended its last message with ([screenReplyQuestion]), kept apart from
 * [SessionQuestions]: a choice prompt blocks the agent, while a reply question is a finished turn that waits for the
 * user's answer.
 */
object SessionReplyQuestions {
    private val questions = ConcurrentHashMap<String, String>()

    fun record(sessionId: String, question: String?) {
        if (question == null) questions.remove(sessionId) else questions[sessionId] = question
    }

    fun questionFor(sessionId: String): String? = questions[sessionId]

    fun forget(sessionId: String) {
        questions.remove(sessionId)
    }
}
