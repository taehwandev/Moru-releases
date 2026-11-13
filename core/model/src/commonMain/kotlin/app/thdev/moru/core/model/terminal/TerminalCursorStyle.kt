package app.thdev.moru.core.model.terminal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TerminalCursorStyle {
    @SerialName("block")
    Block,

    @SerialName("underline")
    Underline,

    @SerialName("bar")
    Bar,
}
