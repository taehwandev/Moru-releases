package app.thdev.moru.feature.terminal.search

import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.plaf.basic.BasicButtonUI

/** Keeps native button input/accessibility while avoiding macOS chrome that ignores the terminal's dark palette. */
internal class TerminalFindButton(text: String) : JButton(text) {
    init {
        setUI(BasicButtonUI())
        isContentAreaFilled = false
        isOpaque = false
        isRolloverEnabled = true
        border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
    }

    override fun paintComponent(graphics: Graphics) {
        val canvas = graphics.create() as Graphics2D
        try {
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val emphasis = if (model.isPressed) 0.22f else if (model.isRollover) 0.14f else 0.08f
            canvas.color = background.blend(foreground, emphasis)
            canvas.fillRoundRect(1, 1, width - 2, height - 2, 6, 6)
            canvas.color = if (isEnabled) foreground else background.blend(foreground, 0.45f)
            canvas.font = font
            val metrics = canvas.fontMetrics
            canvas.drawString(text, (width - metrics.stringWidth(text)) / 2, (height - metrics.height) / 2 + metrics.ascent)
            if (hasFocus()) {
                canvas.color = foreground
                canvas.drawRoundRect(1, 1, width - 3, height - 3, 6, 6)
            }
        } finally {
            canvas.dispose()
        }
    }

    private fun Color.blend(other: Color, amount: Float) = Color(
        (red + (other.red - red) * amount).toInt(),
        (green + (other.green - green) * amount).toInt(),
        (blue + (other.blue - blue) * amount).toInt(),
    )
}
