package app.thdev.moru.app

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities

/**
 * Offers to report an error nothing handled as a GitHub issue. The first one in a run is kept in [pendingFile] and
 * shown at once; the file goes once the user answered, so one that ended Moru before anyone saw it is offered on the
 * next start instead. Later errors in the same run are only logged, so an error that repeats does not stack alerts.
 */
internal class MoruCrashReports(
    appDataDir: File,
    private val version: String = MoruBuildVersion.value,
    private val system: String = moruSystemDescription(),
    /** Shows an alert with [title], [message] and Report Issue for [issueUrl]; returns once the user answered. */
    private val show: (title: String, message: String, issueUrl: String) -> Unit = ::showOnUiThread,
) {
    private val log = MoruErrorLog.path(appDataDir).path
    val pendingFile: File = appDataDir.resolve("logs").resolve("pending-crash.txt")
    private val offered = AtomicBoolean(false)

    /** Called for an error nothing handled on [thread]. */
    fun crashed(thread: Thread, error: Throwable) {
        if (!offered.compareAndSet(false, true)) return
        val reason = reasonOf(error)
        val url = crashIssueUrl(error, thread.name, version, system)
        runCatching { keep(reason, url) }
        val strings = systemAppStrings()
        runCatching {
            show(strings.crashTitle, strings.crashBody(reason, log), url)
            pendingFile.delete()
        }
    }

    /** Offers the error that ended the last run, if Moru ended before the user saw it. Call before the window opens. */
    fun offerPrevious() {
        val lines = runCatching { pendingFile.readLines() }.getOrNull() ?: return
        // Gone before it is shown, so an alert that fails or a Moru that ends again cannot offer it every start.
        pendingFile.delete()
        val reason = lines.getOrNull(0) ?: return
        val url = lines.getOrNull(1)?.takeIf { it.startsWith("https://github.com/") } ?: return
        val strings = systemAppStrings()
        runCatching { show(strings.crashLastTimeTitle, strings.crashLastTime(reason, log), url) }
    }

    private fun keep(reason: String, url: String) {
        pendingFile.parentFile.mkdirs()
        val saving = File(pendingFile.path + ".saving")
        saving.writeText(reason.lineSequence().first() + "\n" + url + "\n")
        Files.move(saving.toPath(), pendingFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private fun reasonOf(error: Throwable): String {
        val reason = generateSequence(error) { it.cause }.mapNotNull { it.message }.firstOrNull() ?: error::class.simpleName.orEmpty()
        val home = System.getProperty("user.home").orEmpty()
        return if (home.length > 1) reason.replace(home, "~") else reason
    }
}

private fun showOnUiThread(title: String, message: String, issueUrl: String) {
    val alert = { showReportAlert(systemAppStrings(), title, message) { issueUrl } }
    if (SwingUtilities.isEventDispatchThread()) alert() else SwingUtilities.invokeAndWait(alert)
}
