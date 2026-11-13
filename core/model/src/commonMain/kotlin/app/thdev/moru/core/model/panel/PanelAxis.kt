package app.thdev.moru.core.model.panel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Side by side ([PanelAxis.Row]) or stacked ([PanelAxis.Column]). */
@Serializable
enum class PanelAxis {
    @SerialName("row") Row,
    @SerialName("column") Column,
}
