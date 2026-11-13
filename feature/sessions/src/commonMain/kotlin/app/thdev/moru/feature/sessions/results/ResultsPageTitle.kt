package app.thdev.moru.feature.sessions.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.sessions.i18n.sessionsStrings

/** The page's name and, under it, how much waits on the user and how much finished. */
@Composable
internal fun ResultsPageTitle(asking: Int, done: Int) {
    val strings = sessionsStrings()
    Column(
        Modifier.padding(horizontal = MoruTheme.spacing.small).padding(bottom = MoruTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall),
    ) {
        MoruText(strings.resultsTitle, style = MoruTheme.typography.headline)
        strings.resultsSummary(asking, done).takeIf(String::isNotEmpty)?.let {
            MoruText(it, style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary)
        }
    }
}
