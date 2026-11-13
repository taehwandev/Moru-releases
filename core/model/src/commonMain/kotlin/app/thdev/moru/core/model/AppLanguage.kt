package app.thdev.moru.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The language the user chose for the app's own text. [System] follows the computer's language. */
@Serializable
enum class AppLanguage {
    @SerialName("system")
    System,

    @SerialName("en")
    English,

    @SerialName("ko")
    Korean,
}
