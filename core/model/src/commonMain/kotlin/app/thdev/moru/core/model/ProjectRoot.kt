package app.thdev.moru.core.model

/**
 * One stable root reference for a Project.
 *
 * The root id is opaque and stable across relocation. [canonicalPath] is present
 * only while the root is accessible and is used to reject registering the same
 * root twice; [lastKnownPath] is always retained so a missing or permission-denied
 * root still shows where it used to be.
 */
data class ProjectRoot(
    val id: String,
    val role: ProjectRootRole,
    val lastKnownPath: String,
    val canonicalPath: String? = null,
    val availability: ProjectAvailability = ProjectAvailability.Available,
)
