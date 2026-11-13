package app.thdev.moru.feature.terminal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionSpec

/**
 * Owns the pane's selection and user-driven reloads independently of periodic metadata observation. A pane that does
 * not [publish] keeps its choice to itself and never moves the app-wide selected session, so two panes on screen do
 * not take it from each other. [publish] can change while the pane stays, as when its window loses focus, and the
 * pane keeps its choices through that.
 */
internal class TerminalWorkspaceSelection(
    private val manager: TerminalSessionManager,
    publish: Boolean = true,
) {
    var publish by mutableStateOf(publish)
    var reloadVersion by mutableStateOf(0)
        private set
    private val selectedByWorkspace = mutableMapOf<String, String>()
    private val attachedWorkspaces = mutableSetOf<String>()

    fun create(workspace: String, directory: String, shell: String, title: String): Boolean {
        val created = runCatching { manager.createTaskTerminalSession(TerminalSessionSpec(
            title = title, shell = shell, workingDirectory = directory, attachedItemId = workspace,
        )) }.getOrNull() ?: return false
        if (publish) manager.selectSession(created.id)
        selectedByWorkspace[workspace] = created.id
        changed()
        return true
    }

    fun select(workspace: String, sessionId: String) {
        activate(workspace, sessionId)
        requestTerminalFocus(sessionId)
    }

    /** Publish the visible/native selection without asking to move the keyboard again. */
    fun activate(workspace: String, sessionId: String) {
        val selectionChanged = publish && manager.selectedSessionId() != sessionId
        val rememberedChanged = selectedByWorkspace[workspace] != sessionId
        if (selectionChanged) manager.selectSession(sessionId)
        selectedByWorkspace[workspace] = sessionId
        if (selectionChanged || rememberedChanged) changed()
    }

    fun selected(workspace: String, sessions: List<TerminalSessionSnapshot>): String? =
        manager.selectedSessionId()?.takeIf { id -> publish && sessions.any { it.id == id } }
            ?: selectedByWorkspace[workspace]?.takeIf { id -> sessions.any { it.id == id } }
            ?: sessions.firstOrNull()?.id

    fun hasAttached(workspace: String): Boolean = workspace in attachedWorkspaces

    fun markAttached(workspace: String) { attachedWorkspaces += workspace }

    fun close(workspace: String, sessionId: String, sessions: List<TerminalSessionSnapshot>) {
        val next = sessions.firstOrNull { it.id != sessionId }?.id
        manager.closeSession(sessionId)
        releaseTerminalSession(sessionId)
        if (next == null) selectedByWorkspace.remove(workspace) else selectedByWorkspace[workspace] = next
        changed()
    }

    fun changed() { reloadVersion += 1 }
}
