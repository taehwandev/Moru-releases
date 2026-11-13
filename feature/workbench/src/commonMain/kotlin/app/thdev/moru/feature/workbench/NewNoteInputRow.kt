package app.thdev.moru.feature.workbench

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.MoruTextField
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.i18n.codeStrings

@Composable
internal fun NewNoteInputRow(
    name: String,
    error: String?,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val strings = codeStrings()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MoruTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.weight(1f),
                placeholder = strings.newNotePlaceholder,
                singleLine = true,
            )
            MoruButton(
                text = strings.create,
                onClick = onConfirm,
                type = MoruButtonType.SECONDARY,
                enabled = name.isNotBlank(),
            )
        }
        error?.let { message ->
            MoruText(
                text = message,
                color = MoruTheme.colors.warning,
                style = MoruTheme.typography.caption,
                maxLines = 1,
            )
        }
    }
}
