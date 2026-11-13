package app.thdev.moru.app

import java.util.UUID

internal actual object NotificationReceipt {
    actual fun id(): String = UUID.randomUUID().toString()
    actual fun now(): Long = System.currentTimeMillis()
}
