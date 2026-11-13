package app.thdev.moru.core.ui.path

import androidx.compose.runtime.staticCompositionLocalOf

/** The user's home folder, provided once by the app; empty when unknown, so paths are shown as they are. */
val LocalHomeDirectory = staticCompositionLocalOf { "" }
