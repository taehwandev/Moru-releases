package app.thdev.moru.app

import java.awt.Image
import java.awt.Taskbar

/**
 * Shows [image] as the app's Dock icon while it runs. Only the running app's icon changes: the app bundle's own icon,
 * which Finder and Launchpad show, stays as it is, since changing it would break the app's signature.
 */
internal object MoruDockIcon {
    fun show(image: Image) {
        runCatching {
            if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
                Taskbar.getTaskbar().iconImage = image
            }
        }
    }
}
