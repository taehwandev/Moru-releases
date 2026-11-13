package app.thdev.moru.core.data

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v2 -> v3: add `catalog_project.closed_at` so a Project can be closed (hidden, Tasks kept) and restored by
 * opening its folder again. A real migration so registered Projects and Tasks survive the schema bump.
 */
val CatalogMigration2To3: Migration = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `catalog_project` ADD COLUMN `closed_at` INTEGER DEFAULT NULL")
    }
}
