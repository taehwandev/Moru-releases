package app.thdev.moru.feature.workbench

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruSurfaceColumn
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.MoruTextField
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.files.WorkbenchFileRenameOutcome
import app.thdev.moru.feature.workbench.i18n.codeStrings

@Composable
internal fun OpenFileTabStrip(
    file: WorkbenchFilePreview,
    isDirty: Boolean = false,
    onClose: () -> Unit,
    onRename: (String) -> WorkbenchFileRenameOutcome = { name ->
        WorkbenchFileRenameOutcome.Failed(name, "Renaming is not wired.")
    },
    onDelete: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val strings = codeStrings()
    var renaming by remember(file.path) { mutableStateOf<String?>(null) }
    var renameError by remember(file.path) { mutableStateOf<String?>(null) }
    var confirmDelete by remember(file.path) { mutableStateOf(false) }

    MoruSurfaceColumn(
        role = MoruSurfaceRole.Panel,
        modifier = Modifier.fillMaxWidth(),
        padding = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .padding(horizontal = 10.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FileBreadcrumb(
                file = file,
                isDirty = isDirty,
                modifier = Modifier.weight(1f),
            )
            actions()
            MoruIconButton(
                description = strings.rename,
                size = 28.dp,
                onClick = {
                    renaming = if (renaming == null) file.path else null
                    renameError = null
                },
            ) {
                MoruIcon(name = MoruIconName.Pencil, size = 14.dp, tint = MoruTheme.colors.textSecondary)
            }
            if (confirmDelete) {
                Box(
                    modifier = Modifier
                        .ckSurface(MoruSurfaceRole.Warning)
                        .clickable {
                            confirmDelete = false
                            onDelete()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    MoruText(
                        text = strings.confirmDelete,
                        color = MoruTheme.colors.warning,
                        style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                    )
                }
                MoruButton(text = strings.cancel, onClick = { confirmDelete = false }, type = MoruButtonType.GHOST)
            } else {
                MoruIconButton(
                    description = strings.delete,
                    onClick = { confirmDelete = true },
                    size = 28.dp,
                ) {
                    MoruIcon(name = MoruIconName.Trash, size = 14.dp, tint = MoruTheme.colors.textSecondary)
                }
            }
            MoruIconButton(
                onClick = onClose,
                description = strings.close,
                size = 28.dp,
            ) {
                MoruIcon(name = MoruIconName.Close, size = 12.dp, tint = MoruTheme.colors.textSecondary)
            }
        }

        renaming?.let { name ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MoruTextField(
                    value = name,
                    onValueChange = {
                        renaming = it
                        renameError = null
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = strings.newPathPlaceholder,
                    singleLine = true,
                )
                MoruButton(
                    text = strings.save,
                    onClick = {
                        when (val outcome = onRename(name)) {
                            is WorkbenchFileRenameOutcome.Renamed -> {
                                renaming = null
                                renameError = null
                            }
                            is WorkbenchFileRenameOutcome.Failed -> renameError = outcome.message
                        }
                    },
                    type = MoruButtonType.SECONDARY,
                    enabled = name.isNotBlank(),
                )
            }
            renameError?.let { message ->
                MoruText(
                    text = message,
                    color = MoruTheme.colors.warning,
                    style = MoruTheme.typography.caption,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    maxLines = 1,
                )
            }
        }
    }
}
