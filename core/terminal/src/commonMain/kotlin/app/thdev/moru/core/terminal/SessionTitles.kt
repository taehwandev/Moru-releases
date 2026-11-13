package app.thdev.moru.core.terminal

import java.util.concurrent.ConcurrentHashMap

/**
 * What each session's program says it is doing, from the terminal title it sets (OSC 0/2). Claude Code and Codex keep
 * a short summary of the current work there, behind a spinner glyph. Like [SessionLastLines], the UI reads this on its
 * own timer, so it stays out of the session snapshot.
 */
object SessionTitles {
    private val titles = ConcurrentHashMap<String, String>()

    /** Keeps [title] once it says something; a generic title (the program's or shell's name, a path) clears it. */
    fun record(sessionId: String, title: String) {
        val text = agentTitleText(title)
        if (text == null) titles.remove(sessionId) else titles[sessionId] = text
    }

    fun titleFor(sessionId: String): String? = titles[sessionId]

    fun forget(sessionId: String) {
        titles.remove(sessionId)
    }
}
