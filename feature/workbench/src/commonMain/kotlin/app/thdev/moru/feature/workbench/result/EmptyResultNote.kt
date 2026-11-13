package app.thdev.moru.feature.workbench.result

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.moru.core.designsystem.component.MoruEmptyState
import app.thdev.moru.core.designsystem.component.MoruIconName

/** What a Result tab says while it has nothing to show, under the tab's own [icon] when it has one. */
@Composable
fun EmptyResultNote(text: String, modifier: Modifier = Modifier, icon: MoruIconName? = null) {
    MoruEmptyState(text = text, modifier = modifier, icon = icon)
}
