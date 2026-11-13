package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import app.thdev.moru.core.designsystem.theme.MoruTheme

/**
 * An unframed, multi-line monospace editor for source text. The caller owns the frame, the gutter and
 * scrolling; [visualTransformation] carries syntax colouring.
 */
@Composable
fun MoruCodeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = MoruTheme.typography.monospace.copy(color = MoruTheme.colors.textPrimary),
        cursorBrush = SolidColor(MoruTheme.colors.primary),
        visualTransformation = visualTransformation,
    )
}
