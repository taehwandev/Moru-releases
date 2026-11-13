package app.thdev.moru.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.RenderSettings
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import app.thdev.moru.core.designsystem.material.prepareMoruNativeWindow
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JLayeredPane
import javax.swing.JPanel

/** Owns one composition and explicitly disposes it after its parent view leaves. All calls are on the EDT. */
@OptIn(ExperimentalComposeUiApi::class)
internal class DesktopPaneHost(private val mainWindow: Window) {
    // A lightweight Swing surface can be reparented; a native Skia surface is tied to its first peer.
    val panel = ComposePanel(renderSettings = RenderSettings.SwingGraphics()).apply {
        isDisposeOnRemove = false
        background = java.awt.Color(0, 0, 0, 0)
        isOpaque = false
    }
    val dock: JPanel = OverlappingDock().apply { add(panel, BorderLayout.CENTER) }
    private val floatingDock = transparentContainer()
    var windowOwner by mutableStateOf(mainWindow)
        private set
    /**
     * The panel's Swing size, in dp. A SwingGraphics panel hosted inside another Compose window lays its root out
     * almost unbounded, so the content is sized from this instead of from the root constraints.
     */
    var size by mutableStateOf(IntSize.Zero)
        private set

    init {
        panel.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(event: ComponentEvent) { size = IntSize(panel.width, panel.height) }
        })
    }
    private var floating: ComposeWindow? = null
    private var floatingBounds: Rectangle? = null
    private var restore: () -> Unit = {}

    fun move(detached: Boolean, title: String, onRestore: () -> Unit) {
        restore = onRestore
        if (detached) {
            val frame = floating ?: ComposeWindow().apply {
                prepareMoruNativeWindow(this)
                defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
                minimumSize = Dimension(360, 280)
                iconImages = mainWindow.iconImages
                setSize(640, 720)
                setLocationRelativeTo(mainWindow)
                floatingBounds?.let { bounds = it }
                addWindowListener(object : WindowAdapter() {
                    override fun windowClosing(event: WindowEvent) { restore() }
                })
                // Keep a native Skiko window handle while the original SwingGraphics composition moves intact.
                setContent {
                    SwingPanel(factory = { floatingDock }, background = Color.Transparent, modifier = Modifier.fillMaxSize())
                }
            }.also { floating = it }
            frame.title = title
            if (panel.parent != floatingDock) {
                windowOwner = frame
                panel.parent?.remove(panel)
                floatingDock.add(panel, BorderLayout.CENTER)
                frame.isVisible = true
                frame.toFront()
                frame.requestFocus()
                panel.requestFocusInWindow()
            }
        } else if (panel.parent != dock) {
            val frame = floating!!
            floatingBounds = frame.bounds
            panel.parent?.remove(panel)
            dock.add(panel, BorderLayout.CENTER)
            windowOwner = mainWindow
            frame.dispose()
            floating = null
            dock.revalidate()
            dock.repaint()
            mainWindow.toFront()
            mainWindow.requestFocus()
            panel.requestFocusInWindow()
        }
    }

    fun dispose() {
        panel.parent?.remove(panel)
        floating?.dispose()
        floating = null
        panel.dispose()
        dock.removeAll()
        floatingDock.removeAll()
        restore = {}
    }

    private fun transparentContainer() = JPanel(BorderLayout()).apply {
        background = java.awt.Color(0, 0, 0, 0)
        isOpaque = false
    }
}

/**
 * The docked pane's Swing view. A window's embedded Swing views are siblings in one layered root, which reports that
 * its children never overlap until one of them carries a layer. Without it, a terminal that repaints for new output
 * paints straight over a pane shown above it. Marking this view's interop wrapper before the root finishes adding it
 * makes Swing repaint the views above a dirty one; panes that only sit beside others are unaffected.
 */
private class OverlappingDock : JPanel(BorderLayout()) {
    init {
        background = java.awt.Color(0, 0, 0, 0)
        isOpaque = false
    }

    override fun addNotify() {
        (parent as? JComponent)?.putClientProperty(JLayeredPane.LAYER_PROPERTY, JLayeredPane.DEFAULT_LAYER)
        super.addNotify()
    }
}
