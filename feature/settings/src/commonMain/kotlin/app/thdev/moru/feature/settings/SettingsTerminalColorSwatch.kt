package app.thdev.moru.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.terminal.TerminalColorSlot
import app.thdev.moru.core.ui.terminal.terminalHexColor
import app.thdev.moru.feature.settings.i18n.settingsStrings

/** One color to pick for editing; a dot marks a color the user set instead of Moru's. */
@Composable
internal fun SettingsTerminalColorSwatch(
    slot: TerminalColorSlot,
    hex: String?,
    overridden: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val name = settingsStrings().terminalColorSlot(slot)
    val shape = RoundedCornerShape(MoruTheme.radius.xSmall)
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(shape)
            .background(terminalHexColor(hex) ?: Color.Transparent)
            .border(if (selected) 2.dp else 1.dp, if (selected) MoruTheme.colors.primary else MoruTheme.colors.border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = name
                this.selected = selected
            }
            .testTag("settings-terminal-color-${slot.name}"),
        contentAlignment = Alignment.TopEnd,
    ) {
        if (overridden) {
            Box(
                Modifier.padding(3.dp).size(6.dp).clip(CircleShape).background(MoruTheme.colors.primary)
                    .border(1.dp, MoruTheme.colors.background, CircleShape),
            )
        }
    }
}
