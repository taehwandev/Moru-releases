package app.thdev.moru.core.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

fun defaultDesktopNotificationRepository(appDataDir: File): RoomNotificationRepository {
    val directory = appDataDir.resolve("notifications").apply { mkdirs() }
    val database = Room.databaseBuilder<MoruNotificationDatabase>(
        name = directory.resolve("notifications.db").absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
    return RoomNotificationRepository(database)
}
