package app.thdev.moru.app

import app.thdev.moru.app.i18n.appStrings
import app.thdev.moru.core.designsystem.theme.UiLanguage
import kotlin.test.Test
import kotlin.test.assertEquals

class StartupFailureTextTest {
    private val strings = appStrings(UiLanguage.English)

    @Test
    fun dataSavedByANewerMoruAsksForThatMoruInsteadOfReportingAnError() {
        // What Room throws when the catalog on disk is at version 5 and this Moru knows up to 4.
        val error = RuntimeException("wrapped", IllegalStateException("A migration from 5 to 4 was required but not found."))

        assertEquals(strings.startupNeedsNewerMoru("/logs/moru.log"), startupFailureText(strings, error, "/logs/moru.log"))
    }

    @Test
    fun aCatalogSavedByANewerMoruIsRecognisedFromWhatRoomActuallyThrows() {
        val dir = java.nio.file.Files.createTempDirectory("moru-catalog-newer").toFile()
        try {
            fun load(): Throwable? {
                val database = app.thdev.moru.core.data.defaultDesktopMoruCatalogDatabase(dir)
                val repository = app.thdev.moru.core.data.RoomProjectCatalogRepository(
                    database, app.thdev.moru.core.data.DesktopProjectRootCanonicalizer(),
                    idGenerator = { java.util.UUID.randomUUID().toString() }, nowEpochMillis = System::currentTimeMillis,
                )
                return runCatching { repository.loadCatalog() }.exceptionOrNull().also { database.close() }
            }
            assertEquals(null, load())
            // A catalog a newer Moru left behind: SQLite keeps the schema version at byte 60 of the file.
            java.io.RandomAccessFile(dir.resolve("catalog/moru-catalog.db"), "rw").use { file ->
                file.seek(60)
                file.writeInt(99)
            }
            val failure = load()

            kotlin.test.assertNotNull(failure, "Room opened a catalog from a newer schema")
            assertEquals(strings.startupNeedsNewerMoru("/l"), startupFailureText(strings, failure, "/l"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun anyOtherFailureSaysWhatWentWrong() {
        val upgrade = IllegalStateException("A migration from 3 to 4 was required but not found.")
        assertEquals(strings.startupFailed(upgrade.message!!, "/l"), startupFailureText(strings, upgrade, "/l"))
        assertEquals(strings.startupFailed("IllegalStateException", "/l"), startupFailureText(strings, IllegalStateException(), "/l"))
    }
}
