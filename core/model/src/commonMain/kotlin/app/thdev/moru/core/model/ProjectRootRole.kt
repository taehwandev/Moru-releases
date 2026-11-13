package app.thdev.moru.core.model

enum class ProjectRootRole {
    Primary,
    Secondary,
}

val ProjectRootRole.wireValue: String
    get() = when (this) {
        ProjectRootRole.Primary -> "primary"
        ProjectRootRole.Secondary -> "secondary"
    }

fun projectRootRoleFromWireValue(value: String): ProjectRootRole =
    when (value) {
        "primary" -> ProjectRootRole.Primary
        "secondary" -> ProjectRootRole.Secondary
        else -> ProjectRootRole.Secondary
    }
