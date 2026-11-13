package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.thdev.moru.core.designsystem.theme.MoruTheme

/**
 * A one-line field at [MoruControlHeight] with a button's corner and outline, for forms where it sits beside buttons
 * and selects. [MoruTextField] stays the taller field that can float a label.
 */
@Composable
fun MoruCompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    textAlign: TextAlign = TextAlign.Start,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val focused by interactionSource.collectIsFocusedAsState()
    val textStyle = MoruTheme.typography.body.copy(fontSize = 13.sp, color = MoruTheme.colors.textPrimary, textAlign = textAlign)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.moruControlFrame(focused = focused, error = isError, enabled = enabled),
        enabled = enabled,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(MoruTheme.colors.primary),
        interactionSource = interactionSource,
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty() && placeholder != null) {
                    MoruText(placeholder, style = textStyle, color = MoruTheme.colors.textSecondary, maxLines = 1)
                }
                inner()
            }
        },
    )
}
