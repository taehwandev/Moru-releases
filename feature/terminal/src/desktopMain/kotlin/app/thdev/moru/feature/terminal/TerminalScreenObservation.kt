package app.thdev.moru.feature.terminal

import app.thdev.moru.core.terminal.screenBusy
import app.thdev.moru.core.terminal.screenQuestion
import app.thdev.moru.core.terminal.screenPermission
import app.thdev.moru.core.terminal.SessionPermissionPrompt
import app.thdev.moru.core.terminal.terminalScreenRows
import app.thdev.moru.core.terminal.terminalScreenSummary
import com.jediterm.terminal.model.TerminalModelListener
import com.jediterm.terminal.model.TerminalTextBuffer
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities
import javax.swing.Timer
import app.thdev.moru.core.terminal.screenReplyQuestion
import app.thdev.moru.core.terminal.screenLastReply

/**
 * Coalesces emulator changes instead of walking the screen for each character or retaining output. Reports the newest
 * line through [onLine]; through [onQuestion], the question the screen asks the user, or null once it is answered; and
 * through [onBusy], whether the screen says the program is at work; through [onReplyQuestion], the question an agent
 * ended its finished turn with, or null; through [onLastReply], that turn's whole last message. All come from one read of the screen, made only after it changes.
 */
internal class TerminalScreenObservation(
    private val buffer: TerminalTextBuffer,
    private val onLine: (String) -> Unit,
    private val onQuestion: (String?) -> Unit = {},
    private val onBusy: (Boolean) -> Unit = {},
    private val onPermission: (SessionPermissionPrompt?) -> Unit = {},
    private val onReplyQuestion: (String?) -> Unit = {},
    private val onLastReply: (String) -> Unit = {},
) {
    private val pending = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private var lastLine: String? = null
    private var lastQuestion: String? = null
    private var lastBusy = false
    private var lastReply: String? = null
    private var lastMessage: String? = null
    private val listener = TerminalModelListener { schedule() }
    private val timer = Timer(250) { sample() }.apply { isRepeats = false }

    init {
        buffer.addModelListener(listener)
        schedule()
    }

    private fun schedule() {
        if (closed.get() || !pending.compareAndSet(false, true)) return
        SwingUtilities.invokeLater {
            if (!closed.get()) timer.start()
        }
    }

    private fun sample() {
        pending.set(false)
        if (closed.get()) return
        if (!buffer.tryLock()) {
            schedule()
            return
        }
        val line: String?
        val rows: List<String>
        try {
            line = terminalScreenSummary(buffer)
            rows = terminalScreenRows(buffer)
        } finally {
            buffer.unlock()
        }
        if (closed.get()) return
        val question = screenQuestion(rows)
        onPermission(screenPermission(rows))
        val busy = screenBusy(rows)
        if (line != null && line != lastLine) {
            lastLine = line
            onLine(line)
        }
        if (question != lastQuestion) {
            lastQuestion = question
            onQuestion(question)
        }
        if (busy != lastBusy) {
            lastBusy = busy
            onBusy(busy)
        }
        // A turn still at work, or blocked on a choice, is not waiting for a reply yet.
        val reply = if (busy || question != null) null else screenReplyQuestion(rows)
        if (reply != lastReply) {
            lastReply = reply
            onReplyQuestion(reply)
        }
        // The message a finished turn leaves stays the result until another finished turn replaces it.
        val message = if (busy || question != null) null else screenLastReply(rows)
        if (message != null && message != lastMessage) {
            lastMessage = message
            onLastReply(message)
        }
    }

    fun close() {
        closed.set(true)
        timer.stop()
        buffer.removeModelListener(listener)
    }
}
