package app.thdev.moru.app

import javax.swing.JRootPane

/**
 * Makes the native title bar part of the window on macOS, so it shows the app's own colors instead of following the
 * operating system's light or dark setting. The app draws under a transparent title bar and the title text is hidden;
 * [topInset] keeps the content clear of the window buttons. Elsewhere nothing changes. The menu bar at the top of the
 * screen belongs to macOS and keeps following the system.
 */
internal object MoruWindowChrome {
    private const val MacTopInsetDp = 28

    internal val macProperties: Map<String, Boolean> = mapOf(
        "apple.awt.fullWindowContent" to true,
        "apple.awt.transparentTitleBar" to true,
        "apple.awt.windowTitleVisible" to false,
    )

    fun isMac(osName: String): Boolean = osName.startsWith("Mac", ignoreCase = true)

    /** Height in dp to keep free at the top of the content for the title bar area. */
    fun topInsetDp(osName: String = System.getProperty("os.name").orEmpty()): Int = if (isMac(osName)) MacTopInsetDp else 0

    fun apply(rootPane: JRootPane, osName: String = System.getProperty("os.name").orEmpty()) {
        if (!isMac(osName)) return
        macProperties.forEach { (key, value) -> rootPane.putClientProperty(key, value) }
    }
}
