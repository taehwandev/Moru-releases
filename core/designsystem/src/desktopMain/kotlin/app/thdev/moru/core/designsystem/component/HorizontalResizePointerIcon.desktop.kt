package app.thdev.moru.core.designsystem.component

import androidx.compose.ui.input.pointer.PointerIcon
import java.awt.Cursor

internal actual fun horizontalResizePointerIcon(): PointerIcon =
    PointerIcon(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR))
