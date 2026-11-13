package app.thdev.moru.core.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase

/** App-global local history, isolated from catalog and project databases. */
@Database(entities = [NotificationEntity::class], version = 1)
@ConstructedBy(MoruNotificationDatabaseConstructor::class)
abstract class MoruNotificationDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
}
