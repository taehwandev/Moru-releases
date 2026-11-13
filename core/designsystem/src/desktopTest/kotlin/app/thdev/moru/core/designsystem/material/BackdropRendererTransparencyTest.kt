package app.thdev.moru.core.designsystem.material

import org.jetbrains.skiko.SkiaLayer
import java.awt.Color
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackdropRendererTransparencyTest {
    @Test
    fun closingRestoresBackgroundInheritanceWithoutChangingOpaqueChildren() = onEdt {
        val parent = JPanel().apply { background = Color.RED }
        val ancestor = JPanel().apply {
            background = null
            isOpaque = true
        }
        val child = JPanel().apply {
            background = Color.GREEN
            isOpaque = true
        }
        val layer = SkiaLayer()
        parent.add(ancestor)
        ancestor.add(layer)
        ancestor.add(child)
        try {
            assertFalse(ancestor.isBackgroundSet)
            assertEquals(Color.RED, ancestor.background)
            val previousTransparency = layer.transparency
            val adapter = BackdropRendererTransparency(layer)
            try {
                assertTrue(layer.transparency)
                assertFalse(ancestor.isOpaque)
                assertEquals(Color.GREEN, child.background)
                assertTrue(child.isOpaque)
            } finally {
                adapter.close()
            }

            assertEquals(previousTransparency, layer.transparency)
            assertTrue(ancestor.isOpaque)
            assertFalse(ancestor.isBackgroundSet)
            assertEquals(Color.RED, parent.background)
            parent.background = Color.BLUE
            assertEquals(Color.BLUE, ancestor.background, "Restored background must follow later parent changes")
            assertEquals(Color.GREEN, child.background)
            assertTrue(child.isOpaque)
        } finally {
            ancestor.removeAll()
            parent.removeAll()
            layer.dispose()
        }
    }

    private fun onEdt(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) return action()
        var result: Result<Unit>? = null
        SwingUtilities.invokeAndWait { result = runCatching(action) }
        checkNotNull(result).getOrThrow()
    }
}
