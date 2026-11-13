package app.thdev.moru.core.domain.markdown

/** Existing link classification without an OS launcher. */
object MarkdownLinkPolicy {
    /**
     * Classifies the reader's destination. Heading anchors and unsupported schemes remain inert.
     * The host alone decides whether a web address is a local page beside the Task.
     */
    fun kind(target: String, isLocalPage: (String) -> Boolean = { false }): String? = when {
        target.startsWith('#') -> null
        target.startsWith("https://", ignoreCase = true) || target.startsWith("http://", ignoreCase = true) ->
            if (isLocalPage(target)) "LocalPage" else "Web"
        target.startsWith("file:", ignoreCase = true) || fileLinePattern.matches(target) -> "File"
        !schemePattern.containsMatchIn(target) -> "File"
        else -> null
    }

    private val fileLinePattern = Regex("^[^:/]+\\.[^:/]+:\\d+(?::\\d+)?(?:#.*)?$")
    private val schemePattern = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")
}
