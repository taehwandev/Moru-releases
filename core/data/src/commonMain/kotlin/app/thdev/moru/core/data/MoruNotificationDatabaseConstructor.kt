package app.thdev.moru.core.data

import androidx.room.RoomDatabaseConstructor

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object MoruNotificationDatabaseConstructor : RoomDatabaseConstructor<MoruNotificationDatabase> {
    override fun initialize(): MoruNotificationDatabase
}
