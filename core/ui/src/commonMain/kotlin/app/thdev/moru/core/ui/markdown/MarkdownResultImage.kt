package app.thdev.moru.core.ui.markdown

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import kotlinx.coroutines.CancellationException

/** Inline Markdown and standalone Result images share loading, sizing and click-to-open behavior. */
@Composable
fun MarkdownResultImage(
    source: String,
    description: String,
    onOpenImage: (String) -> Unit,
    modifier: Modifier = Modifier,
    imageBasePath: String? = null,
    imageLoader: MarkdownImageLoader? = null,
) {
    val loader = imageLoader ?: remember { defaultMarkdownImageLoader() }
    val result by produceState<MarkdownImageLoadResult?>(null, source, imageBasePath, loader) {
        value = null
        value = try {
            loader.load(source, imageBasePath)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            MarkdownImageLoadResult.Unavailable
        }
    }
    DisableSelection {
        Column(
            modifier.fillMaxWidth().ckSurface(MoruSurfaceRole.Code).padding(MoruTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall),
        ) {
            when (val image = result) {
                is MarkdownImageLoadResult.Available -> Image(
                    bitmap = image.bitmap,
                    contentDescription = description.ifBlank { "Result image" },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).testTag("markdown-image")
                        .clickable(role = Role.Button, onClickLabel = "Open image") { onOpenImage(image.openTarget) },
                )
                MarkdownImageLoadResult.Unavailable -> MoruText(
                    "Image unavailable", modifier = Modifier.testTag("markdown-image-unavailable"),
                    style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary,
                )
                null -> MoruText(
                    "Loading image…", modifier = Modifier.testTag("markdown-image-loading"),
                    style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary,
                )
            }
            if (description.isNotBlank()) MoruText(description, style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary)
        }
    }
}
