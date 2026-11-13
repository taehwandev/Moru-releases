package app.thdev.moru.feature.workbench.sidebar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.i18n.workbenchStrings

@Composable
internal fun SidebarEmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(MoruTheme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small),
    ) {
        val strings = workbenchStrings()
        MoruText(strings.emptyTitle, style = MoruTheme.typography.body)
        MoruText(
            strings.emptyBody,
            style = MoruTheme.typography.caption,
            color = MoruTheme.colors.textSecondary,
        )
    }
}
