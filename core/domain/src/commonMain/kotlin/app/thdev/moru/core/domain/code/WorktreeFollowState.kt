package app.thdev.moru.core.domain.code

/** Sticky checkout selection. The main shell never replaces a previously observed non-main checkout. */
data class WorktreeFollowState(val shown: String? = null, val seen: String? = null) {
    fun observe(roots: List<String>, checkoutPaths: List<String>, mainPath: String?): WorktreeFollowState {
        // Failed/empty inventory is not evidence that the chosen checkout disappeared.
        if (checkoutPaths.isEmpty()) return this
        val known = checkoutPaths.toSet()
        val found = roots.filter { root -> root in known && root != mainPath }
        val current = seen?.takeIf { it in found } ?: found.firstOrNull()
        val moved = current != null && current != seen
        return WorktreeFollowState(
            shown = when {
                moved -> current
                shown in known -> shown
                else -> mainPath
            },
            seen = current ?: seen?.takeIf { it in known },
        )
    }

    /** A manual pick holds until a different non-main checkout is observed. */
    fun pick(root: String): WorktreeFollowState = copy(shown = root)
}
