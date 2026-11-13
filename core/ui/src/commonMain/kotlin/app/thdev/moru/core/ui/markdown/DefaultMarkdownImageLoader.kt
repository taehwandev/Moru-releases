package app.thdev.moru.core.ui.markdown

/** Platform binding for readers that do not supply an image loader. */
internal expect fun defaultMarkdownImageLoader(): MarkdownImageLoader
