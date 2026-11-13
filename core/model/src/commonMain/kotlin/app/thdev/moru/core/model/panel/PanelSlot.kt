package app.thdev.moru.core.model.panel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The host's item at [index], in the host's own order. */
@Serializable
@SerialName("slot")
data class PanelSlot(val index: Int) : PanelNode
