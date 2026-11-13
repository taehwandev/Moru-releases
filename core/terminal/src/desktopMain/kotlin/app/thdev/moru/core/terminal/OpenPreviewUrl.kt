package app.thdev.moru.core.terminal

import java.awt.Desktop
import java.net.URI

/** Opens [url] in the default browser. Returns false, and opens nothing, if it is not a local address or no browser is available. */
fun openPreviewUrl(url: String): Boolean {
    if (!isLocalPreviewUrl(url)) return false
    return runCatching {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
            true
        } else {
            false
        }
    }.getOrDefault(false)
}
