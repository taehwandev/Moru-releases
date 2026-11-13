package app.thdev.moru.app

import androidx.compose.ui.awt.ComposeWindow
import java.awt.AWTEvent
import java.awt.AlphaComposite
import java.awt.Graphics2D
import java.awt.Toolkit
import java.awt.Window
import java.awt.Rectangle
import java.awt.event.ComponentEvent
import java.awt.event.AWTEventListener
import java.awt.event.WindowEvent
import javax.swing.JDialog
import javax.swing.RootPaneContainer
import javax.swing.SwingUtilities

/** Compose 1.10 window layers are always-on-top; normal ownership keeps menus above Moru only. */
internal object MoruPopupWindowPolicy {
    fun install(): AutoCloseable {
        val toolkit = Toolkit.getDefaultToolkit()
        val listener = AWTEventListener { event ->
            if (event is WindowEvent && event.id == WindowEvent.WINDOW_OPENED) {
                val popup = event.window
                if (popup.isAlwaysOnTop && popup.owner?.hasComposeOwner() == true) {
                    popup.isAlwaysOnTop = false
                    // macOS may drop focus when changing the native window level.
                    popup.toFront()
                    if (popup.isFocusableWindow && popup.isAutoRequestFocus) popup.requestFocus()
                }
                clearBacking(popup)
            }
            if (event is ComponentEvent && event.component is Window &&
                (event.id == ComponentEvent.COMPONENT_RESIZED || event.id == ComponentEvent.COMPONENT_MOVED)) {
                boundPopup(event.component as Window)
                if (event.id == ComponentEvent.COMPONENT_RESIZED) clearBacking(event.component as Window)
            }
        }
        toolkit.addAWTEventListener(listener, AWTEvent.WINDOW_EVENT_MASK or AWTEvent.COMPONENT_EVENT_MASK)
        return AutoCloseable { toolkit.removeAWTEventListener(listener) }
    }

    /** Recorded drawing bounds can exceed a clipped scroll viewport. Keep the native popup within
     * its owner while retaining a valid opening origin; repeated events are idempotent. */
    private fun boundPopup(popup: Window) {
        if (!popup.isShowing || !popup.isTransparentComposePopup()) return
        val content = (popup.owner as? RootPaneContainer)?.contentPane ?: return
        if (!content.isShowing || content.width <= 0 || content.height <= 0) return
        val owner = Rectangle(content.locationOnScreen, content.size)
        val x = popup.x.coerceIn(owner.x, owner.x + owner.width - 1)
        val y = popup.y.coerceIn(owner.y, owner.y + owner.height - 1)
        val bounds = Rectangle(x, y, popup.width.coerceIn(1, owner.x + owner.width - x),
            popup.height.coerceIn(1, owner.y + owner.height - y))
        if (popup.bounds != bounds) popup.bounds = bounds
    }

    /** Java2D's OpenGL pipeline sometimes keeps a resized window's fresh surface uncleared, so a transparent
     * popup shows opaque white around its rounded sheet. Compose draws in its own Metal layer above that
     * surface, so clearing it again after the resize removes only the white. */
    private fun clearBacking(popup: Window) {
        if (!popup.isTransparentComposePopup()) return
        SwingUtilities.invokeLater {
            if (!popup.isShowing) return@invokeLater
            val graphics = popup.graphics as? Graphics2D ?: return@invokeLater
            try {
                graphics.composite = AlphaComposite.Clear
                graphics.fillRect(0, 0, popup.width, popup.height)
            } finally {
                graphics.dispose()
            }
            popup.toolkit.sync()
        }
    }

    private fun Window.isTransparentComposePopup(): Boolean =
        this is JDialog && isUndecorated && background?.alpha == 0 && owner?.hasComposeOwner() == true

    private fun Window.hasComposeOwner(): Boolean =
        this is ComposeWindow || owner?.hasComposeOwner() == true
}
