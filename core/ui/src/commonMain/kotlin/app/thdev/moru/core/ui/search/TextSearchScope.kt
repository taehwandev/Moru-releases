package app.thdev.moru.core.ui.search

/** An optional reader concern; callers own queries, navigation and the lifetime of a search. */
data class TextSearchScope(
    val query: String,
    val active: TextSearchMatch?,
    val path: String,
    val revision: Int = 0,
) {
    fun child(key: String): TextSearchScope = copy(path = "$path/$key")
}
