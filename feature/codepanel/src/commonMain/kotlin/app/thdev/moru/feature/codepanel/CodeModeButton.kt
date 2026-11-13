package app.thdev.moru.feature.codepanel

import androidx.compose.runtime.Composable
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonSize
import app.thdev.moru.core.designsystem.component.MoruButtonType

/** One of the Code / Diff switches over a changed file; the chosen one is filled. */
@Composable
internal fun CodeModeButton(text: String, selected: Boolean, onClick: () -> Unit) {
    MoruButton(
        text = text,
        onClick = onClick,
        type = if (selected) MoruButtonType.SECONDARY else MoruButtonType.GHOST,
        size = MoruButtonSize.Small,
    )
}
