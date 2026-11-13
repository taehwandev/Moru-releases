package app.thdev.moru.feature.git.components.branches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** One local tag, one compact line; a right click opens [menu]. */
@Composable
internal fun GitTagRow(name: String, menu: () -> List<GitRefMenuItem>) {
    GitRefContextMenuArea(menu) {
        Row(
            Modifier.fillMaxWidth().height(24.dp).testTag("git-tag-$name")
                .semantics { contentDescription = "Tag $name" }
                .padding(start = 20.dp, end = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MoruIcon(MoruIconName.Tag, size = 13.dp, tint = MoruTheme.colors.textSecondary)
            MoruText(name, Modifier.weight(1f), color = MoruTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
