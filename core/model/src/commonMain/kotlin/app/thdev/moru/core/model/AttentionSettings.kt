package app.thdev.moru.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** How Moru tells the user a Task needs them: system notifications, which are off unless the user turns them on. */
@Serializable
data class AttentionSettings(
    @SerialName("desktop_notifications")
    val desktopNotifications: Boolean = false,
)
