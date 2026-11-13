package app.thdev.moru.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.thdev.moru.core.runtime.AgentCli
import app.thdev.moru.core.runtime.RuntimeStatusRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The agent CLIs installed on this machine and each one's version and update state, by provider id. Both are read on
 * [io] when Settings opens; an update runs only when the user presses a CLI's button, never on its own.
 */
@Stable
internal class AgentCliState(
    private val repository: RuntimeStatusRepository,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    /** Null until the first look for installed CLIs has finished. */
    var clis by mutableStateOf<List<AgentCli>?>(null)
        private set

    var views by mutableStateOf(emptyMap<String, CliUpdateView>())
        private set

    suspend fun load() = coroutineScope {
        val found = withContext(io) { repository.installedAgentClis() }
        clis = found
        found.forEach { cli ->
            val id = cli.provider
            launch {
                val view = withContext(io) {
                    CliUpdateView(installed = repository.installedVersion(id), command = repository.updateCommand(id))
                }
                // A result from an update that finished meanwhile is kept.
                views = views + (id to view.copy(outcome = views[id]?.outcome, running = views[id]?.running == true))
            }
        }
    }

    fun update(provider: String) {
        val current = views[provider] ?: CliUpdateView()
        if (current.running) return
        views = views + (provider to current.copy(running = true, outcome = null))
        scope.launch {
            val outcome = withContext(io) { repository.runUpdate(provider) }
            val installed = withContext(io) { repository.installedVersion(provider) }
            val latest = views[provider] ?: current
            views = views + (provider to latest.copy(running = false, outcome = outcome, installed = installed ?: latest.installed))
        }
    }
}

@Composable
internal fun rememberAgentCliState(
    repository: RuntimeStatusRepository,
    io: CoroutineDispatcher = Dispatchers.IO,
): AgentCliState {
    val scope = rememberCoroutineScope()
    return remember(repository, scope, io) { AgentCliState(repository, scope, io) }
}
