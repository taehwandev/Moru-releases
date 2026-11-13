package app.thdev.moru.core.system

import java.awt.Taskbar

/**
 * Notifications through macOS and the badge on the Dock icon. Anywhere else, or if the system refuses, it does nothing
 * rather than fail: the app works the same without them.
 */
class DesktopAttentionNotifier(
    private val osName: String = System.getProperty("os.name").orEmpty(),
    private val run: (List<String>) -> Unit = { command -> ProcessBuilder(command).redirectErrorStream(true).start() },
) : AttentionNotifier {
    override fun show(title: String, body: String) {
        if (!osName.startsWith("Mac", ignoreCase = true)) return
        runCatching { run(osascriptNotificationCommand(title, body)) }
    }

    override fun badge(count: Int) {
        runCatching {
            if (!Taskbar.isTaskbarSupported()) return
            val taskbar = Taskbar.getTaskbar()
            if (taskbar.isSupported(Taskbar.Feature.ICON_BADGE_TEXT)) {
                taskbar.setIconBadge(if (count > 0) count.toString() else null)
            }
        }
    }
}
