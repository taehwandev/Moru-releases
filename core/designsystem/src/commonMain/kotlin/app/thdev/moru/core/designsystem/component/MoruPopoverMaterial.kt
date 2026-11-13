package app.thdev.moru.core.designsystem.component

import androidx.compose.runtime.Composable

/** Owns the material of the actual popup window independently of its parent window. */
@Composable
internal expect fun MoruPopoverMaterial(content: @Composable () -> Unit)
