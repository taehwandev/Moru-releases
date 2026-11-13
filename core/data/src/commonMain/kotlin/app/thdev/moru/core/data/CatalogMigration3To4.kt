package app.thdev.moru.core.data

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v3 -> v4: add `catalog_task.last_agent` so a Task remembers the agent it was started with. A real migration so
 * registered Projects and Tasks survive the schema bump.
 */
val CatalogMigration3To4: Migration = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `catalog_task` ADD COLUMN `last_agent` TEXT DEFAULT NULL")
    }
}
