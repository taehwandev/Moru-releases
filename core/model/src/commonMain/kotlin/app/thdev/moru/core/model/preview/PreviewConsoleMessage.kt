package app.thdev.moru.core.model.preview

/** One message the previewed page wrote to its console, or an uncaught error it raised. */
data class PreviewConsoleMessage(
    val level: PreviewConsoleLevel,
    val text: String,
)
