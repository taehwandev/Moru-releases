package app.thdev.moru.core.terminal

import java.util.concurrent.ConcurrentHashMap

/**
 * When Moru itself last prodded each session's program: attaching its view (the terminal host replays the screen),
 * resizing it, or sending it input (keys, pasted text, focus reports). Claude Code and Codex redraw their whole
 * screen in answer, and that output is a reaction, not the agent working. Counting it made opening a finished
 * session from its notification read as running again, and then as finished again, over and over.
 */
object SessionStimuli {
    /** Output this soon after a stimulus is taken as the answer to it. */
    const val ReactionWindowMillis: Long = 2_000

    private val stimulatedAt = ConcurrentHashMap<String, Long>()

    fun record(sessionId: String, atMillis: Long) {
        stimulatedAt[sessionId] = atMillis
    }

    /** True when output at [atMillis] answers something Moru did to [sessionId] just before. */
    fun isReaction(sessionId: String, atMillis: Long): Boolean {
        val stimulus = stimulatedAt[sessionId] ?: return false
        return atMillis - stimulus in 0..ReactionWindowMillis
    }

    fun forget(sessionId: String) {
        stimulatedAt.remove(sessionId)
    }
}
