package app.thdev.moru.core.ui.markdown

/** KeyFlow extensions and standard Markdown alerts share the same quoted source syntax. */
enum class MarkdownCalloutKind {
    Note, Tip, Important, Warning, Caution, Ai, User, Bot, Step;

    val marker: String get() = name.uppercase()
}
