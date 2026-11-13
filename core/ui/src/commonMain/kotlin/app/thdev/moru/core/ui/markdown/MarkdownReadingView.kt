package app.thdev.moru.core.ui.markdown

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Dp
import app.thdev.moru.core.designsystem.theme.MoruTheme

/**
 * Shared Knowledge/Code reader. The caller owns vertical scrolling and local link resolution. [padding] surrounds the
 * text; null keeps the reader's own margin. [codeBlockAction] replaces the default code-copy button in each block's
 * corner. [imageBasePath] resolves relative image paths, usually to the Task's folder. [blockFrame], when given, draws
 * around each top-level block, such as a control to remark on that block; it gets the block and draws its content.
 * [streaming] is for text that grows at its end: only what follows its last finished block is read again.
 */
@Composable
fun MarkdownReadingView(
    content: String,
    onOpenWikiLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    padding: Dp? = null,
    codeBlockAction: (@Composable (code: String) -> Unit)? = null,
    imageBasePath: String? = null,
    imageLoader: MarkdownImageLoader? = null,
    blockFrame: (@Composable (block: MarkdownBlock, content: @Composable () -> Unit) -> Unit)? = null,
    streaming: Boolean = false,
) {
    val stream = remember { MarkdownStreamParser() }
    val blocks = remember(content, streaming) { if (streaming) stream.parse(content) else parseMarkdownBlocks(content) }
    val uriHandler = LocalUriHandler.current
    val openWikiLink by rememberUpdatedState(onOpenWikiLink)
    // The same handler on every redraw, so blocks that did not change are skipped while text streams in.
    val openLink: (String) -> Unit = remember(uriHandler) {
        { target ->
            when (markdownLinkKind(target)) {
                MarkdownLinkKind.Web, MarkdownLinkKind.LocalPage ->
                    try { uriHandler.openUri(target) } catch (_: Exception) { /* Unavailable browsers leave the document readable. */ }
                MarkdownLinkKind.File -> openWikiLink(target)
                // Heading navigation is not a file-open action, and other schemes cannot launch processes or apps.
                null -> Unit
            }
        }
    }
    SelectionContainer(modifier = modifier) {
        Column(modifier = Modifier.padding(padding ?: MoruTheme.spacing.large), verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small)) {
            CompositionLocalProvider(LocalCodeBlockAction provides codeBlockAction) {
                MarkdownBlocks(blocks, openLink, imageBasePath, imageLoader, blockFrame)
            }
        }
    }
}
