package app.thdev.moru.app

import app.thdev.moru.app.i18n.AppStrings
import app.thdev.moru.app.i18n.appStrings
import app.thdev.moru.core.model.AppLanguage
import java.awt.Desktop
import java.net.URI
import java.util.Locale
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

/**
 * Logs [error] and tells the user, in a native alert, why Moru could not start. Report Issue opens a new GitHub issue
 * filled in with the error (see [startupIssueUrl]) in the browser; nothing is sent until the user submits it there.
 */
internal fun showStartupFailure(error: Throwable) {
    System.err.println("Moru could not start:")
    error.printStackTrace()
    val log = MoruErrorLog.path(resolveMoruAppDataDir()).path
    val strings = systemAppStrings()
    runCatching {
        SwingUtilities.invokeAndWait {
            showReportAlert(strings, strings.startupFailedTitle, startupFailureText(strings, error, log)) {
                startupIssueUrl(error, MoruBuildVersion.value, moruSystemDescription())
            }
        }
    }
}

/** The app's text in the computer's language, for alerts shown before or outside any window and its settings. */
internal fun systemAppStrings(): AppStrings = appStrings(resolveUiLanguage(AppLanguage.System, Locale.getDefault().toLanguageTag()))

/** A native error alert with Report Issue, which opens [issueUrl] in the browser, and Close. Call on the UI thread. */
internal fun showReportAlert(strings: AppStrings, title: String, message: String, issueUrl: () -> String) {
    val options = arrayOf(strings.startupReportIssue, strings.startupClose)
    val chosen = JOptionPane.showOptionDialog(
        null, message, title, JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[1],
    )
    if (chosen == 0) {
        runCatching { Desktop.getDesktop().browse(URI(issueUrl())) }
            .onFailure { System.err.println("Moru could not open the issue page: $it") }
    }
}
