package app.thdev.moru.core.data

import androidx.room.Room
import java.io.File

/**
 * Builds the global Project/Task catalog database inside the operating-system
 * application-data directory. `app-desktop` resolves [appDataDir]; shared code
 * never hard-codes a platform or home path.
 */
fun defaultDesktopMoruCatalogDatabase(appDataDir: File): MoruCatalogDatabase {
    val catalogDir = appDataDir.resolve("catalog").apply { mkdirs() }
    return createMoruCatalogDatabase(
        Room.databaseBuilder<MoruCatalogDatabase>(
            name = catalogDir.resolve("moru-catalog.db").absolutePath,
        ),
    )
}
