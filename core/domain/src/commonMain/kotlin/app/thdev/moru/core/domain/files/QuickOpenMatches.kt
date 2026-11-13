package app.thdev.moru.core.domain.files

private const val MAX_RESULTS = 40

/** Portable file-name-first ranking for a bounded project path snapshot. */
fun quickOpenMatches(query: String, paths: List<String>): List<String> {
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return emptyList()
    return paths.mapNotNull { path ->
        val score = score(needle, path) ?: return@mapNotNull null
        path to score
    }.sortedWith(compareBy({ (_, score) -> score }, { (path, _) -> path.length }, { (path, _) -> path }))
        .take(MAX_RESULTS)
        .map { (path, _) -> path }
}

private fun score(needle: String, path: String): Int? {
    val name = path.substringAfterLast('/').lowercase()
    return when {
        name.startsWith(needle) -> 0
        name.contains(needle) -> 1
        isSubsequence(needle, name) -> 2
        path.lowercase().contains(needle) -> 3
        else -> null
    }
}

private fun isSubsequence(needle: String, haystack: String): Boolean {
    var index = 0
    for (char in haystack) {
        if (index < needle.length && char == needle[index]) index++
    }
    return index == needle.length
}
