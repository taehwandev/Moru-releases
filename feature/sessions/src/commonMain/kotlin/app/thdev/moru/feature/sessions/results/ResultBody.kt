package app.thdev.moru.feature.sessions.results

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonSize
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.ui.markdown.MarkdownReadingView
import app.thdev.moru.feature.sessions.i18n.sessionsStrings

/**
 * The agent's own words, drawn as the Markdown it wrote. A long result shows its opening until the user asks for the
 * rest, so one report never pushes every other result out of sight.
 */
@Composable
internal fun ResultBody(text: String) {
    val strings = sessionsStrings()
    var expanded by remember(text) { mutableStateOf(false) }
    val long = text.length > CollapseChars || text.lines().size > CollapseLines
    Column(Modifier.fillMaxWidth()) {
        MarkdownReadingView(
            content = remember(text) { text.withSmallHeadings() },
            onOpenWikiLink = {},
            modifier = Modifier.fillMaxWidth()
                .then(if (long && !expanded) Modifier.heightIn(max = CollapsedHeight).clipToBounds() else Modifier),
            padding = MoruTheme.spacing.none,
        )
        if (long) MoruButton(if (expanded) strings.resultsLess else strings.resultsMore, onClick = { expanded = !expanded },
            type = MoruButtonType.GHOST, size = MoruButtonSize.ExtraSmall)
    }
}

private const val CollapseChars = 360
private const val CollapseLines = 8
private val CollapsedHeight = 140.dp
