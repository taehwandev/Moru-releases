package app.thdev.moru.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.app.i18n.appStrings
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

/**
 * What the main window shows for a Project open in a window of its own: its terminals are drawn there only, so this
 * offers to bring that window forward or to move the Project back here.
 */
@Composable
internal fun ProjectInOwnWindow(name: String, onBringForward: () -> Unit, onMoveHere: () -> Unit) {
    val strings = appStrings()
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MoruText(strings.projectInOwnWindowTitle(name), style = MoruTheme.typography.h2)
            MoruText(strings.projectInOwnWindowBody, color = MoruTheme.colors.textSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MoruButton(strings.bringWindowForward, onBringForward)
                MoruButton(strings.moveToMainWindow, onMoveHere, type = MoruButtonType.SECONDARY)
            }
        }
    }
}
