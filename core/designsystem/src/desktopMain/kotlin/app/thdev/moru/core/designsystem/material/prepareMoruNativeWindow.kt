package app.thdev.moru.core.designsystem.material

import java.awt.Window
import javax.swing.RootPaneContainer

/** Prepare Java2D's translucent backing before macOS creates the heavyweight window peer. */
fun prepareMoruNativeWindow(window: Window) {
    if (!System.getProperty("os.name").startsWith("Mac") || window !is RootPaneContainer) return
    check(!window.isDisplayable) { "Native material preparation must precede window realization" }
    // This documented macOS property suppresses Java2D's opaque window-background fill.
    // AppKit supplies the visible material; embedded Swing terminals still paint opaque pixels.
    window.rootPane.putClientProperty("apple.awt.brushMetalLook", true)
}
