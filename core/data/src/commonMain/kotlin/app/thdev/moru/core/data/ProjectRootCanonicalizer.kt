package app.thdev.moru.core.data

/**
 * Resolves a registration path to its canonical identity and current
 * availability. Path resolution is platform-specific, so it is injected to keep
 * the catalog repository free of filesystem types and unit-testable.
 */
fun interface ProjectRootCanonicalizer {
    fun canonicalize(rootPath: String): CanonicalRoot
}
