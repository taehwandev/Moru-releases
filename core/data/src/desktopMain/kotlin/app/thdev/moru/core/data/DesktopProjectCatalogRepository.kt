package app.thdev.moru.core.data

import java.io.File
import java.util.UUID

/** Wire a Room-backed catalog repository for the desktop app. */
fun defaultDesktopProjectCatalogRepository(appDataDir: File): RoomProjectCatalogRepository =
    RoomProjectCatalogRepository(
        database = defaultDesktopMoruCatalogDatabase(appDataDir),
        canonicalizer = DesktopProjectRootCanonicalizer(),
        idGenerator = { UUID.randomUUID().toString() },
        nowEpochMillis = { System.currentTimeMillis() },
    )
