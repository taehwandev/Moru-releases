package app.thdev.moru.core.designsystem.material

import java.awt.Component
import java.awt.Container
import java.awt.Point
import java.awt.Window
import javax.swing.SwingUtilities
import org.jetbrains.skiko.SkiaLayer

/** Read-only native input diagnostics; never requests or changes macOS automation permission. */
class MoruNativePointerProbe private constructor(private val flags: Int) {
    val windowSelected: Boolean get() = flags and 1 != 0
    val contentSelected: Boolean get() = flags and 2 != 0
    val eventPostingAllowed: Boolean get() = flags and 4 != 0

    companion object {
        /** Call away from EDT so AppKit can service accessibility requests through EDT while probing. */
        fun inspect(window: Window, screenPoint: Point): MoruNativePointerProbe {
            check(!SwingUtilities.isEventDispatchThread()) { "Native diagnostics must leave EDT available for AppKit accessibility" }
            val api = MacBackdropBridge.api
            var handle: Long? = null
            val capture = Runnable {
                if (window.isShowing) handle = findLayer(window)?.windowHandle
            }
            SwingUtilities.invokeAndWait(capture)
            val flags = handle?.let { api?.moru_backdrop_probe(it, screenPoint.x.toDouble(), screenPoint.y.toDouble()) } ?: 0
            return MoruNativePointerProbe(flags)
        }

        private fun findLayer(component: Component): SkiaLayer? = if (component is SkiaLayer) component
            else (component as? Container)?.components?.firstNotNullOfOrNull(::findLayer)
    }
}
