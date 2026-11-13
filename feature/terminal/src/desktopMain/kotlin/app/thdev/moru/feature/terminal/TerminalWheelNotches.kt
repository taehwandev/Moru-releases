package app.thdev.moru.feature.terminal

import java.awt.event.InputEvent
import java.awt.event.MouseWheelEvent
import kotlin.math.abs
import kotlin.math.sign

/**
 * A macOS trackpad sends fractional wheel events whose whole rotation is often 0. JediTerm builds the CLI's wheel
 * report (and its alternate-screen arrow keys) from that whole rotation and drops a 0, so an agent TUI never scrolls.
 * Collect the precise rotation instead and hand over one whole notch per full step. A wheel turned while a selection
 * holds the button goes without the held buttons, which JediTerm would otherwise report as a press.
 */
internal class TerminalWheelNotches {
    private var pending = 0.0

    fun split(event: MouseWheelEvent): List<MouseWheelEvent> {
        val precise = event.preciseWheelRotation
        if (!precise.isFinite() || precise == 0.0) return emptyList()
        if (pending != 0.0 && sign(pending) != sign(precise)) pending = 0.0
        pending += precise
        val notches = pending.toInt()
        pending -= notches
        val direction = sign(notches.toDouble()).toInt()
        return List(abs(notches)) {
            MouseWheelEvent(
                event.component, event.id, event.`when`, event.modifiersEx and HeldButtons.inv(), event.x, event.y,
                event.xOnScreen, event.yOnScreen, event.clickCount, event.isPopupTrigger,
                event.scrollType, event.scrollAmount, direction, direction.toDouble(),
            )
        }
    }

    fun reset() {
        pending = 0.0
    }
}

private const val HeldButtons =
    InputEvent.BUTTON1_DOWN_MASK or InputEvent.BUTTON2_DOWN_MASK or InputEvent.BUTTON3_DOWN_MASK
