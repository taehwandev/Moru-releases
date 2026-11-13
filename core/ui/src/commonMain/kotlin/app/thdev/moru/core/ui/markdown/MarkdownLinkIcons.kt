package app.thdev.moru.core.ui.markdown

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** Places the icon for [kind] in front of a link's label. */
internal fun AnnotatedString.Builder.appendLinkIcon(kind: MarkdownLinkKind) =
    // A word joiner stands in for the icon in copied text, so a copied link reads as its label alone.
    appendInlineContent(kind.name, "⁠")

/** The icon each [MarkdownLinkKind] starts with, sized to the line, in the link's color. */
internal val markdownLinkIcons: Map<String, InlineTextContent> = MarkdownLinkKind.entries.associate { kind ->
    kind.name to InlineTextContent(Placeholder(1.2.em, 1.em, PlaceholderVerticalAlign.TextCenter)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
            MoruIcon(kind.icon(), size = 12.dp, tint = MoruTheme.colors.primary)
        }
    }
}

private fun MarkdownLinkKind.icon(): MoruIconName = when (this) {
    MarkdownLinkKind.File -> MoruIconName.File
    MarkdownLinkKind.Web -> MoruIconName.Globe
    // A local page opens in the Task's Preview, in the sidebar on the right.
    MarkdownLinkKind.LocalPage -> MoruIconName.PanelRight
}
