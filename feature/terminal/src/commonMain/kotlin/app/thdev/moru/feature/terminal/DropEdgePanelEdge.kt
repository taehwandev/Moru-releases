package app.thdev.moru.feature.terminal

import app.thdev.moru.core.designsystem.component.MoruDropEdge
import app.thdev.moru.core.model.panel.PanelEdge

/** The side of a terminal a drop puts the dragged one on, or null for the middle, which swaps the two. */
internal fun MoruDropEdge.panelEdge(): PanelEdge? = when (this) {
    MoruDropEdge.Center -> null
    MoruDropEdge.Left -> PanelEdge.Left
    MoruDropEdge.Right -> PanelEdge.Right
    MoruDropEdge.Top -> PanelEdge.Top
    MoruDropEdge.Bottom -> PanelEdge.Bottom
}
