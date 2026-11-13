package app.thdev.moru.core.designsystem.component

import androidx.compose.runtime.staticCompositionLocalOf
import java.awt.Window

/** Native app dialogs and popovers share this owner; windowless UI fixtures have none. */
val LocalMoruPopupOwner = staticCompositionLocalOf<Window?> { null }
