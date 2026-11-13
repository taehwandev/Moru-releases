package app.thdev.moru.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.thdev.moru.core.terminal.SessionPermissions
import app.thdev.moru.core.terminal.SessionResponseAttention
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.deriveSessionActivity
import app.thdev.moru.feature.sessions.rememberSessionNow
import app.thdev.moru.feature.workbench.resume.launchRuntimeOf

/**
 * Reads the sessions on the shared session clock, since a terminal going silent changes nothing the screen would
 * react to, and returns which Tasks need the user.
 */
@Composable
internal fun rememberAttention(navigation: WorkbenchNavigation, sessions: TerminalSessionManager): AttentionState {
    val now = rememberSessionNow()
    val permissions by SessionPermissions.requests.collectAsState()
    val responseRevision by SessionResponseAttention.revision.collectAsState()
    responseRevision
    val titles = navigation.sidebar.projects.flatMap { project -> project.tasks }.associate { task -> task.id to task.title }
    val snapshots = sessions.listSessions()
    val busy = snapshots.filter { session ->
        session.spec.kind.isTaskSession &&
            deriveSessionActivity(session, now).let { observed ->
                val activity = if (launchRuntimeOf(session.spec) != null) SessionResponseAttention.project(session.id, observed) else observed
                activity == SessionActivity.Working || activity == SessionActivity.Starting
            }
    }
    val working = busy.mapNotNull { session ->
        val taskId = session.spec.attachedItemId ?: return@mapNotNull null
        val agent = launchRuntimeOf(session.spec)?.displayName ?: return@mapNotNull null
        WorkingAgent(taskId, session.id, titles[taskId] ?: return@mapNotNull null, agent)
    }
    return AttentionState(attentionCandidates(titles, snapshots, now, permissions = permissions), busy.isNotEmpty(), working)
}
