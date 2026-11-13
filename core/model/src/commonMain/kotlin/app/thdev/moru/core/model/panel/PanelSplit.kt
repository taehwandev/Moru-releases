package app.thdev.moru.core.model.panel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** [children] along [axis], each taking its share (0..1, summing to 1) of the room. */
@Serializable
@SerialName("split")
data class PanelSplit(
    val axis: PanelAxis,
    val children: List<PanelNode>,
    val shares: List<Float> = List(children.size) { 1f / children.size },
) : PanelNode
