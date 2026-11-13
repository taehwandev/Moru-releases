package app.thdev.moru.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize

/** Bounds native content before placement, including callers that request a fixed oversized panel. */
@Composable
internal expect fun rememberMoruPopoverWindowSize(): DpSize?
