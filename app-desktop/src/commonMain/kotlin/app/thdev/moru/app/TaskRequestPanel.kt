package app.thdev.moru.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.app.i18n.appStrings
import app.thdev.moru.core.designsystem.component.*
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** Keeps the submitted intent available while reviewing the Task's terminal or changes. */
@Composable
internal fun TaskRequestPanel(request: String) {
    var expanded by remember { mutableStateOf(false) }
    val strings = appStrings()
    Column(Modifier.fillMaxWidth().ckSurface(MoruSurfaceRole.Panel)) {
        MoruButton(
            text = if (expanded) strings.hideTaskBrief else strings.taskBrief,
            onClick = { expanded = !expanded },
            type = MoruButtonType.GHOST,
        )
        if (expanded) {
            SelectionContainer {
                MoruText(
                    request,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp)
                        .verticalScroll(rememberScrollState()).padding(MoruTheme.spacing.medium),
                )
            }
        }
    }
}
