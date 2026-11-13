package app.thdev.moru.core.data

import app.thdev.moru.core.domain.SettingsRepository
import java.io.File
import okio.Path.Companion.toPath

/**
 * The app's settings, kept once in the app data folder: theme, fonts, terminal and alerts are the user's, not a
 * Project's. Settings used to live in each opened folder's .moru-community/settings.json; the
 * first time the shared file is missing, the one from [legacyProjectRoot] is carried over so nothing the user chose
 * is lost. The old files are left.
 */
fun defaultDesktopSettingsRepository(appDataDir: File, legacyProjectRoot: File? = null): SettingsRepository {
    val settings = appDataDir.resolve("settings.json")
    if (!settings.exists()) {
        listOf(".moru-community").firstNotNullOfOrNull { folder ->
            legacyProjectRoot?.resolve("$folder/settings.json")?.takeIf { it.isFile }
        }?.let { legacy ->
            runCatching { legacy.copyTo(settings.apply { parentFile.mkdirs() }) }
        }
    }
    return FileBackedSettingsRepository(settings.absolutePath.toPath())
}
