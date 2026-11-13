package app.thdev.moru.core.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

/**
 * The global Project/Task catalog database. It lives in the operating-system
 * application-data directory (resolved by `app-desktop`), separate from the
 * project-local [MoruDatabase], so one catalog can own several Projects
 * including missing roots before any Project is opened.
 */
@Database(
    entities = [
        ProjectEntity::class,
        ProjectRootEntity::class,
        TaskEntity::class,
        TaskRevisionEntity::class,
        CatalogSelectionEntity::class,
        WorkContextEntity::class,
    ],
    version = 4,
)
@ConstructedBy(MoruCatalogDatabaseConstructor::class)
abstract class MoruCatalogDatabase : RoomDatabase() {
    abstract fun projectCatalogDao(): ProjectCatalogDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object MoruCatalogDatabaseConstructor : RoomDatabaseConstructor<MoruCatalogDatabase> {
    override fun initialize(): MoruCatalogDatabase
}

fun createMoruCatalogDatabase(
    builder: RoomDatabase.Builder<MoruCatalogDatabase>,
): MoruCatalogDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        // Migrate real schema changes so registered Projects/Tasks are not wiped.
        .addMigrations(CatalogMigration1To2, CatalogMigration2To3, CatalogMigration3To4)
        .build()
}
