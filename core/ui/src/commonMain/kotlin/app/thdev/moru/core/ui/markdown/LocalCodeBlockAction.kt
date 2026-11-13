package app.thdev.moru.core.ui.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** A reader's optional code control; null keeps the shared code-copy button. */
internal val LocalCodeBlockAction = staticCompositionLocalOf<(@Composable (code: String) -> Unit)?> { null }
