package app.thdev.moru.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import app.thdev.moru.core.model.session.AgentSessionRow

@Composable
internal fun ObserveViewedSessionResponse(
    rows: List<AgentSessionRow>,
    taskId: String?,
    workVisible: Boolean,
    windowFocused: Boolean,
) {
    val observer = remember { ViewedSessionResponseObserver() }
    val currentRows by rememberUpdatedState(rows)
    val selected = rows.firstOrNull { it.selected && it.taskKey == taskId && taskId != null }?.sessionId
    val opening by ViewedSessionResponseObserver.openings.collectAsState()
    val explicitOpening = opening?.takeIf { it.sessionId == selected }?.revision ?: 0L
    LaunchedEffect(selected, workVisible, windowFocused, explicitOpening) {
        observer.openedSession(selected, workVisible, windowFocused, explicitOpening)?.let { sessionId ->
            acknowledgeViewedSessionResponse(currentRows, sessionId, taskId, workVisible, windowFocused)
        }
    }
}
