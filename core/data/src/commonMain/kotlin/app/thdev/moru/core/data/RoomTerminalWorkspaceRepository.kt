package app.thdev.moru.core.data

import app.thdev.moru.core.domain.TerminalWorkspaceRepository
import app.thdev.moru.core.model.DefaultTerminalWorkspaceId
import app.thdev.moru.core.model.DefaultTerminalWorkspaceName
import app.thdev.moru.core.model.TerminalWorkspaceSettings
import app.thdev.moru.core.model.TerminalWorkspaceState
import kotlinx.coroutines.runBlocking

class RoomTerminalWorkspaceRepository(
    private val database: MoruDatabase,
) : TerminalWorkspaceRepository {
    override fun loadWorkspaceState(): TerminalWorkspaceState = runBlocking {
        val dao = database.terminalWorkspaceDao()
        val entities = dao.listWorkspaces()

        if (entities.isEmpty()) {
            val defaultState = TerminalWorkspaceState().normalized()

            dao.replaceWorkspaceState(
                workspaces = defaultState.workspaces.toEntities(),
                selectedWorkspaceId = defaultState.selectedWorkspaceId,
            )

            defaultState
        } else {
            val workspaces = entities.toWorkspaceSettings().normalizedWorkspaces()
            val selectedWorkspaceId = dao.getWorkspaceState()
                ?.selectedWorkspaceId
                .validWorkspaceId(workspaces)

            TerminalWorkspaceState(
                workspaces = workspaces,
                selectedWorkspaceId = selectedWorkspaceId,
            )
        }
    }

    override fun saveWorkspaceState(state: TerminalWorkspaceState) {
        runBlocking {
            val normalizedState = state.normalized()
            database.terminalWorkspaceDao().replaceWorkspaceState(
                workspaces = normalizedState.workspaces.toEntities(),
                selectedWorkspaceId = normalizedState.selectedWorkspaceId,
            )
        }
    }
}

private fun TerminalWorkspaceState.normalized(): TerminalWorkspaceState {
    val normalizedWorkspaces = workspaces.normalizedWorkspaces()
    return copy(
        workspaces = normalizedWorkspaces,
        selectedWorkspaceId = selectedWorkspaceId.validWorkspaceId(normalizedWorkspaces),
    )
}

private fun List<TerminalWorkspaceSettings>.normalizedWorkspaces(): List<TerminalWorkspaceSettings> {
    return distinctBy { workspace -> workspace.id }
        .filter { workspace -> workspace.id.isNotBlank() && workspace.name.isNotBlank() }
        .ifEmpty {
            listOf(TerminalWorkspaceSettings(id = DefaultTerminalWorkspaceId, name = DefaultTerminalWorkspaceName))
        }
}

private fun String?.validWorkspaceId(workspaces: List<TerminalWorkspaceSettings>): String {
    return takeIf { workspaceId -> workspaceId != null && workspaces.any { workspace -> workspace.id == workspaceId } }
        ?: workspaces.first().id
}

private fun List<TerminalWorkspaceEntity>.toWorkspaceSettings(): List<TerminalWorkspaceSettings> {
    return map { workspace ->
        TerminalWorkspaceSettings(
            id = workspace.id,
            name = workspace.name,
        )
    }
}

private fun List<TerminalWorkspaceSettings>.toEntities(): List<TerminalWorkspaceEntity> {
    return mapIndexed { index, workspace ->
        TerminalWorkspaceEntity(
            id = workspace.id,
            name = workspace.name,
            sortOrder = index,
        )
    }
}
