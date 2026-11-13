package app.thdev.moru.core.model.panel

/** Which side of a panel another panel is put on. */
enum class PanelEdge {
    Left, Right, Top, Bottom;

    internal val axis: PanelAxis get() = if (this == Left || this == Right) PanelAxis.Row else PanelAxis.Column

    /** Whether the panel put on this side comes before the one it is put beside. */
    internal val before: Boolean get() = this == Left || this == Top
}
