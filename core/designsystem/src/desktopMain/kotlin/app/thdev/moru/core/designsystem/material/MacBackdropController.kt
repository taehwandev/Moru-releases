package app.thdev.moru.core.designsystem.material

import org.jetbrains.skiko.SkiaLayer
import java.awt.Component
import java.awt.Container
import java.awt.Window
import javax.swing.SwingUtilities

/** EDT owner of one native material, including the renderer state that lets the material show through. */
internal class MacBackdropController(
    private val window: Window,
    private val api: MacBackdropApi,
    private val role: Int,
    private var dark: Int = -1,
) : AutoCloseable {
    private var transparency: BackdropRendererTransparency? = null
    private var token = 0L

    fun attach(): Boolean {
        check(SwingUtilities.isEventDispatchThread())
        if (token != 0L) return true
        if (!window.isShowing) return false
        val layer = findLayer(window) ?: return false
        val handle = layer.windowHandle
        if (handle == 0L) return false
        transparency = BackdropRendererTransparency(layer)
        token = api.moru_backdrop_install(handle, role, if (role == 1) 20.0 else 0.0, dark)
        if (token == 0L) {
            transparency?.close()
            transparency = null
            return false
        }
        return true
    }

    fun kind(): Int = if (token == 0L) 0 else api.moru_backdrop_kind(token)

    /** Existing docked hosts may retain leases while the window's palette changes. */
    fun updateAppearance(dark: Int) {
        check(SwingUtilities.isEventDispatchThread())
        if (this.dark == dark) return
        this.dark = dark
        if (token != 0L) {
            close()
            attach()
        }
    }

    override fun close() {
        check(SwingUtilities.isEventDispatchThread())
        if (token != 0L) api.moru_backdrop_remove(token)
        token = 0L
        transparency?.close()
        transparency = null
    }

    private fun findLayer(component: Component): SkiaLayer? {
        if (component is SkiaLayer) return component
        return (component as? Container)?.components?.firstNotNullOfOrNull(::findLayer)
    }
}
