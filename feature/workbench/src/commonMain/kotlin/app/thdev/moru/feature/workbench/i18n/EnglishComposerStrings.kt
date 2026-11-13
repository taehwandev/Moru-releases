package app.thdev.moru.feature.workbench.i18n

internal object EnglishComposerStrings : ComposerStrings {
    override fun labSession(agent: String) = "$agent Lab"
    override val labBadge = "Lab"
    override val headingTitle = "What do you want to start with?"
    override val headingBody = "Pick what to start. It opens in its own terminal, where you type your request."
    override val preparingTask = "Preparing task…"
    override val cancel = "Cancel"
}
