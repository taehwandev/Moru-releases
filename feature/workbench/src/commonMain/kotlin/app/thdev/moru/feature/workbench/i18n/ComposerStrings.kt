package app.thdev.moru.feature.workbench.i18n

/**
 * The new Task composer's text in one language. Agent names ("Claude", "Codex") are not translated and
 * are passed in. The check and arrow marks are drawn by the screen, not written in the table.
 */
internal interface ComposerStrings {
    val headingTitle: String
    /** A Lab session of [agent] in the New Session menu. */
    fun labSession(agent: String): String
    val labBadge: String
    val headingBody: String
    val preparingTask: String
    val cancel: String
}
