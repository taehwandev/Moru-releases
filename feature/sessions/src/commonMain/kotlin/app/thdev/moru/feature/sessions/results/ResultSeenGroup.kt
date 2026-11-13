package app.thdev.moru.feature.sessions.results

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonSize
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.feature.sessions.i18n.sessionsStrings

/** The results the user marked seen, folded under one line so the unseen ones stay in front. */
@Composable
internal fun ResultSeenGroup(items: List<ResultItem>, row: @Composable (ResultItem) -> Unit) {
    if (items.isEmpty()) return
    val strings = sessionsStrings()
    var open by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().testTag("results-seen")) {
        MoruButton(strings.resultsSeen(items.size), onClick = { open = !open }, type = MoruButtonType.GHOST,
            size = MoruButtonSize.Small, icon = if (open) MoruIconName.ChevronDown else MoruIconName.ChevronRight)
        if (open) items.forEach { row(it) }
    }
}
