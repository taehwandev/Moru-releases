package app.thdev.moru.core.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruColors
import app.thdev.moru.core.designsystem.theme.MoruIdentityAccent
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** A quoted alert, conversation turn, or numbered step, retaining the reader's Markdown actions. */
@Composable
internal fun MarkdownCallout(
    block: MarkdownBlock.Callout,
    onOpenLink: (String) -> Unit,
    imageBasePath: String?,
    imageLoader: MarkdownImageLoader?,
) {
    val colors = MoruTheme.colors
    val accent = block.kind.accent(colors)
    val user = block.kind == MarkdownCalloutKind.User
    val bot = block.kind == MarkdownCalloutKind.Bot
    val shape = RoundedCornerShape(
        topStart = MoruTheme.radius.large,
        topEnd = MoruTheme.radius.large,
        bottomStart = if (bot) MoruTheme.radius.xSmall else MoruTheme.radius.large,
        bottomEnd = if (user) MoruTheme.radius.xSmall else MoruTheme.radius.large,
    )
    val leadParagraph = if (block.kind == MarkdownCalloutKind.Step) {
        block.blocks.firstOrNull()?.takeIf { it is MarkdownBlock.Paragraph }
    } else null
    // A bounded inset leaves the conversation direction visible even in a narrow Result pane.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (user) MoruTheme.spacing.xLarge else MoruTheme.spacing.none,
                end = if (bot) MoruTheme.spacing.xLarge else MoruTheme.spacing.none,
            )
            .testTag("markdown-callout-${block.kind.marker.lowercase()}")
            .clip(shape)
            .background(accent.copy(alpha = if (colors.isLight) 0.05f else 0.08f).compositeOver(colors.surface))
            .border(MoruTheme.stroke.thin, accent.copy(alpha = if (colors.isLight) 0.25f else 0.3f), shape)
            .padding(MoruTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small),
    ) {
        DisableSelection {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (user) Arrangement.spacedBy(MoruTheme.spacing.small, Alignment.End)
                else Arrangement.spacedBy(MoruTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!user) CalloutSymbol(block, accent)
                MoruText(
                    text = block.kind.label,
                    style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = accent,
                    modifier = Modifier.semantics { heading() },
                )
                if (user) CalloutSymbol(block, accent)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small)) {
            MarkdownBlocks(block.blocks, onOpenLink, imageBasePath, imageLoader, frame = { child, content ->
                if (child === leadParagraph && child is MarkdownBlock.Paragraph) {
                    MarkdownInlineText(
                        text = child.text,
                        onOpenLink = onOpenLink,
                        modifier = Modifier.fillMaxWidth(),
                        style = MoruTheme.typography.title,
                        imageBasePath = imageBasePath,
                        imageLoader = imageLoader,
                    )
                } else content()
            })
        }
    }
}

@Composable
private fun CalloutSymbol(block: MarkdownBlock.Callout, accent: Color) {
    val numbered = block.kind == MarkdownCalloutKind.Step && block.stepNumber != null
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = MoruTheme.spacing.xLarge, minHeight = MoruTheme.spacing.xLarge)
            .background(
                if (numbered) accent else accent.copy(alpha = if (MoruTheme.colors.isLight) 0.1f else 0.14f),
                RoundedCornerShape(MoruTheme.radius.small),
            )
            .padding(MoruTheme.spacing.xSmall)
            .testTag(if (numbered) "markdown-callout-step-number" else "markdown-callout-symbol"),
        contentAlignment = Alignment.Center,
    ) {
        if (numbered) {
            MoruText(
                text = block.stepNumber.toString(),
                style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                color = MoruTheme.colors.onPrimary,
            )
        } else {
            MoruIcon(block.kind.icon, size = MoruTheme.spacing.large, tint = accent)
        }
    }
}

private val MarkdownCalloutKind.label: String
    get() = when (this) {
        MarkdownCalloutKind.Ai -> "AI insight"
        MarkdownCalloutKind.User -> "User question"
        MarkdownCalloutKind.Bot -> "AI answer"
        else -> name
    }

private val MarkdownCalloutKind.icon: MoruIconName
    get() = when (this) {
        MarkdownCalloutKind.Note -> MoruIconName.File
        MarkdownCalloutKind.Tip -> MoruIconName.Check
        MarkdownCalloutKind.Important -> MoruIconName.Flag
        MarkdownCalloutKind.Warning -> MoruIconName.Bell
        MarkdownCalloutKind.Caution -> MoruIconName.Close
        MarkdownCalloutKind.Ai -> MoruIconName.Sparkles
        MarkdownCalloutKind.User -> MoruIconName.Comment
        MarkdownCalloutKind.Bot -> MoruIconName.Bot
        MarkdownCalloutKind.Step -> MoruIconName.ChevronRight
    }

private fun MarkdownCalloutKind.accent(colors: MoruColors): Color = when (this) {
    MarkdownCalloutKind.Tip -> colors.success
    MarkdownCalloutKind.Warning -> colors.warning
    MarkdownCalloutKind.Caution -> colors.error
    MarkdownCalloutKind.Ai, MarkdownCalloutKind.Important -> MoruIdentityAccent.Second.color(colors)
    MarkdownCalloutKind.Bot -> colors.textSecondary
    else -> colors.primary
}
