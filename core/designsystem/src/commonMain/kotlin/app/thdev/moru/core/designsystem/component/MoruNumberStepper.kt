package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * A number field between a minus and a plus button, all at [MoruControlHeight]. The caller owns the text, so a draft
 * that is not a number yet stays editable; [onDecrease] and [onIncrease] run only while their side is enabled.
 */
@Composable
fun MoruNumberStepper(
    text: String,
    onTextChange: (String) -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseDescription: String,
    increaseDescription: String,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    decreaseModifier: Modifier = Modifier,
    increaseModifier: Modifier = Modifier,
    canDecrease: Boolean = true,
    canIncrease: Boolean = true,
    isError: Boolean = false,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoruStepperButton(MoruIconName.Minus, decreaseDescription, canDecrease, onDecrease, decreaseModifier)
        MoruCompactTextField(
            value = text,
            onValueChange = onTextChange,
            isError = isError,
            textAlign = TextAlign.Center,
            modifier = fieldModifier.width(64.dp),
        )
        MoruStepperButton(MoruIconName.Plus, increaseDescription, canIncrease, onIncrease, increaseModifier)
    }
}

@Composable
private fun MoruStepperButton(
    icon: MoruIconName,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val style = moruButtonStyle(MoruButtonType.SECONDARY, hovered = enabled && hovered, pressed = enabled && pressed)
    Box(
        modifier = modifier
            .width(MoruControlHeight)
            .moruControlFrame(hovered = hovered, enabled = enabled)
            .hoverable(interaction, enabled)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        MoruIcon(icon, size = 14.dp, tint = style.content)
    }
}
