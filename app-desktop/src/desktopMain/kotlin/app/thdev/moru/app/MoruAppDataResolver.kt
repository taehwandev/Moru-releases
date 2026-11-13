package app.thdev.moru.app

import java.io.File

/**
 * Resolve the operating-system application-data directory that owns the global
 * Project/Task catalog. Shared modules never hard-code a platform path; the app
 * shell resolves it here and passes it into `core:data`. A development build
 * (`gradlew run`) keeps its own folder, "Moru Community Dev", so it never shares the
 * catalog, settings, notifications or terminal host with the installed app.
 */
internal fun resolveMoruAppDataDir(
    osName: String = System.getProperty("os.name").orEmpty(),
    userHome: File = File(System.getProperty("user.home")),
    appDataOverride: String? = System.getProperty("moru.appDataDir") ?: System.getenv("MORU_APP_DATA_DIR"),
    development: Boolean = MoruDesktopRuntimeProperties.development,
): File {
    appDataOverride
        ?.trim()
        ?.takeIf { path -> path.isNotEmpty() }
        ?.let(::File)
        ?.let { file -> return file.absoluteFile }

    val folder = if (development) "Moru Community Dev" else "Moru Community"
    val lowerOs = osName.lowercase()
    val base = when {
        lowerOs.contains("mac") || lowerOs.contains("darwin") ->
            userHome.resolve("Library").resolve("Application Support").resolve(folder)
        lowerOs.contains("win") ->
            (System.getenv("APPDATA")?.let(::File) ?: userHome.resolve("AppData").resolve("Roaming"))
                .resolve(folder)
        else ->
            (System.getenv("XDG_DATA_HOME")?.let(::File) ?: userHome.resolve(".local").resolve("share"))
                .resolve(folder)
    }
    return base.absoluteFile
}
