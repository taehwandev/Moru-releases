package app.thdev.moru.feature.sessions.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.ui.markdown.MarkdownReadingView

/**
 * What [agent] wrote back, as the Markdown it wrote, headings included, on the page itself rather than in a box. The [latest] answer is
 * the result and reads whole; earlier ones show their opening until asked for the rest.
 */
@Composable
internal fun ResultAnswer(text: String, agent: String, latest: Boolean) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall)) {
        MoruText(agent, style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = MoruTheme.colors.textSecondary)
        if (latest) {
            MarkdownReadingView(
                content = text,
                onOpenWikiLink = {},
                modifier = Modifier.fillMaxWidth(),
                padding = MoruTheme.spacing.none,
            )
        } else ResultBody(text)
    }
}
