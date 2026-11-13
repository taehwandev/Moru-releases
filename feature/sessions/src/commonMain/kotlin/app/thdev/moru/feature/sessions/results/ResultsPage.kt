package app.thdev.moru.feature.sessions.results

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruEmptyState
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruVerticalDivider
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionTaskRef
import app.thdev.moru.core.terminal.SessionPermissions
import app.thdev.moru.core.terminal.SessionResults
import app.thdev.moru.feature.sessions.i18n.sessionsStrings

/**
 * The Moru page: outcomes, not processes, across every Project. The list holds what waits on the user, then what
 * finished, newest first, and the results already marked seen ([seen], by [ResultItem.seenKey]) folded at the end. The
 * chosen one reads in full beside it, with the exchanges that led to it, and is answered or continued right there.
 * Running and idle sessions are left to the other views.
 */
@Composable
fun ResultsPage(
    rows: List<AgentSessionRow>,
    tasks: Map<String, SessionTaskRef>,
    onOpenSession: (row: AgentSessionRow, projectId: String?) -> Unit,
    seen: Set<Int>,
    onSeen: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = sessionsStrings()
    val permissions by SessionPermissions.requests.collectAsState()
    val results by SessionResults.results.collectAsState()
    val changes by SessionResults.changes.collectAsState()
    val threads by SessionResults.threads.collectAsState()
    val items = resultItems(rows, tasks, permissions.keys, results)
    val asking = items.filter { it.kind != ResultKind.Done }
    val (seenDone, done) = items.filter { it.kind == ResultKind.Done }.partition { it.seenKey in seen }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = (asking + done + seenDone).let { all -> all.firstOrNull { it.row.sessionId == selectedId } ?: all.firstOrNull() }
    Row(modifier.fillMaxSize().background(MoruTheme.colors.background).testTag("results-page")) {
        if (selected == null) {
            MoruEmptyState(strings.resultsEmpty, icon = MoruIconName.Sparkles)
            return@Row
        }
        ResultsPageList(asking, done, seenDone, selected.row.sessionId, onSelect = { selectedId = it.row.sessionId },
            modifier = Modifier.width(ListWidth).fillMaxHeight().background(MoruTheme.colors.surface))
        MoruVerticalDivider()
        key(selected.row.sessionId) {
            ResultDetail(
                item = selected,
                changedFiles = changes[selected.row.sessionId],
                thread = threads[selected.row.sessionId].orEmpty(),
                onOpen = { onOpenSession(selected.row, selected.task?.projectId) },
                onSeen = if (selected in done) {
                    {
                        // The next unseen result takes its place, so a run of results is read one after another.
                        val index = done.indexOf(selected)
                        selectedId = (done.getOrNull(index + 1) ?: done.getOrNull(index - 1))?.row?.sessionId ?: selectedId
                        onSeen(selected.seenKey)
                    }
                } else null,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private val ListWidth = 320.dp
