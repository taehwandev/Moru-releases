package app.thdev.moru.core.data

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v1 -> v2: add the `catalog_work_context` table. A real migration (not a
 * destructive drop) so registered Projects and Tasks survive the schema bump.
 */
val CatalogMigration1To2: Migration = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `catalog_work_context` " +
                "(`task_id` TEXT NOT NULL, `id` TEXT NOT NULL, `lifecycle` TEXT NOT NULL, " +
                "`branch` TEXT, `worktree_path` TEXT, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`task_id`))",
        )
    }
}
