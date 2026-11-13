package app.thdev.moru.core.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        TerminalWorkspaceEntity::class,
        TerminalWorkspaceStateEntity::class,
    ],
    version = 1,
)
@ConstructedBy(MoruDatabaseConstructor::class)
abstract class MoruDatabase : RoomDatabase() {
    abstract fun terminalWorkspaceDao(): TerminalWorkspaceDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object MoruDatabaseConstructor : RoomDatabaseConstructor<MoruDatabase> {
    override fun initialize(): MoruDatabase
}

fun createMoruDatabase(
    builder: RoomDatabase.Builder<MoruDatabase>,
): MoruDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
