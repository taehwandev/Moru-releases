package app.thdev.moru.core.designsystem.component

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme

@Composable
fun MoruTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true
) {
    val inputSurface = MoruTheme.surfaceStyle(MoruSurfaceRole.Card)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label?.let { { MoruText(it) } },
        placeholder = placeholder?.let { { MoruText(it, color = MoruTheme.colors.textSecondary) } },
        singleLine = singleLine,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MoruTheme.colors.primary,
            unfocusedBorderColor = MoruTheme.colors.border,
            focusedContainerColor = inputSurface.background,
            unfocusedContainerColor = inputSurface.background,
            focusedLabelColor = MoruTheme.colors.primary,
            unfocusedLabelColor = MoruTheme.colors.textSecondary
        )
    )
}
