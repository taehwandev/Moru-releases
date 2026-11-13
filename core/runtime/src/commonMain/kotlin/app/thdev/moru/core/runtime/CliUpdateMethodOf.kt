package app.thdev.moru.core.runtime

/** The update method for an executable whose symlinks resolve to [realPath]. */
fun cliUpdateMethodOf(realPath: String): CliUpdateMethod {
    val segments = realPath.split('/').filter { segment -> segment.isNotEmpty() }
    segments.segmentAfter("Caskroom")?.let { token -> return CliUpdateMethod.Homebrew(token, cask = true) }
    segments.segmentAfter("Cellar")?.let { formula -> return CliUpdateMethod.Homebrew(formula, cask = false) }
    val modules = segments.lastIndexOf("node_modules")
    if (modules >= 0) {
        val first = segments.getOrNull(modules + 1)
        val name = if (first?.startsWith("@") == true) segments.getOrNull(modules + 2)?.let { "$first/$it" } else first
        if (name != null) return CliUpdateMethod.Npm(name)
    }
    return CliUpdateMethod.SelfUpdate
}

private fun List<String>.segmentAfter(marker: String): String? =
    indexOf(marker).takeIf { it >= 0 }?.let { getOrNull(it + 1) }
