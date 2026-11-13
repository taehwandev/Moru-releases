package app.thdev.moru.feature.workbench

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruSyntaxColors
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.domain.files.ProjectExplorerPolicy
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.ui.code.CodeLanguage

/**
 * The open file's place in its folder, read like an IDE's navigation bar: each folder with a folder mark, soft
 * chevrons between them, and the file's mark tinted by its language from the code colors. A deep path keeps its first
 * folder and the two nearest the file around an ellipsis.
 */
@Composable
internal fun FileBreadcrumb(
    file: WorkbenchFilePreview,
    isDirty: Boolean,
    modifier: Modifier = Modifier,
) {
    val shown = remember(file.path) { ProjectExplorerPolicy.breadcrumbFolders(file.path) }
    val dim = MoruTheme.colors.textSecondary
    val separator = dim.copy(alpha = 0.55f)
    val folderTint = MoruTheme.colors.primary.copy(alpha = 0.8f)
    BoxWithConstraints(modifier = modifier.semantics { contentDescription = file.path }) {
        val showFolders = shown.isNotEmpty() && maxWidth >= 240.dp
        val filenameMaxWidth = maxWidth * 0.6f
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showFolders) {
                Row(
                    modifier = Modifier.weight(1f, fill = false).clipToBounds(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    shown.forEach { folder ->
                        if (folder != "…") MoruIcon(name = MoruIconName.Folder, size = 13.dp, tint = folderTint)
                        MoruText(text = folder, color = dim, style = MoruTheme.typography.caption, maxLines = 1)
                        MoruIcon(name = MoruIconName.ChevronRight, size = 11.dp, tint = separator)
                    }
                }
            }
            MoruIcon(name = MoruIconName.File, size = 14.dp, tint = fileTint(file.path, MoruSyntaxColors.of(MoruTheme.colors), dim))
            MoruText(
                text = if (isDirty) "${file.name} ●" else file.name,
                modifier = if (showFolders) Modifier.widthIn(max = filenameMaxWidth) else Modifier.weight(1f),
                color = if (isDirty) MoruTheme.colors.primary else MoruTheme.colors.textPrimary,
                style = MoruTheme.typography.caption.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A file's mark color by language, from the code palette, so a Kotlin file reads purple as its keywords do. */
private fun fileTint(path: String, syntax: MoruSyntaxColors, plain: Color): Color = when (CodeLanguage.of(path)) {
    CodeLanguage.Kotlin -> syntax.keyword
    CodeLanguage.Java, CodeLanguage.Swift, CodeLanguage.Rust -> syntax.annotation
    CodeLanguage.Script -> syntax.function
    CodeLanguage.Python, CodeLanguage.Go -> syntax.type
    CodeLanguage.Shell -> syntax.string
    CodeLanguage.Data -> syntax.number
    CodeLanguage.Plain -> plain
}
