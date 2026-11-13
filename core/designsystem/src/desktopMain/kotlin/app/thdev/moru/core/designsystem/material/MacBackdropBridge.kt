package app.thdev.moru.core.designsystem.material

import com.sun.jna.Native
import java.nio.file.Files

/** Loads the bundled, optionally signed AppKit bridge once. Call initialization away from the EDT. */
internal object MacBackdropBridge {
    val api: MacBackdropApi? by lazy {
        if (!System.getProperty("os.name").startsWith("Mac", ignoreCase = true)) null
        else runCatching {
            val resource = javaClass.getResourceAsStream("/macos-backdrop/libmoru-backdrop.dylib")
                ?: return@runCatching null
            val path = Files.createTempFile("moru-backdrop-", ".dylib")
            path.toFile().deleteOnExit()
            resource.use { input -> Files.newOutputStream(path).use { output -> input.copyTo(output) } }
            Native.load(path.toString(), MacBackdropApi::class.java)
        }.getOrNull()
    }
}
