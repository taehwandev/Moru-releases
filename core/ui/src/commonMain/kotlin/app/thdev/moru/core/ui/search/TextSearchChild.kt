package app.thdev.moru.core.ui.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/** With no active search this adds no provider and leaves the original reader unchanged. */
@Composable
fun TextSearchChild(key: String, content: @Composable () -> Unit) {
    val parent = LocalTextSearchScope.current
    if (parent == null) content()
    else CompositionLocalProvider(LocalTextSearchScope provides parent.child(key), content = content)
}
