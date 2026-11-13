package app.thdev.moru.feature.sessions.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.sessions.i18n.sessionsStrings

/** The page's list: its title, then what waits on the user, what finished and, folded, what was already seen. */
@Composable
internal fun ResultsPageList(
    asking: List<ResultItem>,
    done: List<ResultItem>,
    seenDone: List<ResultItem>,
    selectedId: String?,
    onSelect: (ResultItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = sessionsStrings()
    val row: @Composable (ResultItem) -> Unit = { item ->
        key(item.row.sessionId) { ResultListRow(item, item.row.sessionId == selectedId) { onSelect(item) } }
    }
    Column(
        modifier.verticalScroll(rememberScrollState())
            .padding(horizontal = MoruTheme.spacing.small, vertical = MoruTheme.spacing.large)
            .testTag("results-list"),
        verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall),
    ) {
        ResultsPageTitle(asking.size, done.size)
        if (asking.isNotEmpty()) {
            ResultsSectionHeading(strings.resultsAsking, asking.size, MoruTheme.colors.warning)
            asking.forEach { row(it) }
        }
        if (done.isNotEmpty()) {
            ResultsSectionHeading(strings.resultsDone, done.size, MoruTheme.colors.success)
            done.forEach { row(it) }
        }
        ResultSeenGroup(seenDone, row)
    }
}
