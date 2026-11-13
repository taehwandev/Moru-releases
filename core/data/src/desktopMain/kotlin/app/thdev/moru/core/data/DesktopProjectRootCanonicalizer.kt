package app.thdev.moru.core.data

import app.thdev.moru.core.model.ProjectAvailability
import java.io.File

/** Resolves a registration path to its canonical identity and current availability on disk. */
class DesktopProjectRootCanonicalizer : ProjectRootCanonicalizer {
    override fun canonicalize(rootPath: String): CanonicalRoot {
        val file = File(rootPath)
        val lastKnownPath = runCatching { file.absoluteFile.path }.getOrDefault(rootPath)
        return when {
            !file.exists() -> CanonicalRoot(
                lastKnownPath = lastKnownPath,
                canonicalPath = null,
                availability = ProjectAvailability.Missing,
            )
            !file.canRead() -> CanonicalRoot(
                lastKnownPath = lastKnownPath,
                canonicalPath = runCatching { file.canonicalFile.path }.getOrNull(),
                availability = ProjectAvailability.PermissionDenied,
            )
            else -> CanonicalRoot(
                lastKnownPath = lastKnownPath,
                canonicalPath = runCatching { file.canonicalFile.path }.getOrDefault(lastKnownPath),
                availability = ProjectAvailability.Available,
            )
        }
    }
}
