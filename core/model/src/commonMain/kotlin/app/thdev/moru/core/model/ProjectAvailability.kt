package app.thdev.moru.core.model

enum class ProjectAvailability {
    Available,
    Missing,
    PermissionDenied,
}

val ProjectAvailability.wireValue: String
    get() = when (this) {
        ProjectAvailability.Available -> "available"
        ProjectAvailability.Missing -> "missing"
        ProjectAvailability.PermissionDenied -> "permission_denied"
    }

fun projectAvailabilityFromWireValue(value: String): ProjectAvailability =
    when (value) {
        "available" -> ProjectAvailability.Available
        "missing" -> ProjectAvailability.Missing
        "permission_denied" -> ProjectAvailability.PermissionDenied
        else -> ProjectAvailability.Missing
    }
