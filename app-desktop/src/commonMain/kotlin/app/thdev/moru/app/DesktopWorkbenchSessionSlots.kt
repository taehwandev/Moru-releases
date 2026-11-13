package app.thdev.moru.app

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import app.thdev.moru.feature.sessions.ProjectAgentActivity
import app.thdev.moru.feature.sessions.SessionBoard
import app.thdev.moru.feature.sessions.SessionPermissionActions
import app.thdev.moru.feature.sessions.SessionRowView
import app.thdev.moru.feature.sessions.SessionsPopover
import app.thdev.moru.feature.sessions.UnattachedSessions
import app.thdev.moru.feature.sessions.results.ResultsPage
import app.thdev.moru.core.terminal.deriveSessionActivity
import app.thdev.moru.feature.workbench.slots.WorkbenchSessionSlots

/** The real session rows, the list of every session and the activity judgement the workbench sidebar uses. */
internal val DesktopWorkbenchSessionSlots = WorkbenchSessionSlots(
    popover = { spec ->
        SessionsPopover(
            rows = spec.rows,
            tasks = spec.tasks,
            selectedProjectId = spec.selectedProjectId,
            onSelectSession = spec.onSelectSession,
            onOpenPreview = spec.onOpenPreview,
        )
    },
    row = { spec, modifier ->
        Column {
            SessionRowView(
                row = spec.row,
                onClick = spec.onClick,
                accent = spec.accent,
                modifier = modifier,
                indent = spec.indent,
                onOpenPreview = spec.onOpenPreview,
                actions = LocalSessionTaskMove.current?.takeIf { spec.row.taskKey != null }?.let { move ->
                    { SessionTaskMoveMenu(spec.row.sessionId, spec.row.title) { move(spec.row.sessionId) } }
                },
            )
        }
    },
    unattached = { spec -> UnattachedSessions(spec.rows, spec.onSelectSession, spec.onOpenPreview) },
    activityOf = { snapshot, nowMillis, programRunning ->
        deriveSessionActivity(snapshot, nowMillis, hasRunningProgram = { programRunning })
    },
    permissionActions = { sessionId, modifier -> SessionPermissionActions(sessionId, modifier = modifier) },
    board = { spec, modifier ->
        SessionBoard(spec.rows, spec.tasks, spec.onSelectSession, spec.onOpenPreview, modifier)
    },
    results = { spec, modifier ->
        val seen = LocalSeenResults.current
        ResultsPage(spec.rows, spec.tasks, spec.onSelectSession, seen.seen, seen.markSeen, modifier)
    },
    projectActivity = { spec, modifier -> ProjectAgentActivity(spec.rows, spec.tasks, spec.onSelectSession, modifier) },
)
