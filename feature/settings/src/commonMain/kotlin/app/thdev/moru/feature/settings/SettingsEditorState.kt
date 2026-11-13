package app.thdev.moru.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.thdev.moru.core.domain.SettingsRepository
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.effectiveScrollbackLines
import app.thdev.moru.core.model.effectiveTerminalFontFamily
import app.thdev.moru.core.model.effectiveTerminalFontSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The app owns this editor so leaving Settings cannot cancel a pending save. Each change is kept as an edit of the
 * one field the user touched and replayed onto the latest stored settings, so a stale view of the file can never
 * write back fields the user did not change.
 */
class SettingsEditorState internal constructor(
    private val repository: SettingsRepository,
    scope: CoroutineScope,
    private val writeDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val onSettingsSaved: (AppSettings) -> Unit,
) {
    var settings by mutableStateOf(repository.loadSettings())
        private set
    var saveStatus by mutableStateOf(SettingsSaveStatus.Ready)
        private set
    var saveError by mutableStateOf<String?>(null)
        private set
    var fontSizeText by mutableStateOf(settings.effectiveTerminalFontSize.toString())
        private set
    var uiFontSizeText by mutableStateOf(settings.appearance.uiFontSize.toString())
        private set
    var scrollbackText by mutableStateOf(settings.effectiveScrollbackLines.toString())
        private set
    val hasInvalidNumbers: Boolean
        get() = fontSizeText.validSettingsNumber(10..28) == null ||
            uiFontSizeText.validSettingsNumber(10..28) == null || scrollbackText.validSettingsNumber(500..50000) == null

    private var revision = 0L
    private var edits = listOf<SettingsEdit>()
    private val pending = Channel<SaveRequest>(Channel.CONFLATED)

    init {
        scope.launch {
            for (request in pending) save(request)
        }
    }

    fun change(edit: (AppSettings) -> AppSettings) {
        val next = edit(settings)
        if (next == settings) return
        if (next.effectiveTerminalFontSize != settings.effectiveTerminalFontSize) {
            fontSizeText = next.effectiveTerminalFontSize.toString()
        }
        if (next.appearance.uiFontSize != settings.appearance.uiFontSize) {
            uiFontSizeText = next.appearance.uiFontSize.toString()
        }
        if (next.effectiveScrollbackLines != settings.effectiveScrollbackLines) {
            scrollbackText = next.effectiveScrollbackLines.toString()
        }
        settings = next
        revision += 1
        edits = edits + SettingsEdit(revision, edit)
        enqueue()
    }

    fun retry() {
        if (saveStatus == SettingsSaveStatus.Failed) enqueue()
    }

    /**
     * Shows the stored settings again when nothing is waiting to be saved, dropping drafts that were never valid.
     * A save confirmation from an earlier visit, or from a change made outside Settings, is cleared too.
     */
    fun refresh() {
        if (edits.isNotEmpty()) return
        saveStatus = SettingsSaveStatus.Ready
        settings = repository.loadSettings()
        fontSizeText = settings.effectiveTerminalFontSize.toString()
        uiFontSizeText = settings.appearance.uiFontSize.toString()
        scrollbackText = settings.effectiveScrollbackLines.toString()
        saveError = null
    }

    fun changeFontSize(text: String) {
        fontSizeText = text
        text.validSettingsNumber(10..28)?.let { size ->
            change { it.withTerminalFont(it.effectiveTerminalFontFamily, size) }
        }
    }

    fun changeScrollback(text: String) {
        scrollbackText = text
        text.validSettingsNumber(500..50000)?.let { lines ->
            change { it.withScrollback(lines) }
        }
    }

    fun changeUiFontSize(text: String) {
        uiFontSizeText = text
        text.validSettingsNumber(10..28)?.let { size ->
            change { it.copy(appearance = it.appearance.copy(uiFontSize = size)) }
        }
    }

    private fun enqueue() {
        saveStatus = SettingsSaveStatus.Saving
        saveError = null
        pending.trySend(SaveRequest(revision, edits))
    }

    private suspend fun save(request: SaveRequest) {
        val saved = try {
            withContext(writeDispatcher) {
                repository.updateSettings { latest -> request.edits.fold(latest) { current, edit -> edit.apply(current) } }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (request.revision == revision) {
                saveError = error.message?.takeIf { it.isNotBlank() }
                saveStatus = SettingsSaveStatus.Failed
            }
            return
        }
        edits = edits.filter { it.revision > request.revision }
        if (request.revision == revision) {
            settings = saved
            saveStatus = SettingsSaveStatus.Saved
            onSettingsSaved(saved)
        }
    }
}

private class SettingsEdit(val revision: Long, val apply: (AppSettings) -> AppSettings)

private data class SaveRequest(val revision: Long, val edits: List<SettingsEdit>)
