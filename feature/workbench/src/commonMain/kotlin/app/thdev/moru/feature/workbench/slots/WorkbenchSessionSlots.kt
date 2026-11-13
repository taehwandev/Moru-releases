package app.thdev.moru.feature.workbench.slots

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.terminal.TerminalSessionSnapshot

/**
 * The terminal sessions the sidebar shows: [popover] is the header's list of every session, [row] one session under
 * its Task and [unattached] the sessions that belong to no Task. [activityOf] judges what a session is doing at a
 * time, given whether its shell runs a program (null when that is unknown). [permissionActions] answers a session's
 * live permission request with the same controls the sidebar row shows; it draws nothing without one. [board] is the
 * sidebar's second view; [results] is the page the middle shows in the third, the Moru view of what finished and what
 * waits on the user, while the sidebar keeps its Projects. [projectActivity] lists, under a collapsed Project, the observed sessions it owns that are
 * still at work; the session renderer decides which observed states count.
 */
@Immutable
data class WorkbenchSessionSlots(
    val popover: @Composable (spec: SessionsPopoverSpec) -> Unit,
    val row: @Composable (spec: SessionRowSpec, modifier: Modifier) -> Unit,
    val unattached: @Composable (spec: UnattachedSessionsSpec) -> Unit,
    val activityOf: (snapshot: TerminalSessionSnapshot, nowMillis: Long, programRunning: Boolean?) -> SessionActivity,
    val permissionActions: @Composable (sessionId: String, modifier: Modifier) -> Unit = { _, _ -> },
    val board: @Composable (spec: SessionBoardSpec, modifier: Modifier) -> Unit = { _, _ -> },
    val results: @Composable (spec: SessionBoardSpec, modifier: Modifier) -> Unit = { _, _ -> },
    val projectActivity: @Composable (spec: ProjectActivitySpec, modifier: Modifier) -> Unit = { _, _ -> },
)
