package app.thdev.moru.core.runtime

/**
 * Whether [candidate] is a later release than [current]. Numeric parts are compared in order, a missing part counts
 * as 0, and a prerelease (`1.2.0-beta`) comes before its release (`1.2.0`).
 */
fun isNewerCliVersion(candidate: String, current: String): Boolean {
    val (candidateParts, candidatePre) = versionParts(candidate) ?: return false
    val (currentParts, currentPre) = versionParts(current) ?: return false
    for (index in 0 until maxOf(candidateParts.size, currentParts.size)) {
        val a = candidateParts.getOrElse(index) { 0 }
        val b = currentParts.getOrElse(index) { 0 }
        if (a != b) return a > b
    }
    return currentPre && !candidatePre
}

private fun versionParts(version: String): Pair<List<Int>, Boolean>? {
    val parts = version.substringBefore('-').split('.').map { part -> part.toIntOrNull() ?: return null }
    return parts to version.contains('-')
}
