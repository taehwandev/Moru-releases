package app.thdev.moru.app

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.app.i18n.appStrings
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

@Composable
internal fun WorkbenchUnavailableProject(name: String, onFindFolder: () -> Unit, onOpenProject: () -> Unit) {
    val strings = appStrings()
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MoruText(strings.unavailableTitle, style = MoruTheme.typography.h2)
            MoruText(strings.unavailableBody(name))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MoruButton(strings.findProjectFolder, onFindFolder)
                MoruButton(strings.openProject, onOpenProject, type = MoruButtonType.SECONDARY)
            }
        }
    }
}
