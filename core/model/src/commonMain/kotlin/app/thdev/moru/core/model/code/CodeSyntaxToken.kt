package app.thdev.moru.core.model.code

/** Half-open UTF-16 offsets into the supplied text. Unclassified text keeps its default presentation. */
data class CodeSyntaxToken(val start: Int, val end: Int, val kind: Kind) {
    enum class Kind { Keyword, String, Number, Comment, Annotation, Type, Function }
    val italic: Boolean get() = kind == Kind.Comment
}
