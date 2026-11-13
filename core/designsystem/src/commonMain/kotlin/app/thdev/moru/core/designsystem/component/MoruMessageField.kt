package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import app.thdev.moru.core.designsystem.theme.MoruTheme

/**
 * A borderless field that grows with what is typed, for writing a message inside a composer that draws its own frame.
 * Keys such as Enter-to-send belong to the caller's modifier; [value]'s composition tells them whether an input method
 * is still composing, as Korean is until a syllable is committed. A field taller than its text, such as one as tall as
 * the round send button beside it, keeps a single line centred.
 */
@Composable
fun MoruMessageField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = false,
) {
    val textStyle = MoruTheme.typography.body.copy(color = MoruTheme.colors.textPrimary)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = singleLine,
        textStyle = textStyle,
        cursorBrush = SolidColor(MoruTheme.colors.primary),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                if (value.text.isEmpty() && placeholder != null) {
                    MoruText(placeholder, style = textStyle, color = MoruTheme.colors.textSecondary)
                }
                inner()
            }
        },
    )
}
