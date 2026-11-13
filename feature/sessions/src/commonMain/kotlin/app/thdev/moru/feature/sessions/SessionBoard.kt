package app.thdev.moru.feature.sessions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruStatusGlyphKind
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionTaskRef
import app.thdev.moru.core.terminal.SessionPermissions
import app.thdev.moru.feature.sessions.i18n.sessionsStrings

/**
 * Every observed session remains visible under its status, Project, and Task. A finished response
 * describes a provider turn, not whether the Task goal was met.
 */
@Composable
fun SessionBoard(
    rows: List<AgentSessionRow>,
    tasks: Map<String, SessionTaskRef>,
    onSelectSession: (row: AgentSessionRow, projectId: String?) -> Unit,
    onOpenPreview: (sessionId: String, url: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = sessionsStrings()
    val permissionRequests by SessionPermissions.requests.collectAsState()
    val bySection = rows.groupBy { it.boardSection(it.sessionId in permissionRequests) }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 8.dp)) {
        if (rows.isEmpty()) SessionBoardEmpty(strings.empty)
        SessionBoardGroup(SessionBoardSection.NeedsAttention, strings.boardNeedsAttention, MoruStatusGlyphKind.NeedsYou,
            bySection[SessionBoardSection.NeedsAttention].orEmpty().byBoardUrgency(), tasks, onSelectSession, onOpenPreview)
        SessionBoardGroup(SessionBoardSection.AwaitingReply, strings.boardAwaitingReply, MoruStatusGlyphKind.NeedsYou,
            bySection[SessionBoardSection.AwaitingReply].orEmpty(), tasks, onSelectSession, onOpenPreview)
        SessionBoardGroup(SessionBoardSection.Running, strings.boardRunning, MoruStatusGlyphKind.Working,
            bySection[SessionBoardSection.Running].orEmpty(), tasks, onSelectSession, onOpenPreview)
        SessionBoardGroup(SessionBoardSection.Completed, strings.boardCompleted, MoruStatusGlyphKind.Done,
            bySection[SessionBoardSection.Completed].orEmpty(), tasks, onSelectSession, onOpenPreview)
        SessionBoardGroup(SessionBoardSection.Ready, strings.boardReady, MoruStatusGlyphKind.Idle,
            bySection[SessionBoardSection.Ready].orEmpty(), tasks, onSelectSession, onOpenPreview)
    }
}
