package app.thdev.moru.core.ui.markdown

/** Desktop binding keeps platform IO out of the shared Markdown contract. */
internal actual fun defaultMarkdownImageLoader(): MarkdownImageLoader = DesktopMarkdownImageLoader
