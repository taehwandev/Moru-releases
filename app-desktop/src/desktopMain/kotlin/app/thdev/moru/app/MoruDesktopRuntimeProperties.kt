package app.thdev.moru.app

import app.thdev.moru.core.model.AppIcon
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

internal object MoruDesktopRuntimeProperties {
    /** A `gradlew run` build: its own name, icon and data, apart from the installed app. */
    val development: Boolean get() = java.lang.Boolean.getBoolean("moru.development")

    /** The folder inside each project that holds its local Moru data; a development build keeps its own. */
    val projectDataFolder: String get() = if (development) ".moru-community-dev" else ".moru-community"
    val applicationName: String get() = if (development) "Moru Community Dev" else "Moru Community"

    fun configure(
        setProperty: (String, String) -> Unit = { key, value -> System.setProperty(key, value) },
    ) {
        setProperty(AppleApplicationNameProperty, applicationName)
        // Native owned windows preserve both drawing and hit testing above Swing terminals.
        setProperty(ComposeLayersTypeProperty, "WINDOW")
        // Swing on Java2D's Metal pipeline keeps native graphics memory each time a window, split or terminal is
        // resized (about 3MB a resize, 10GB after days of use), held outside any Java object so no GC returns it. The
        // OpenGL pipeline draws the terminals just as sharply on Retina and keeps no such memory. Compose is not
        // affected: it renders through its own Metal layer.
        setProperty(Java2DMetalProperty, "false")
    }

    /** The window and macOS launcher use the same development artwork. */
    /** The picture of [choice] (Settings › Appearance); the default icon is the development one in a development build. */
    fun applicationIcon(choice: AppIcon = AppIcon.Moru): BufferedImage {
        // This build ships one picture; a choice without a picture of its own shows the default one.
        val default = if (development) "/icon-dev.png" else "/icon.png"
        val chosen = "/app-icons/${choice.id}.png".takeIf { choice != AppIcon.Moru && javaClass.getResource(it) != null }
        val resource = chosen ?: default
        return requireNotNull(javaClass.getResourceAsStream(resource)) { "no app icon $resource" }.use { ImageIO.read(it) }
    }

    fun warmUpGraphicsEnvironmentOnFirstThread(
        osName: String = System.getProperty("os.name").orEmpty(),
        isHeadless: () -> Boolean = GraphicsEnvironment::isHeadless,
        warmUp: () -> Unit = {
            val graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment()
            val defaultConfiguration = graphicsEnvironment.defaultScreenDevice.defaultConfiguration
            Toolkit.getDefaultToolkit().getScreenInsets(defaultConfiguration)
        },
    ) {
        if (osName.contains("Mac", ignoreCase = true) && !isHeadless()) {
            warmUp()
        }
    }

    internal const val AppleApplicationNameProperty = "apple.awt.application.name"
    internal const val ComposeLayersTypeProperty = "compose.layers.type"
    internal const val Java2DMetalProperty = "sun.java2d.metal"
}
