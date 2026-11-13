package app.thdev.moru.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.thdev.moru.core.model.AgentWorkflowSettings
import app.thdev.moru.core.model.AgentWorkflowSource
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.knowledge.KnowledgeDocumentIndex
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Owns cancellable loading; children render data and emit user intent. */
@Composable
internal fun KnowledgeSourcesScreen(
    workspacePath: String,
    workflows: AgentWorkflowSettings,
    scanWorkflowSources: (AgentWorkflowSettings, String) -> List<AgentWorkflowSource>,
    onWorkflowsChange: (AgentWorkflowSettings) -> Unit,
    onPickFolder: (String, String?) -> String?,
    loadDocuments: (String) -> KnowledgeDocumentIndex,
    openDocument: (String, String) -> WorkbenchFilePreview,
    openDocumentLink: (String, String, String) -> WorkbenchFilePreview,
) {
    var refresh by remember { mutableStateOf(0) }
    var selectedRoot by remember(workspacePath) { mutableStateOf<String?>(null) }
    val sourceState = key(workspacePath) { val state by produceState(KnowledgeSourcesState(), workflows, workspacePath, refresh) {
        value = value.copy(loading = true, error = null)
        value = try {
            KnowledgeSourcesState(loading = false, sources = withContext(Dispatchers.IO) { scanWorkflowSources(workflows, workspacePath) })
        } catch (error: CancellationException) { throw error }
        catch (_: Exception) { KnowledgeSourcesState(loading = false, error = "Unable to find local sources. Try Refresh.") }
    }; state }
    val selected = sourceState.sources.firstOrNull { it.path == selectedRoot }
        ?: sourceState.sources.firstOrNull { it.found }
    val root = selected?.path
    var path by remember(root) { mutableStateOf<String?>(null) }
    var link by remember(root) { mutableStateOf<Pair<String, String>?>(null) }
    val index = key(root, selected?.found, refresh) { val state by produceState<KnowledgeDocumentIndex?>(null, root, selected?.found, refresh) {
        if (root != null && selected.found) value = try { withContext(Dispatchers.IO) { loadDocuments(root) } }
        catch (error: CancellationException) { throw error }
        catch (_: Exception) { KnowledgeDocumentIndex(error = "Unable to read source documents. Try Refresh.") }
    }; state }
    val effectivePath = path ?: index?.paths?.firstOrNull()
    val document = key(root, effectivePath, link, refresh) { val state by produceState<WorkbenchFilePreview?>(null, root, effectivePath, link, refresh) {
        if (root != null && effectivePath != null) value = try { withContext(Dispatchers.IO) {
            val request = link
            if (request != null) openDocumentLink(root, request.first, request.second)
            else openDocument(root, effectivePath)
        } } catch (error: CancellationException) { throw error }
        catch (_: Exception) { WorkbenchFilePreview.unavailable(effectivePath, "Unable to read document. Try Refresh.") }
    }; state }
    KnowledgeReaderContent(
        sourceState, selected, index, document, workflows,
        onSelectSource = { selectedRoot = it.path },
        onSelectDocument = { path = it; link = null },
        onOpenLink = { target -> document?.let { link = it.path to target } },
        onRefresh = { refresh++ },
        onWorkflowsChange = onWorkflowsChange,
        onPickFolder = onPickFolder,
    )
}
