package app.thdev.moru.app

import androidx.compose.ui.input.key.Key

/**
 * One menu item: its label, the key that triggers it (with the platform modifier, plus Shift or Option if asked; null
 * for an item without a shortcut) and its action.
 */
internal data class AppMenuEntry(
    val label: String,
    val key: Key?,
    val shift: Boolean,
    val alt: Boolean,
    val enabled: Boolean,
    val onClick: () -> Unit,
)
