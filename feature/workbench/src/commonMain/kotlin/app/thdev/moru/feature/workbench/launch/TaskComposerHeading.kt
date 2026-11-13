package app.thdev.moru.feature.workbench.launch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.i18n.composerStrings

/** What the screen asks. The project and folder are already named by the bar above, so they are not repeated here. */
@Composable
internal fun TaskComposerHeading() {
    val strings = composerStrings()
    Column(verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small)) {
        MoruText(strings.headingTitle, style = MoruTheme.typography.h1)
        MoruText(
            strings.headingBody,
            style = MoruTheme.typography.body,
            color = MoruTheme.colors.textSecondary,
        )
    }
}
