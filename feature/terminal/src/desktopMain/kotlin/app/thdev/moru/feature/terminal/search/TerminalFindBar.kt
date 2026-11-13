package app.thdev.moru.feature.terminal.search

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/** A width-constrained native field: the query remains reachable in a narrow terminal split. */
internal class TerminalFindBar(
    onQueryChanged: () -> Unit,
    private val onNavigate: (backward: Boolean) -> Unit,
    private val onClose: () -> Unit,
    private val onEscape: () -> Unit,
) : JPanel(BorderLayout(6, 0)) {
    val queryField = JTextField().apply {
        name = "terminal-find-query"
        toolTipText = "Find in terminal screen and retained scrollback"
        accessibleContext.accessibleName = "Find in terminal"
        minimumSize = Dimension(32, 26)
        preferredSize = Dimension(180, 26)
        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(event: DocumentEvent) = onQueryChanged()
            override fun removeUpdate(event: DocumentEvent) = onQueryChanged()
            override fun changedUpdate(event: DocumentEvent) = onQueryChanged()
        })
    }
    val resultLabel = JLabel().apply {
        name = "terminal-find-count"
        accessibleContext.accessibleName = "Find results"
        toolTipText = "Matches in the current screen and retained scrollback"
    }
    val previousButton = button("↑", "Previous match (Shift+Enter)") { onNavigate(true) }
    val nextButton = button("↓", "Next match (Enter)") { onNavigate(false) }
    val closeButton = button("×", "Close find (Escape)", onClose)
    private val controls = JPanel(FlowLayout(FlowLayout.RIGHT, 2, 0)).apply {
        isOpaque = false
        add(resultLabel)
        add(previousButton)
        add(nextButton)
        add(closeButton)
    }

    init {
        name = "terminal-find-bar"
        border = BorderFactory.createEmptyBorder(5, 6, 5, 6)
        add(queryField, BorderLayout.CENTER)
        add(controls, BorderLayout.EAST)
        listOf<JComponent>(queryField, previousButton, nextButton, closeButton).forEach { child ->
            child.addKeyListener(object : KeyAdapter() {
                override fun keyPressed(event: KeyEvent) { handleFindKey(event) }
                override fun keyTyped(event: KeyEvent) {
                    if (event.keyChar == '\u001b' || event.keyChar == '\n') event.consume()
                }
            })
        }
        showResults(null, false)
    }

    fun focusQuery(selectAll: Boolean = true): Boolean {
        if (selectAll) queryField.selectAll()
        return queryField.requestFocusInWindow()
    }

    fun handleFindKey(event: KeyEvent): Boolean {
        if (event.id != KeyEvent.KEY_PRESSED) return false
        val handled = when {
            event.keyCode == KeyEvent.VK_ESCAPE -> { onEscape(); true }
            event.keyCode == KeyEvent.VK_F && (event.isMetaDown || event.isControlDown) && !event.isAltDown -> {
                focusQuery()
                true
            }
            event.source === queryField && event.keyCode == KeyEvent.VK_ENTER -> { onNavigate(event.isShiftDown); true }
            event.source === queryField && event.keyCode == KeyEvent.VK_UP -> { onNavigate(true); true }
            event.source === queryField && event.keyCode == KeyEvent.VK_DOWN -> { onNavigate(false); true }
            else -> false
        }
        if (handled) event.consume()
        return handled
    }

    fun showResults(result: com.jediterm.terminal.SubstringFinder.FindResult?, pending: Boolean) {
        val count = result?.items?.size ?: 0
        val hasQuery = queryField.text.isNotEmpty()
        resultLabel.text = when {
            !hasQuery -> ""
            pending -> "…"
            count == 0 -> "0 results"
            else -> "${result!!.selectedItem().index} / $count"
        }
        resultLabel.accessibleContext.accessibleDescription = when {
            !hasQuery -> "Enter text to find"
            pending -> "Searching"
            else -> resultLabel.text
        }
        previousButton.isEnabled = count > 0 && !pending
        nextButton.isEnabled = count > 0 && !pending
    }

    fun applyAppearance(canvas: Color, ink: Color) {
        background = canvas
        foreground = ink
        queryField.background = canvas
        queryField.foreground = ink
        queryField.caretColor = ink
        resultLabel.foreground = ink
        listOf(previousButton, nextButton, closeButton).forEach {
            it.background = canvas
            it.foreground = ink
        }
        val line = Color(ink.red, ink.green, ink.blue, 64)
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, line),
            BorderFactory.createEmptyBorder(5, 6, 5, 6),
        )
    }

    private fun button(text: String, description: String, action: () -> Unit) = TerminalFindButton(text).apply {
        toolTipText = description
        accessibleContext.accessibleName = description
        preferredSize = Dimension(25, 26)
        margin = java.awt.Insets(0, 0, 0, 0)
        addActionListener { action() }
    }
}
