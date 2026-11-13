package app.thdev.moru.feature.workbench

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruCodeTextField
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruSurfaceColumn
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruSyntaxColors
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.files.WorkbenchFileRenameOutcome
import app.thdev.moru.core.ui.code.CodeColoring
import app.thdev.moru.core.ui.code.CodeFileView
import app.thdev.moru.core.ui.code.CodeLineGutter
import app.thdev.moru.core.ui.code.codeLineCount
import app.thdev.moru.core.ui.code.CodeLanguage
import app.thdev.moru.core.ui.markdown.MarkdownReadingView
import app.thdev.moru.core.ui.markdown.MarkdownLiveEditor
import app.thdev.moru.feature.workbench.i18n.CodeStrings
import app.thdev.moru.feature.workbench.i18n.codeStrings

@Composable
internal fun ProjectFilePreviewPane(
    state: WorkbenchEditorState,
    onDraftChange: (String) -> Unit,
    onSave: () -> Unit,
    saveStatus: String?,
    onClose: () -> Unit,
    onRename: (String) -> WorkbenchFileRenameOutcome,
    onDelete: () -> Unit,
    onOpenWikiLink: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val file = state.preview
    val strings = codeStrings()
    val isMarkdown = isMarkdownPath(file.path) && file.content != null
    var readingView by remember(file.path) { mutableStateOf(isMarkdown) }
    val readingViewActive = isMarkdown && readingView
    val language = CodeLanguage.of(file.path)
    val syntax = MoruSyntaxColors.of(MoruTheme.colors)
    val codeColoring = remember(language, syntax) { CodeColoring(language, syntax) }

    MoruSurfaceColumn(
        role = MoruSurfaceRole.Panel,
        modifier = modifier,
        padding = 0.dp,
    ) {
        OpenFileTabStrip(
            file = file,
            isDirty = state.isDirty,
            onClose = onClose,
            onRename = onRename,
            onDelete = onDelete,
        ) {
            saveStatus?.let { message ->
                MoruText(
                    text = message,
                    modifier = Modifier.widthIn(max = 160.dp),
                    color = MoruTheme.colors.textSecondary,
                    style = MoruTheme.typography.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isMarkdown) {
                MoruButton(
                    text = if (readingView) strings.edit else strings.read,
                    onClick = { readingView = !readingView },
                    type = MoruButtonType.GHOST,
                )
            }
            when {
                state.canEdit && !readingViewActive -> MoruButton(
                    text = strings.save,
                    onClick = onSave,
                    type = MoruButtonType.SECONDARY,
                    enabled = state.isDirty,
                )
                !isMarkdown -> MoruText(
                    text = "${file.sizeLabel(strings)} / ${strings.previewKind(file.status)}",
                    color = MoruTheme.colors.textSecondary,
                    style = MoruTheme.typography.caption.copy(fontFamily = FontFamily.Monospace),
                    maxLines = 1,
                )
            }
        }

        val content = file.content
        when {
            readingViewActive -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(min = 120.dp)
                        .verticalScroll(rememberScrollState())
                        .ckSurface(MoruSurfaceRole.Code),
                ) {
                    MarkdownReadingView(
                        content = if (state.canEdit) state.draft else content.orEmpty(),
                        onOpenWikiLink = onOpenWikiLink,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            state.canEdit && isMarkdown -> MarkdownLiveEditor(
                content = state.draft,
                onContentChange = onDraftChange,
                onSave = onSave,
                documentKey = file.path,
                modifier = Modifier.fillMaxWidth().weight(1f).ckSurface(MoruSurfaceRole.Code),
            )
            state.canEdit -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(min = 120.dp)
                        .verticalScroll(rememberScrollState())
                        .ckSurface(MoruSurfaceRole.Code)
                        .padding(10.dp),
                ) {
                    CodeLineGutter(state.draft.lineSpan())
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                        MoruCodeTextField(
                            value = state.draft,
                            onValueChange = onDraftChange,
                            modifier = Modifier.onPreviewKeyEvent { event ->
                                val isSaveShortcut = event.type == KeyEventType.KeyDown &&
                                    event.key == Key.S &&
                                    (event.isMetaPressed || event.isCtrlPressed)
                                if (isSaveShortcut) {
                                    onSave()
                                    true
                                } else {
                                    false
                                }
                            },
                            visualTransformation = codeColoring,
                        )
                    }
                }
            }
            content == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .ckSurface(MoruSurfaceRole.Code),
                    contentAlignment = Alignment.Center,
                ) {
                    MoruText(
                        text = strings.previewMessage(file.status),
                        color = MoruTheme.colors.textSecondary,
                        style = MoruTheme.typography.caption,
                    )
                }
            }
            else -> CodeFileView(
                content = content,
                path = file.path,
                modifier = Modifier.weight(1f).heightIn(min = 120.dp),
            )
        }

        val truncationText = file.truncationText
        if (file.truncated && truncationText != null) {
            MoruSurfaceColumn(
                role = MoruSurfaceRole.Warning,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                padding = 8.dp,
            ) {
                MoruText(
                    text = truncationText,
                    color = MoruTheme.colors.warning,
                    style = MoruTheme.typography.caption,
                )
            }
        }
    }
}

/** Number of visual source lines, counting a trailing newline's empty line. */
internal fun String.lineSpan(): Int = codeLineCount()

private fun WorkbenchFilePreview.sizeLabel(strings: CodeStrings): String =
    sizeBytes?.toByteSizeLabel() ?: strings.unknownSize

private fun Long.toByteSizeLabel(): String {
    val kib = 1024L
    val mib = kib * 1024
    return when {
        this >= mib -> "${this / mib} MiB"
        this >= kib -> "${this / kib} KiB"
        else -> "$this B"
    }
}
