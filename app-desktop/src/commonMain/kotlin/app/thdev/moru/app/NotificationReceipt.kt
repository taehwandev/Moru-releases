package app.thdev.moru.app

/** Platform receipt metadata; notification content never contributes to identity. */
internal expect object NotificationReceipt {
    fun id(): String
    fun now(): Long
}
