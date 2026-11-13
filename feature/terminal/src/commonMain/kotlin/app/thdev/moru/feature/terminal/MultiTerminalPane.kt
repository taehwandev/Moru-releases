package app.thdev.moru.feature.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.model.panel.PanelArrangement
import app.thdev.moru.core.model.panel.PanelLayouts
import app.thdev.moru.core.model.panel.PanelNode
import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionStatus
import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity.canAddTaskTerminalSession
import app.thdev.moru.core.designsystem.component.MoruBadgeType
import app.thdev.moru.core.designsystem.component.MoruDropEdge
import app.thdev.moru.core.designsystem.component.MoruDropPreview
import app.thdev.moru.core.designsystem.component.MoruPanelCell
import app.thdev.moru.core.designsystem.component.MoruPanelDragState
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruStatusDot
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.MoruPaneHeader

@Composable
fun MultiTerminalPane(
    sessionManager: TerminalSessionManager,
    defaultWorkingDirectory: String,
    defaultShell: String,
    workspaceId: String,
    workspaceName: String,
    terminalFontFamily: String?,
    terminalFontSize: Int,
    terminalScrollbackLines: Int,
    onOpenExternalTerminal: (TerminalSessionSnapshot) -> Unit = {},
    modifier: Modifier = Modifier,
    onNewTerminal: (() -> Unit)? = null,
    /** Names each terminal's branch for its header, off the UI thread; none shows no branch. */
    sessionBranch: ((sessionId: String, folder: String) -> String?)? = null,
    /** How the terminals last shared the area, or null for an even split; kept as terminals open and close by [TerminalArrangement]. */
    split: PanelNode? = null,
    /** The user moved or reset a border between terminals. */
    onSplitChange: (PanelNode) -> Unit = {},
    /** Whether choosing a terminal here also makes it the app's selected session; off for a Task shown beside. */
    publishesSelection: Boolean = true,
    /** Other panels arranged among the terminals, such as a device, after them in the arrangement. */
    panels: List<MoruPanelCell> = emptyList(),
    /** The app's chooser can add panels even when this Task has eight terminals. */
    canOpenPanels: Boolean = false,
    /** One of these terminals was chosen or took the keyboard, whether or not it is the app's selected session. */
    onActivate: (sessionId: String) -> Unit = {},
) {
    val selection = remember(sessionManager) { TerminalWorkspaceSelection(sessionManager, publishesSelection) }
    SideEffect { selection.publish = publishesSelection }
    val observedRevision by rememberTerminalRevision(sessionManager)

    fun createAndSelectTerminal(title: String) = selection.create(workspaceId, defaultWorkingDirectory, defaultShell, title)

    val sessions = remember(sessionManager, selection.reloadVersion, observedRevision) {
        sessionManager.listSessions()
    }
    val workspaceSessions = remember(sessions, workspaceId) {
        sessions.filter { session ->
            session.spec.kind.isTaskSession &&
                session.spec.attachedItemId == workspaceId
        }
    }

    LaunchedEffect(workspaceId, workspaceSessions.isEmpty()) {
        // Re-read the live list: a session may have been created for this workspace
        // (for example an agent launch) after the composition that scheduled this effect.
        val liveHasSession = sessionManager.listSessions().any { session ->
            session.spec.kind.isTaskSession &&
                session.spec.attachedItemId == workspaceId
        }
        if (workspaceSessions.isEmpty() && !liveHasSession && !selection.hasAttached(workspaceId)) {
            if (createAndSelectTerminal("Terminal 1")) selection.markAttached(workspaceId)
        }
    }

    val selectedSessionId = selection.selected(workspaceId, workspaceSessions)
    LaunchedEffect(workspaceId, selectedSessionId, observedRevision) {
        // A launch may select a new session after this composition scheduled the effect.
        val liveSessions = sessionManager.listSessions().filter {
            it.spec.kind.isTaskSession && it.spec.attachedItemId == workspaceId
        }
        selection.selected(workspaceId, liveSessions)?.let { selection.activate(workspaceId, it) }
    }
    val requestNewTerminal: () -> Unit = {
        // Recheck the manager when invoked: a menu or pointer callback can outlive its rendered frame.
        if (sessionManager.canAddTaskTerminalSession(workspaceId) || (canOpenPanels && onNewTerminal != null)) {
            if (onNewTerminal != null) onNewTerminal()
            else createAndSelectTerminal("Terminal ${workspaceSessions.size + 1}")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .ckSurface(MoruSurfaceRole.Terminal),
    ) {
        TerminalTabs(
            sessions = workspaceSessions,
            selectedSessionId = selectedSessionId,
            canCreateSplit = sessionManager.canAddTaskTerminalSession(workspaceId) || (canOpenPanels && onNewTerminal != null),
            badgeType = ::statusBadgeType,
            onSelectSession = { sessionId -> selection.select(workspaceId, sessionId); onActivate(sessionId) },
            onCloseSession = { session ->
                selection.close(workspaceId, session.id, workspaceSessions)
            },
            onNewTerminal = requestNewTerminal,
            panels = panels,
        )

        if (workspaceSessions.isEmpty()) {
            EmptyTerminalState(
                workspaceName = workspaceName,
                onNewTerminal = requestNewTerminal,
            )
            return@Column
        }

        val branches = rememberSessionBranches(
            visibleTerminalSessions(workspaceSessions, selectedSessionId).map { it.id },
            defaultWorkingDirectory,
            sessionBranch,
        )
        TerminalSplitGrid(
            workspaceId = workspaceId,
            sessions = workspaceSessions,
            selectedSessionId = selectedSessionId,
            branches = branches,
            split = split,
            onSplitChange = onSplitChange,
            terminalFontFamily = terminalFontFamily,
            terminalFontSize = terminalFontSize,
            terminalScrollbackLines = terminalScrollbackLines,
            onFocus = { session -> selection.select(workspaceId, session.id); onActivate(session.id) },
            onNativeFocus = { session -> selection.activate(workspaceId, session.id); onActivate(session.id) },
            onCommandCaptured = { session, command ->
                sessionManager.recordCommandHistory(session.id, command)
                selection.changed()
            },
            onOutput = { session, atMillis -> sessionManager.reportOutput(session.id, atMillis) },
            onExit = { session, exitCode ->
                sessionManager.reportExit(session.id, exitCode)
                selection.changed()
            },
            onOpenExternalTerminal = onOpenExternalTerminal,
            onClose = { session ->
                selection.close(workspaceId, session.id, workspaceSessions)
            },
            modifier = Modifier.weight(1f),
            panels = panels,
        )
    }
}

@Composable
private fun TerminalSplitGrid(
    workspaceId: String,
    sessions: List<TerminalSessionSnapshot>,
    selectedSessionId: String?,
    branches: Map<String, String>,
    split: PanelNode?,
    onSplitChange: (PanelNode) -> Unit,
    terminalFontFamily: String?,
    terminalFontSize: Int,
    terminalScrollbackLines: Int,
    onFocus: (TerminalSessionSnapshot) -> Unit,
    onNativeFocus: (TerminalSessionSnapshot) -> Unit,
    onClose: (TerminalSessionSnapshot) -> Unit,
    onOpenExternalTerminal: (TerminalSessionSnapshot) -> Unit,
    onCommandCaptured: (TerminalSessionSnapshot, String) -> Unit,
    onOutput: (TerminalSessionSnapshot, Long) -> Unit,
    onExit: (TerminalSessionSnapshot, Int?) -> Unit,
    modifier: Modifier = Modifier,
    panels: List<MoruPanelCell> = emptyList(),
) {
    val visibleSessions = visibleTerminalSessions(sessions, selectedSessionId)
    @Composable
    fun cell(session: TerminalSessionSnapshot, cellModifier: Modifier, headerModifier: Modifier, dropHint: MoruDropEdge?) {
        TerminalGridCell(
            session = session,
            selected = session.id == selectedSessionId,
            branch = branches[session.id],
            onFocus = { onFocus(session) },
            onNativeFocus = { onNativeFocus(session) },
            onClose = { onClose(session) },
            onOpenExternalTerminal = { onOpenExternalTerminal(session) },
            onCommandCaptured = { command -> onCommandCaptured(session, command) },
            onOutput = { atMillis -> onOutput(session, atMillis) },
            onExit = { exitCode -> onExit(session, exitCode) },
            terminalFontFamily = terminalFontFamily,
            terminalFontSize = terminalFontSize,
            terminalScrollbackLines = terminalScrollbackLines,
            modifier = cellModifier,
            headerModifier = headerModifier,
            dropHint = dropHint,
        )
    }
    // Dragging a terminal's header onto the middle of another swaps them; onto one of its sides puts it there.
    val drag = remember(workspaceId) { MoruPanelDragState() }
    val arrangement = remember(workspaceId) { TerminalArrangement() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .ckSurface(MoruSurfaceRole.Terminal),
    ) {
        // Terminals keep the places the user gave them; a new one takes the largest place, a closed one leaves its own.
        val keys = visibleSessions.map { it.id } + panels.map { it.key }
        val root = arrangement.resolve(split, keys)
        LaunchedEffect(workspaceId, root) { if (root != split) onSplitChange(root) }
        MoruDropPreview(drag)
        TerminalPanelTree(root, keys, onSplitChange, Modifier.weight(1f)) { index, cellModifier ->
            val handle = drag.handle(index) { from, to, edge ->
                onSplitChange(
                    edge.panelEdge()?.let { side -> PanelArrangement.move(root, from as Int, to as Int, side) }
                        ?: PanelLayouts.swap(root, from as Int, to as Int),
                )
            }
            val session = visibleSessions.getOrNull(index)
            if (session != null) {
                cell(session, cellModifier.then(drag.dropTarget(index)), handle, dropHint = drag.edge.takeIf { drag.target == index })
            } else {
                // A panel of another feature is moved and dropped on like a terminal.
                panels.getOrNull(index - visibleSessions.size)?.content?.invoke(cellModifier.then(drag.dropTarget(index)), handle)
            }
        }
        if (sessions.size > visibleSessions.size) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MoruPaneHeader.height)
                    .ckSurface(MoruSurfaceRole.Card)
                    .padding(horizontal = MoruPaneHeader.horizontalPadding),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MoruText(
                    text = "+${sessions.size - visibleSessions.size} more in the tabs",
                    color = MoruTheme.colors.textSecondary,
                    style = MoruTheme.typography.micro.copy(fontFamily = FontFamily.Monospace),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TerminalGridCell(
    session: TerminalSessionSnapshot,
    selected: Boolean,
    branch: String?,
    onFocus: () -> Unit,
    onNativeFocus: () -> Unit,
    onClose: () -> Unit,
    onOpenExternalTerminal: () -> Unit,
    onCommandCaptured: (String) -> Unit,
    onOutput: (Long) -> Unit,
    onExit: (Int?) -> Unit,
    terminalFontFamily: String?,
    terminalFontSize: Int,
    terminalScrollbackLines: Int,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    dropHint: MoruDropEdge? = null,
) {
    SessionPane(
        session = session,
        selected = selected,
        branch = branch,
        onFocus = onFocus,
        onNativeFocus = onNativeFocus,
        onClose = onClose,
        onOpenExternalTerminal = onOpenExternalTerminal,
        onCommandCaptured = onCommandCaptured,
        onOutput = onOutput,
        onExit = onExit,
        terminalFontFamily = terminalFontFamily,
        terminalFontSize = terminalFontSize,
        terminalScrollbackLines = terminalScrollbackLines,
        modifier = modifier,
        headerModifier = headerModifier,
        dropHint = dropHint,
    )
}

@Composable
private fun SessionPane(
    session: TerminalSessionSnapshot,
    selected: Boolean,
    branch: String?,
    onFocus: () -> Unit,
    onNativeFocus: () -> Unit,
    onClose: () -> Unit,
    onOpenExternalTerminal: () -> Unit,
    onCommandCaptured: (String) -> Unit,
    onOutput: (Long) -> Unit,
    onExit: (Int?) -> Unit,
    terminalFontFamily: String?,
    terminalFontSize: Int,
    terminalScrollbackLines: Int,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    dropHint: MoruDropEdge? = null,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            // A press anywhere selects the pane without a pressed highlight or a focus of its own, and without taking
            // the press from what is under it, so text in a Lab still selects by dragging.
            .pointerInput(onFocus) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    onFocus()
                }
            }
            .background(MoruTheme.colors.background)
            .ckSurface(if (selected) MoruSurfaceRole.TerminalSelected else MoruSurfaceRole.Terminal),
    ) {
        SessionPaneHeader(
            session = session,
            selected = selected,
            branch = branch,
            onClose = onClose,
            modifier = headerModifier,
            dropHint = dropHint,
        )
        when (session.spec.kind) {
            TerminalSessionKind.TERMINAL -> RealTerminalContainer(
                sessionKey = session.id,
                workingDirectory = session.spec.workingDirectory,
                shell = session.spec.shell,
                startupCommand = session.spec.startupCommand,
                hostKey = session.spec.hostKey,
                onCommandCaptured = onCommandCaptured,
                onOutput = onOutput,
                onExit = onExit,
                fontFamily = terminalFontFamily,
                fontSize = terminalFontSize,
                scrollbackLines = terminalScrollbackLines,
                focused = selected,
                onFocus = onNativeFocus,
                // A weight only shares the height; without fillMaxWidth the Swing terminal keeps the width it was
                // first created with, so a larger window left a narrow terminal and a dead strip beside it.
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            TerminalSessionKind.WEB_PREVIEW -> WebPreviewSurface(
                session = session,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            TerminalSessionKind.LAB -> LocalLabSessionPane.current?.invoke(session, selected, Modifier.fillMaxWidth().weight(1f))
                ?: WebPreviewSurface(session = session, modifier = Modifier.fillMaxWidth().weight(1f))
        }
    }
}

@Composable
private fun SessionPaneHeader(
    session: TerminalSessionSnapshot,
    selected: Boolean,
    branch: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    /** Where another terminal dragged over this one would land, or null. */
    dropHint: MoruDropEdge? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MoruPaneHeader.height)
            .ckSurface(MoruSurfaceRole.Terminal)
            // The terminal under it is native and cannot be drawn over, so the header shows where a drag will land.
            .background(if (dropHint != null) MoruTheme.colors.primary.copy(alpha = .18f) else Color.Transparent)
            .padding(horizontal = MoruPaneHeader.horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val label = LocalTerminalSessionLabel.current
        if (label != null) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically) { label(session, selected) }
        } else {
            StatusDot(type = statusBadgeType(session))
            MoruText(
                text = session.title,
                modifier = Modifier.weight(1f),
                color = if (selected) MoruTheme.colors.textPrimary else MoruTheme.colors.textSecondary,
                style = MoruTheme.typography.caption.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
            )
        }
        if (branch != null) SessionBranchLabel(branch)
        if (session.spec.kind != TerminalSessionKind.LAB) {
            MoruText(
                text = session.spec.shell,
                color = MoruTheme.colors.textSecondary,
                style = MoruTheme.typography.caption.copy(fontFamily = FontFamily.Monospace),
                maxLines = 1,
            )
        }
        MoruIconButton(
            onClick = onClose,
            description = "Close terminal",
            size = 22.dp,
        ) {
            MoruIcon(name = MoruIconName.Close, size = 12.dp, tint = MoruTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun WebPreviewSurface(
    session: TerminalSessionSnapshot,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(10.dp)
            .ckSurface(MoruSurfaceRole.Code)
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        MoruText(
            text = session.spec.url ?: "Web preview session",
            color = MoruTheme.colors.textSecondary,
            style = MoruTheme.typography.caption,
            maxLines = 1,
        )
    }
}

@Composable
private fun EmptyTerminalState(
    workspaceName: String,
    onNewTerminal: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .ckSurface(MoruSurfaceRole.Terminal)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MoruText(
                text = "No terminals in $workspaceName.",
                color = MoruTheme.colors.textSecondary,
                style = MoruTheme.typography.body,
            )
            TerminalAction(
                text = "create terminal",
                enabled = true,
                onClick = onNewTerminal,
            )
        }
    }
}

@Composable
private fun StatusDot(type: MoruBadgeType) {
    MoruStatusDot(type = type, size = 7.dp)
}

private fun statusBadgeType(session: TerminalSessionSnapshot): MoruBadgeType {
    return when (session.status) {
        TerminalSessionStatus.STARTING -> MoruBadgeType.WARNING
        TerminalSessionStatus.READY -> when (session.spec.kind) {
            TerminalSessionKind.TERMINAL, TerminalSessionKind.LAB -> MoruBadgeType.SUCCESS
            TerminalSessionKind.WEB_PREVIEW -> MoruBadgeType.INFO
        }
        TerminalSessionStatus.EXITED -> MoruBadgeType.NEUTRAL
        TerminalSessionStatus.FAILED -> MoruBadgeType.ERROR
    }
}
