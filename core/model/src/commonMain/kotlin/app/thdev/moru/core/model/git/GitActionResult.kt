package app.thdev.moru.core.model.git

sealed interface GitActionResult {
    val message: String

    data class Succeeded(override val message: String) : GitActionResult
    data class Rejected(override val message: String) : GitActionResult
    data class Failed(override val message: String) : GitActionResult
}
