package app.thdev.moru.core.ui.search

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import app.thdev.moru.core.designsystem.theme.MoruTheme

@Composable
fun rememberSearchText(text: AnnotatedString, searchableText: String = text.text, offsets: List<Int>? = null): SearchTextPresentation {
    val scope = LocalTextSearchScope.current
    if (scope == null || scope.query.isEmpty()) return SearchTextPresentation(text, Modifier) {}
    val colors = MoruTheme.colors
    val matches = remember(searchableText, scope?.path, scope?.query) {
        TextSearchLeaf(scope.path, searchableText).matches(scope.query)
    }
    val active = scope?.active?.takeIf { it.path == scope.path && it in matches }
    fun start(match: TextSearchMatch) = offsets?.getOrNull(match.start) ?: match.start
    fun end(match: TextSearchMatch) = offsets?.getOrNull(match.end - 1)?.plus(1) ?: match.end
    val highlighted = remember(text, matches, active, colors, offsets) {
        if (matches.isEmpty()) text else AnnotatedString.Builder(text).apply {
            matches.forEach { match ->
                addStyle(
                    if (match == active) SpanStyle(background = colors.primary, color = colors.onPrimary)
                    else SpanStyle(background = colors.warning.copy(alpha = if (colors.isLight) 0.30f else 0.38f)),
                    start(match), end(match),
                )
            }
        }.toAnnotatedString()
    }
    val requester = remember { BringIntoViewRequester() }
    var rect by remember(active) { mutableStateOf<Rect?>(null) }
    LaunchedEffect(active, rect, scope?.revision) {
        val bounds = rect
        if (active != null && bounds != null && (scope?.revision ?: 0) >= 0) {
            // Lazy parents first compose/position the target item; then reveal the exact line, including nested scrollers.
            withFrameNanos { }
            requester.bringIntoView(bounds)
        }
    }
    return SearchTextPresentation(
        text = highlighted,
        modifier = if (active == null) Modifier else Modifier.bringIntoViewRequester(requester).testTag("text-search-active"),
        onTextLayout = { layout ->
            if (active != null && start(active) < layout.layoutInput.text.length) {
                rect = layout.getBoundingBox(start(active))
            }
        },
    )
}
