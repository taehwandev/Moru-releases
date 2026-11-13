package app.thdev.moru.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf

/** True only after this window's native backdrop is installed; fixtures keep a readable theme surface. */
val LocalMoruNativeMaterial = staticCompositionLocalOf { false }
