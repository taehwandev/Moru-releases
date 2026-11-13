package app.thdev.moru.core.designsystem.component

import androidx.compose.ui.input.pointer.PointerIcon
import java.awt.Cursor

internal actual fun verticalResizePointerIcon(): PointerIcon =
    PointerIcon(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR))
