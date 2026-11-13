package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme

/**
 * What a pane says while it has nothing to show: the pane's own [icon] over one centred note, so an empty pane
 * explains itself instead of looking broken, and every pane's empty state looks the same.
 */
@Composable
fun MoruEmptyState(
    text: String,
    modifier: Modifier = Modifier,
    icon: MoruIconName? = null,
) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            icon?.let { MoruIcon(name = it, size = 22.dp) }
            MoruText(
                text = text,
                style = MoruTheme.typography.body.copy(textAlign = TextAlign.Center),
                color = MoruTheme.colors.textSecondary,
            )
        }
    }
}
