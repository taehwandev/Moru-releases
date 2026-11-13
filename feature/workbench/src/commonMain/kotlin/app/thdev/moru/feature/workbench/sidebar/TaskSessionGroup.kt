package app.thdev.moru.feature.workbench.sidebar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruDivider
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.component.scroll.stableScrollAnchor
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.terminal.SessionPermissions
import app.thdev.moru.core.ui.identity.taskIdentityAccent
import app.thdev.moru.feature.workbench.TaskRow
import app.thdev.moru.feature.workbench.TaskRowActions
import app.thdev.moru.feature.workbench.TaskRowView
import app.thdev.moru.feature.workbench.revealSidebarSelection
import app.thdev.moru.feature.workbench.slots.SessionRowSpec

/**
 * A Task title and its sessions, connected by a Task-accented guide line under the tag icon. When [collapseIdleSessions]
 * is true (an unselected Task while another Task in the Project is focused), only active or attention-needing sessions
 * and finished responses are expanded beneath the Task header while the summary still includes all sessions.
 */
@Composable
internal fun TaskSessionGroup(
    task: TaskRow,
    sessions: List<AgentSessionRow>,
    onSelectTask: () -> Unit,
    onSelectSession: (AgentSessionRow) -> Unit,
    taskActions: TaskRowActions,
    onOpenPreview: (sessionId: String, url: String) -> Unit,
    sessionRow: @Composable (spec: SessionRowSpec, modifier: Modifier) -> Unit,
    first: Boolean = true,
    reorderHandle: (@Composable () -> Unit)? = null,
    revealSelection: Boolean = true,
    collapseIdleSessions: Boolean = false,
) {
    val accent = taskIdentityAccent(task.id)
    val permissionRequests by SessionPermissions.requests.collectAsState()
    val colors = MoruTheme.colors
    val accentColor = accent.color(colors)
    val hasActiveChild = task.selected || sessions.any { it.selected }
    val guideAlpha = when {
        hasActiveChild -> if (colors.isLight) 0.72f else 0.65f
        else -> if (colors.isLight) 0.50f else 0.44f
    }
    val guideColor = accentColor.copy(alpha = guideAlpha)
    val visibleSessions = if (collapseIdleSessions) {
        sessions.filter { row ->
            row.selected || row.activity in ActiveOrAttentionSessions || row.sessionId in permissionRequests
        }
    } else {
        sessions
    }
    val sessionsReveal = remember { Animatable(1f) }
    var lastCollapsed by remember { mutableStateOf(collapseIdleSessions) }
    LaunchedEffect(collapseIdleSessions) {
        if (lastCollapsed != collapseIdleSessions) {
            lastCollapsed = collapseIdleSessions
            sessionsReveal.snapTo(0f)
            sessionsReveal.animateTo(1f, tween(durationMillis = TaskSessionExpandMillis, easing = FastOutSlowInEasing))
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        if (!first) {
            MoruDivider(
                Modifier
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .alpha(if (colors.isLight) 0.45f else 0.34f),
            )
        }
        Box(Modifier.stableScrollAnchor("task:${task.id}")
            .revealSidebarSelection(task.selected && sessions.none { it.selected }, revealSelection)) {
            TaskRowView(
                task = task,
                onClick = onSelectTask,
                actions = taskActions,
                accent = accent,
                reorderHandle = reorderHandle,
                sessions = sessions,
                sessionSummary = SidebarSessionSummary.from(sessions, permissionRequests.keys),
            )
        }
        if (visibleSessions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = sessionsReveal.value
                        translationY = (1f - sessionsReveal.value) * (-SessionRevealSlide.toPx())
                    }
                    .padding(start = GuideInset, top = 3.dp, bottom = 4.dp)
                    .drawBehind {
                        val strokePx = GuideStrokeWidth.toPx()
                        val x = strokePx / 2f
                        drawLine(
                            color = guideColor,
                            start = Offset(x, 2.dp.toPx()),
                            end = Offset(x, (size.height - 2.dp.toPx()).coerceAtLeast(2.dp.toPx())),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round,
                        )
                    }
                    .padding(start = SessionGap),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                visibleSessions.forEach { row ->
                    key(row.sessionId) {
                        sessionRow(
                            SessionRowSpec(
                                row = row,
                                onClick = { onSelectSession(row) },
                                indent = SessionContentInset,
                                accent = accent,
                                onOpenPreview = onOpenPreview,
                            ),
                            Modifier.stableScrollAnchor("session:${row.sessionId}")
                                .revealSidebarSelection(task.selected && row.selected, revealSelection),
                        )
                    }
                }
            }
        }
    }
}

private val ActiveOrAttentionSessions = setOf(
    SessionActivity.Starting,
    SessionActivity.Working,
    SessionActivity.Asking,
    SessionActivity.Failed,
    SessionActivity.Quiet,
)

/** Puts the guide line under the centre of the Task's tag: the Task row's 6dp padding plus half its 14dp tag. */
private val GuideInset = 12.dp

private val GuideStrokeWidth = 1.75.dp

/** The gap between the guide line and a session's selectable row; kept small so sessions sit close under their Task. */
private val SessionGap = 6.dp

/** Pads a session's content inside its row, just clear of the row's edge. */
private val SessionContentInset = 3.dp

private val SessionRevealSlide = 4.dp

private const val TaskSessionExpandMillis = 190
