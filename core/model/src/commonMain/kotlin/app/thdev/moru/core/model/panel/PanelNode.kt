package app.thdev.moru.core.model.panel

import kotlinx.serialization.Serializable

/**
 * A layout of panels: a [PanelSplit] lays its children along a row or a column, a [PanelSlot] names the n-th item the
 * host shows. It is shape and sizes only; what an item is belongs to the host that draws it.
 */
@Serializable
sealed interface PanelNode
