package app.thdev.moru.core.ui.markdown

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Link hints a host asks a reader for: each link starts with an icon for its [MarkdownLinkKind], and [onHover] hears
 * the link under the pointer, or null once the pointer leaves it. [isLocalPage] is the host's own test, so the icon
 * names where the host opens the page.
 */
class MarkdownLinkHints(val isLocalPage: (String) -> Boolean, val onHover: (target: String?) -> Unit)

/** Readers without a host that asks draw links as before. */
val LocalMarkdownLinkHints = staticCompositionLocalOf<MarkdownLinkHints?> { null }
