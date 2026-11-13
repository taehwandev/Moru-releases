package app.thdev.moru.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.thdev.moru.app.i18n.appStrings
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme

/**
 * Asks before quitting ends [runningTerminals] terminals while Remember Last State is off. The box lets the user keep
 * them running this one time; Quit reports whether it was ticked, Cancel keeps Moru open.
 */
@Composable
internal fun QuitConfirmContent(runningTerminals: Int, onQuit: (keepTerminals: Boolean) -> Unit, onCancel: () -> Unit) {
    val strings = appStrings()
    var keep by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().ckSurface(MoruSurfaceRole.Panel).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MoruText(text = strings.quitTitle, style = MoruTheme.typography.h2)
        MoruText(text = strings.quitEndsTerminals(runningTerminals), style = MoruTheme.typography.body, color = MoruTheme.colors.textSecondary)
        Row(
            Modifier
                .clickable(role = Role.Checkbox) { keep = !keep }
                .semantics(mergeDescendants = true) {
                    contentDescription = strings.quitKeepTerminals
                    stateDescription = if (keep) "checked" else "unchecked"
                }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MoruIcon(name = if (keep) MoruIconName.SquareChecked else MoruIconName.Square, size = 16.dp)
            MoruText(text = strings.quitKeepTerminals, style = MoruTheme.typography.body)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            MoruButton(text = strings.quitCancel, onClick = onCancel, type = MoruButtonType.SECONDARY)
            MoruButton(
                text = strings.quitConfirm,
                onClick = { onQuit(keep) },
                type = if (keep) MoruButtonType.PRIMARY else MoruButtonType.DANGER,
            )
        }
    }
}
