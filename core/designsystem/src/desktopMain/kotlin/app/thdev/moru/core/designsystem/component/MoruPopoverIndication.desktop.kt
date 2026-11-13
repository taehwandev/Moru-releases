package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.launch

/**
 * Desktop popup windows are sized from Skia's recorded drawing bounds. Material's circular
 * state layer enlarges those bounds on hover even inside a clipped popup in Compose 1.10.3.
 * A rectangular state layer keeps feedback inside the clickable's measured area.
 */
internal data class MoruPopoverIndication(private val color: Color) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PopoverIndicationNode(interactionSource, color)
}

private class PopoverIndicationNode(
    private val source: InteractionSource,
    private val color: Color,
) : Modifier.Node(), DrawModifierNode {
    private val active = mutableStateListOf<Interaction>()

    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collect { interaction ->
                when (interaction) {
                    is HoverInteraction.Enter, is FocusInteraction.Focus, is PressInteraction.Press -> active.add(interaction)
                    is HoverInteraction.Exit -> active.remove(interaction.enter)
                    is FocusInteraction.Unfocus -> active.remove(interaction.focus)
                    is PressInteraction.Release -> active.remove(interaction.press)
                    is PressInteraction.Cancel -> active.remove(interaction.press)
                }
            }
        }
    }

    override fun onDetach() { active.clear() }

    override fun ContentDrawScope.draw() {
        drawContent()
        val alpha = when {
            active.any { it is PressInteraction.Press } -> 0.16f
            active.any { it is FocusInteraction.Focus } -> 0.12f
            active.any { it is HoverInteraction.Enter } -> 0.08f
            else -> 0f
        }
        if (alpha > 0f) drawRect(color.copy(alpha = alpha))
    }
}
