package app.thdev.moru.core.designsystem.component

import java.awt.Window
import javax.swing.JPanel
import javax.swing.SwingUtilities

/** Zero-size attachment probe binds each popover to its own AWT window, including concurrent popovers. */
internal class MoruPopupWindowProbe(private val onWindow: (Window?) -> Unit) : JPanel() {
    init {
        isOpaque = false
        isFocusable = false
    }

    override fun addNotify() {
        super.addNotify()
        SwingUtilities.invokeLater {
            if (isDisplayable) onWindow(SwingUtilities.getWindowAncestor(this))
        }
    }

    override fun removeNotify() {
        onWindow(null)
        super.removeNotify()
    }
}
