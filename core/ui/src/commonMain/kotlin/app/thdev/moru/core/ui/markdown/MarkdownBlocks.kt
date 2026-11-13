package app.thdev.moru.core.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import app.thdev.moru.core.designsystem.component.MoruDivider
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.ui.search.TextSearchChild

@Composable
internal fun MarkdownBlocks(
    blocks: List<MarkdownBlock>,
    onOpenLink: (String) -> Unit,
    imageBasePath: String?,
    imageLoader: MarkdownImageLoader?,
    frame: (@Composable (block: MarkdownBlock, content: @Composable () -> Unit) -> Unit)? = null,
) {
    blocks.forEachIndexed { index, block ->
        TextSearchChild("block:$index") {
        if (frame != null) frame(block) { MarkdownBlockView(block, onOpenLink, imageBasePath, imageLoader) }
        else MarkdownBlockView(block, onOpenLink, imageBasePath, imageLoader)
        }
    }
}

@Composable
private fun MarkdownBlockView(block: MarkdownBlock, onOpenLink: (String) -> Unit, imageBasePath: String?, imageLoader: MarkdownImageLoader?) {
    when (block) {
        is MarkdownBlock.Heading -> {
            Spacer(Modifier.height(if (block.level <= 2) MoruTheme.spacing.small else MoruTheme.spacing.xSmall))
            MarkdownInlineText(block.text, onOpenLink, Modifier.fillMaxWidth().semantics { heading() }, headingStyle(block.level), imageBasePath, imageLoader)
        }
        is MarkdownBlock.Paragraph -> MarkdownInlineText(block.text, onOpenLink, Modifier.fillMaxWidth(), imageBasePath = imageBasePath, imageLoader = imageLoader)
        is MarkdownBlock.ListItem -> MarkdownListItem(block, onOpenLink, imageBasePath, imageLoader)
        is MarkdownBlock.CodeBlock -> MarkdownCodeBlock(block)
        is MarkdownBlock.Table -> MarkdownTable(block, onOpenLink, imageBasePath, imageLoader)
        is MarkdownBlock.Callout -> MarkdownCallout(block, onOpenLink, imageBasePath, imageLoader)
        is MarkdownBlock.Quote -> Row(
            modifier = Modifier.fillMaxWidth().ckSurface(MoruSurfaceRole.Code).padding(MoruTheme.spacing.medium),
        ) {
            Spacer(Modifier.width(MoruTheme.stroke.thin).height(MoruTheme.spacing.xLarge).background(MoruTheme.colors.primary))
            Column(Modifier.weight(1f).padding(start = MoruTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small)) {
                MarkdownBlocks(block.blocks, onOpenLink, imageBasePath, imageLoader)
            }
        }
        MarkdownBlock.Rule -> MoruDivider(Modifier.padding(vertical = MoruTheme.spacing.small))
    }
}

@Composable
private fun headingStyle(level: Int): TextStyle = when (level) {
    1 -> MoruTheme.typography.h1
    2 -> MoruTheme.typography.h2
    3 -> MoruTheme.typography.title
    4 -> MoruTheme.typography.body.copy(fontWeight = FontWeight.Bold)
    5 -> MoruTheme.typography.body.copy(fontWeight = FontWeight.SemiBold)
    else -> MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold)
}
