package app.thdev.moru.core.domain

import app.thdev.moru.core.model.TerminalWorkspaceState

interface TerminalWorkspaceRepository {
    fun loadWorkspaceState(): TerminalWorkspaceState

    fun saveWorkspaceState(state: TerminalWorkspaceState)
}
