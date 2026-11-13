package app.thdev.moru.core.data

import app.thdev.moru.core.model.DefaultTerminalWorkspaceId
import app.thdev.moru.core.model.DefaultTerminalWorkspaceName
import app.thdev.moru.core.model.TerminalWorkspaceSettings
import app.thdev.moru.core.model.TerminalWorkspaceState
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class RoomTerminalWorkspaceRepositoryTest {
    @Test
    fun loadWorkspaceStateCreatesDefaultWorkspaceWhenDatabaseIsEmpty() {
        withTempDir { tempDir ->
            val database = defaultDesktopMoruDatabase(tempDir.toFile())
            try {
                val repository = RoomTerminalWorkspaceRepository(database)

                val state = repository.loadWorkspaceState()

                assertEquals(DefaultTerminalWorkspaceId, state.selectedWorkspaceId)
                assertEquals(
                    listOf(
                        TerminalWorkspaceSettings(
                            id = DefaultTerminalWorkspaceId,
                            name = DefaultTerminalWorkspaceName,
                        ),
                    ),
                    state.workspaces,
                )
            } finally {
                database.close()
            }
        }
    }

    @Test
    fun saveWorkspaceStatePersistsAcrossDatabaseInstances() {
        withTempDir { tempDir ->
            val expectedState = TerminalWorkspaceState(
                workspaces = listOf(
                    TerminalWorkspaceSettings(id = "workspace-1", name = "Moru"),
                    TerminalWorkspaceSettings(id = "workspace-2", name = "Website"),
                ),
                selectedWorkspaceId = "workspace-2",
            )

            val firstDatabase = defaultDesktopMoruDatabase(tempDir.toFile())
            try {
                RoomTerminalWorkspaceRepository(firstDatabase)
                    .saveWorkspaceState(expectedState)
            } finally {
                firstDatabase.close()
            }

            val secondDatabase = defaultDesktopMoruDatabase(tempDir.toFile())
            try {
                val repository = RoomTerminalWorkspaceRepository(secondDatabase)

                assertEquals(expectedState, repository.loadWorkspaceState())
            } finally {
                secondDatabase.close()
            }
        }
    }

    private fun withTempDir(block: (java.nio.file.Path) -> Unit) {
        val tempDir = Files.createTempDirectory("moru-room-test")
        try {
            block(tempDir)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }
}
