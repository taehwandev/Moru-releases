package app.thdev.moru.core.ui.search

import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult

/** An annotation-only transformation: links, selection characters and syntax spans are retained. */
class SearchTextPresentation(
    val text: AnnotatedString,
    val modifier: Modifier,
    val onTextLayout: (TextLayoutResult) -> Unit,
)
