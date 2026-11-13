package app.thdev.moru.core.designsystem.material

import org.jetbrains.skiko.SkiaLayer
import java.awt.Color
import java.awt.Component
import javax.swing.JComponent

/** Clear the renderer's heavyweight canvas and ancestors; embedded terminals keep opaque painting. */
internal class BackdropRendererTransparency(private val layer: SkiaLayer) : AutoCloseable {
    private data class Snapshot(
        val component: Component,
        val background: Color?,
        val backgroundSet: Boolean,
        val opaque: Boolean?,
    )
    private val previousTransparency = layer.transparency
    private val snapshots = (sequenceOf<Component>(layer.canvas) + generateSequence<Component>(layer) { it.parent }
        .takeWhile { it !is java.awt.Window })
        .map { Snapshot(it, it.background, it.isBackgroundSet, (it as? JComponent)?.isOpaque) }.toList()

    init {
        layer.transparency = true
        snapshots.forEach {
            it.component.background = Color(0, 0, 0, 0)
            (it.component as? JComponent)?.isOpaque = false
        }
        layer.rootPane?.repaint()
    }

    override fun close() {
        layer.transparency = previousTransparency
        // Restore parents before the heavyweight canvas so its peer resolves inherited colors correctly.
        snapshots.asReversed().forEach {
            it.component.background = if (it.backgroundSet) it.background else null
            if (it.opaque != null) (it.component as JComponent).isOpaque = it.opaque
        }
        layer.rootPane?.repaint()
    }
}
