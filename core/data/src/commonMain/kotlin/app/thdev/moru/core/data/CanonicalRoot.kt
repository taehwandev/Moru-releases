package app.thdev.moru.core.data

import app.thdev.moru.core.model.ProjectAvailability

/** Canonical identity and availability for a root, resolved at registration time. */
data class CanonicalRoot(
    val lastKnownPath: String,
    val canonicalPath: String?,
    val availability: ProjectAvailability,
)
