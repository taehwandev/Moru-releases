package app.thdev.moru.feature.sessions.results

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.sessions.i18n.sessionsStrings

/**
 * What the user asked, set to the right on a soft tint the way a sent message reads. A long request shows its
 * opening until it is clicked.
 */
@Composable
internal fun ResultAskBubble(text: String) {
    val strings = sessionsStrings()
    var expanded by remember(text) { mutableStateOf(false) }
    val shape = RoundedCornerShape(MoruTheme.radius.large)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Column(
            Modifier.widthIn(max = BubbleWidth).clip(shape)
                .background(MoruTheme.colors.primary.copy(alpha = 0.10f))
                .clickable { expanded = !expanded }
                .padding(horizontal = MoruTheme.spacing.medium, vertical = MoruTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall),
        ) {
            MoruText(strings.resultsYou, style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                color = MoruTheme.colors.primary)
            MoruText(text, style = MoruTheme.typography.body, maxLines = if (expanded) Int.MAX_VALUE else CollapsedLines,
                overflow = TextOverflow.Ellipsis)
        }
    }
}

private val BubbleWidth = 560.dp
private const val CollapsedLines = 6
