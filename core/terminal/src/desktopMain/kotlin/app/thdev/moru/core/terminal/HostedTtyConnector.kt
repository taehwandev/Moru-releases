package app.thdev.moru.core.terminal

import app.thdev.moru.core.terminal.host.HostTtyConnector
import com.jediterm.core.util.TermSize
import java.nio.charset.Charset

/** A terminal in the terminal host, observed the same way as one running inside Moru: output time, addresses, commands. */
internal class HostedTtyConnector(
    private val hosted: HostTtyConnector,
    private val charset: Charset,
    private val onCommandCaptured: (String) -> Unit,
    private val onOutput: () -> Unit,
    private val onLocalServerUrl: (String) -> Unit,
    /** Null when nobody follows raw output lines; the screen is observed instead, so they are not parsed. */
    private val onOutputLine: ((String) -> Unit)?,
    /** Called when Moru resizes the terminal or sends it input; see [SessionStimuli]. */
    private val onStimulus: () -> Unit = {},
    private val onBackgroundColorRead: (rgb: Int) -> Unit = {},
    private val onInputSubmitted: () -> Unit = {},
) : TerminableTtyConnector, ThemeReportingTtyConnector {
    private val inputParser = TerminalCommandInputParser()
    private val urlDetector = LocalServerUrlDetector()
    private val lineTracker = onOutputLine?.let { LastOutputLineTracker() }
    private var reportedLine: String? = null
    private val themeReport = ThemeChangeReport()

    override fun read(buf: CharArray, offset: Int, length: Int): Int {
        val read = hosted.read(buf, offset, length)
        if (read > 0) {
            onOutput()
            val text = String(buf, offset, read)
            themeReport.accept(text)
            urlDetector.accept(text)?.let(onLocalServerUrl)
            lineTracker?.accept(text)?.let { line ->
                if (line != reportedLine) {
                    reportedLine = line
                    onOutputLine?.invoke(line)
                }
            }
        }
        return read
    }

    override fun write(bytes: ByteArray) {
        onStimulus()
        val text = String(bytes, charset)
        val commands = inputParser.accept(text, onInputSubmitted)
        hosted.write(bytes)
        terminalBackgroundColorReply(text)?.let { rgb ->
            if (!hosted.consumeReplayBackgroundReply()) onBackgroundColorRead(rgb)
        }
        commands.forEach(onCommandCaptured)
    }

    override fun write(string: String) = write(string.toByteArray(charset))

    override fun reportThemeChange(dark: Boolean) {
        val sequence = themeReport.sequenceFor(dark) ?: return
        onStimulus()
        hosted.write(sequence.toByteArray(charset))
    }

    override fun isConnected(): Boolean = hosted.isConnected

    override fun resize(termSize: TermSize) {
        onStimulus()
        hosted.resize(termSize)
    }

    override fun waitFor(): Int = hosted.waitFor()

    override fun ready(): Boolean = hosted.ready()

    override fun getName(): String = hosted.name

    override fun close() = hosted.close()

    override fun terminate() = hosted.terminate()
}
