package app.thdev.moru.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A Project open in a window of its own, with where that window was in dp on the screen; a null place or size lets the
 * window take the system's default.
 */
@Serializable
data class SavedProjectWindow(
    @SerialName("project_id") val projectId: String,
    val x: Float? = null,
    val y: Float? = null,
    val width: Float? = null,
    val height: Float? = null,
)
