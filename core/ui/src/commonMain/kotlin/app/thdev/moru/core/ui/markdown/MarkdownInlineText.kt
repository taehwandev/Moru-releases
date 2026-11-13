package app.thdev.moru.core.ui.markdown

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import app.thdev.moru.core.designsystem.component.MoruAnnotatedText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.MoruSyntaxColors
import app.thdev.moru.core.ui.search.TextSearchChild
import app.thdev.moru.core.ui.search.rememberSearchText
import kotlin.math.roundToInt

@Composable
internal fun MarkdownInlineText(
    text: String,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = MoruTheme.typography.body,
    imageBasePath: String? = null,
    imageLoader: MarkdownImageLoader? = null,
) {
    val runs = remember(text) { markdownRuns(text) }
    if (runs.none { it.imageSource != null }) {
        TextSearchChild("inline:0") { MarkdownRunText(runs, onOpenLink, modifier, style) }
    } else {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small)) {
            var start = 0
            runs.forEachIndexed { index, run ->
                val source = run.imageSource
                if (source != null) {
                    if (index > start) TextSearchChild("inline:$start") { MarkdownRunText(runs.subList(start, index), onOpenLink, Modifier.fillMaxWidth(), style) }
                    MarkdownResultImage(
                        source = source,
                        description = run.text,
                        onOpenImage = onOpenLink,
                        imageBasePath = imageBasePath,
                        imageLoader = imageLoader,
                    )
                    start = index + 1
                }
            }
            if (start < runs.size) TextSearchChild("inline:$start") { MarkdownRunText(runs.subList(start, runs.size), onOpenLink, Modifier.fillMaxWidth(), style) }
        }
    }
}

@Composable
private fun MarkdownRunText(runs: List<MarkdownRun>, onOpenLink: (String) -> Unit, modifier: Modifier, style: TextStyle) {
    val colors = MoruTheme.colors
    val hints = LocalMarkdownLinkHints.current
    val openLink by rememberUpdatedState(onOpenLink)
    // Built once per text and theme; a new listener each redraw would make the text unequal and lay it out again.
    val annotated = remember(runs, colors, hints) { buildAnnotatedString {
        var previousTarget: String? = null
        runs.forEach { run ->
            val span = SpanStyle(
                fontWeight = if (run.bold) FontWeight.Bold else null,
                fontStyle = if (run.italic) FontStyle.Italic else null,
                fontFamily = if (run.code) FontFamily.Monospace else null,
                color = if (run.code && run.target == null) MoruSyntaxColors.of(colors).type else Color.Unspecified,
                background = if (run.code) colors.surface else Color.Unspecified,
            )
            if (run.target != null) {
                withLink(LinkAnnotation.Clickable(
                    tag = run.target,
                    styles = TextLinkStyles(SpanStyle(color = colors.primary, textDecoration = TextDecoration.Underline)),
                    linkInteractionListener = { openLink(run.target) },
                )) {
                    // One icon per link, even when its label is split into differently styled runs.
                    if (hints != null && run.target != previousTarget) markdownLinkKind(run.target, hints.isLocalPage)?.let { appendLinkIcon(it) }
                    withStyle(span) { append(run.text) }
                }
            } else withStyle(span) { append(run.text) }
            previousTarget = run.target
        }
    } }
    // Link icons insert a non-text glyph. Search offsets stay in the visible label's characters.
    val offsets = remember(runs, hints) {
        buildList {
            var offset = 0
            var previousTarget: String? = null
            runs.forEach { run ->
                if (hints != null && run.target != null && run.target != previousTarget && markdownLinkKind(run.target, hints.isLocalPage) != null) offset++
                run.text.forEach { add(offset++) }
                previousTarget = run.target
            }
        }
    }
    val search = rememberSearchText(annotated, runs.joinToString("") { it.text }, offsets)
    val layoutResult = remember { arrayOfNulls<TextLayoutResult>(1) }
    // Text invokes onTextLayout while measuring. A plain holder lets the enclosing layout publish
    // the resulting line geometry in the same pass, without a state write or a second composition.
    val firstLineCenter = remember { intArrayOf(AlignmentLine.Unspecified) }
    MoruAnnotatedText(
        text = search.text,
        style = style,
        color = colors.textPrimary,
        modifier = modifier.then(search.modifier).then(if (hints == null) Modifier else Modifier.markdownLinkHover(annotated, { layoutResult[0] }, hints.onHover))
            .layout { measurable, constraints ->
                val text = measurable.measure(constraints)
                val center = firstLineCenter[0]
                val lines: Map<AlignmentLine, Int> = if (center == AlignmentLine.Unspecified) emptyMap() else mapOf(MarkdownFirstLineCenter to center)
                layout(text.width, text.height, alignmentLines = lines) { text.placeRelative(0, 0) }
            },
        inlineContent = if (hints == null) emptyMap() else markdownLinkIcons,
        onTextLayout = { result ->
            search.onTextLayout(result)
            layoutResult[0] = result
            firstLineCenter[0] = if (result.layoutInput.text.isNotEmpty() && result.lineCount > 0) {
                ((result.getLineTop(0) + result.getLineBottom(0)) / 2f).roundToInt()
            } else AlignmentLine.Unspecified
        },
    )
}
