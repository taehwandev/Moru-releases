package app.thdev.moru.core.ui.markdown

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** Continuous local draft editing; the caller alone owns file writes and save feedback. */
@Composable
fun MarkdownLiveEditor(
    content: String,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    documentKey: String = "",
) {
    var value by remember(documentKey) { mutableStateOf(TextFieldValue(content)) }
    var history by remember(documentKey) { mutableStateOf(MarkdownEditHistory(value)) }
    var sourceMode by remember(documentKey) { mutableStateOf(false) }
    var editorFocused by remember(documentKey) { mutableStateOf(false) }
    var actionValue by remember(documentKey) { mutableStateOf(value) }
    var toolbarValue by remember(documentKey) { mutableStateOf<TextFieldValue?>(null) }
    var findOpen by remember(documentKey) { mutableStateOf(false) }
    var query by remember(documentKey) { mutableStateOf("") }
    var replacement by remember(documentKey) { mutableStateOf("") }
    val focus = remember(documentKey) { FocusRequester() }
    val findFocus = remember(documentKey) { FocusRequester() }
    val editorScroll = rememberScrollState()
    var textLayout by remember(documentKey) { mutableStateOf<TextLayoutResult?>(null) }
    LaunchedEffect(content) {
        if (content != value.text) {
            value = TextFieldValue(content, value.selection.let { androidx.compose.ui.text.TextRange(it.start.coerceAtMost(content.length), it.end.coerceAtMost(content.length)) })
            history = MarkdownEditHistory(value)
            actionValue = value
            toolbarValue = null
        }
    }
    fun publish(next: TextFieldValue) {
        val changed = value.text != next.text
        value = next
        actionValue = next
        toolbarValue = null
        if (changed) onContentChange(next.text)
    }
    fun edit(action: (TextFieldValue) -> TextFieldValue) {
        publish(history.accept(action(toolbarValue ?: actionValue)))
        focus.requestFocus()
    }
    val matches = remember(value.text, query) { MarkdownSearch.matches(value.text, query) }
    val activeMatch = matches.indexOf(TextRange(value.selection.min, value.selection.max))
    fun moveMatch(backwards: Boolean) {
        MarkdownSearch.next(matches, value.selection, backwards)?.let {
            publish(history.accept(value.copy(selection = it, composition = null)))
        }
    }
    fun changeQuery(next: String) {
        query = next
        val found = MarkdownSearch.matches(value.text, next)
        val match = found.firstOrNull { it.start >= value.selection.min } ?: found.firstOrNull()
        if (match != null) publish(history.accept(value.copy(selection = match, composition = null)))
    }
    fun openFind() {
        if (value.composition != null) return
        if (findOpen) { findFocus.requestFocus(); return }
        val selected = toolbarValue ?: value
        publish(history.accept(selected))
        val seed = selected.text.substring(selected.selection.min, selected.selection.max)
        if (seed.isNotEmpty() && '\n' !in seed && '\r' !in seed) query = seed
        // Ignore the text field's selection reset during the pending focus transfer.
        editorFocused = false
        findOpen = true
        changeQuery(query)
    }
    fun closeFind() { findOpen = false; focus.requestFocus() }
    val colors = MoruTheme.colors
    val liveCache = remember(documentKey) { MarkdownLiveCache() }
    val transformation = remember(value.selection, value.composition, colors.primary, sourceMode) {
        if (sourceMode) VisualTransformation.None else MarkdownLiveTransformation(value.selection, value.composition, colors.primary, liveCache)
    }
    val activeRange = matches.getOrNull(activeMatch).takeIf { findOpen }
    val displayed = remember(transformation, activeRange, colors.primary) {
        MarkdownSearchHighlight(transformation, activeRange, colors.primary)
    }
    LaunchedEffect(activeRange, textLayout) {
        val layout = textLayout
        if (activeRange != null && layout != null) {
            val transformed = displayed.filter(AnnotatedString(value.text))
            if (layout.layoutInput.text.text == transformed.text.text) {
                val offset = transformed.offsetMapping.originalToTransformed(activeRange.start)
                editorScroll.scrollTo((layout.getCursorRect(offset).top.toInt() - 24).coerceAtLeast(0))
            }
        }
    }
    val toolbarModifier = Modifier.pointerInput(documentKey) {
        awaitPointerEventScope {
            while (true) {
                if (awaitPointerEvent(PointerEventPass.Initial).type == PointerEventType.Press) toolbarValue = value
            }
        }
    }
    Column(modifier.onPreviewKeyEvent { event ->
        when {
            event.type != KeyEventType.KeyDown -> false
            event.key == Key.Escape && findOpen -> { closeFind(); true }
            (event.isMetaPressed || event.isCtrlPressed) && event.key == Key.F -> { openFind(); true }
            (event.isMetaPressed || event.isCtrlPressed) && event.key == Key.S -> { onSave(); true }
            else -> false
        }
    }) {
        Row(toolbarModifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp)) {
            MoruButton(if (sourceMode) "Live preview" else "Source", {
                publish(history.accept(toolbarValue ?: value)); sourceMode = !sourceMode; focus.requestFocus()
            }, type = MoruButtonType.GHOST)
            MoruText(
                if (sourceMode) "Markdown source" else "Live preview · edit syntax on the current line",
                modifier = Modifier.padding(8.dp), color = colors.textSecondary, style = MoruTheme.typography.caption,
            )
        }
        MarkdownEditorToolbar(
            modifier = toolbarModifier,
            onAction = { action -> edit(action) },
            onUndo = { publish(history.undo()); focus.requestFocus() },
            onRedo = { publish(history.redo()); focus.requestFocus() },
            onFind = ::openFind,
        )
        if (findOpen) MarkdownSearchBar(
            query = query, onQueryChange = ::changeQuery,
            replacement = replacement, onReplacementChange = { replacement = it },
            matchCount = matches.size, activeMatch = activeMatch, focus = findFocus,
            onMove = ::moveMatch,
            onReplace = { publish(history.accept(MarkdownSearch.replaceCurrent(value, query, replacement))) },
            onReplaceAll = { publish(history.accept(MarkdownSearch.replaceAll(value, query, replacement))) },
            onClose = ::closeFind,
        )
        Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(editorScroll).padding(16.dp)) {
            BasicTextField(
                value = value,
                onValueChange = {
                    // Clicking a formatting action must retain the last editing selection.
                    if ((editorFocused && toolbarValue == null) || it.text != value.text || it.composition != value.composition) publish(history.accept(it))
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp)
                    .testTag("markdown-live-editor").semantics { contentDescription = "Markdown editor" }
                    .focusRequester(focus).onFocusChanged { editorFocused = it.isFocused; if (it.isFocused) toolbarValue = null }.onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && !(event.isMetaPressed || event.isCtrlPressed || event.isAltPressed) &&
                            (event.key == Key.Tab || (event.key == Key.Enter && !event.isShiftPressed))) {
                            val next = if (event.key == Key.Tab) MarkdownListEditing.indent(value, event.isShiftPressed) else MarkdownListEditing.enter(value)
                            if (next != null) { publish(history.accept(next)); true }
                            else { if (event.key == Key.Tab) toolbarValue = value; false }
                        }
                        else if (event.type != KeyEventType.KeyDown || !(event.isMetaPressed || event.isCtrlPressed)) false
                        else when (event.key) {
                            Key.S -> { onSave(); true }
                            Key.B -> { edit { MarkdownEditingActions.wrap(it, "**") }; true }
                            Key.I -> { edit { MarkdownEditingActions.wrap(it, "*") }; true }
                            Key.K -> { edit(MarkdownEditingActions::link); true }
                            Key.Z -> { publish(if (event.isShiftPressed) history.redo() else history.undo()); true }
                            Key.Y -> { publish(history.redo()); true }
                            else -> false
                        }
                    },
                textStyle = (if (sourceMode) MoruTheme.typography.monospace else MoruTheme.typography.body).copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.primary),
                visualTransformation = displayed,
                onTextLayout = { textLayout = it },
            )
        }
    }
}
