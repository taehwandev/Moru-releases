package app.thdev.moru.feature.git.components.branches

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

/**
 * A ref section's header, like a Git client's sidebar: a fold chevron, the section's icon and name, an optional
 * [trailing] control such as New branch, and its count. Clicking the header folds or unfolds the section.
 */
@Composable
internal fun GitRefSectionHeader(
    title: String,
    count: String,
    icon: MoruIconName,
    expanded: Boolean = true,
    onToggle: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(26.dp)
            .testTag("git-section-$title")
            .then(if (onToggle == null) Modifier else Modifier.clickable(role = Role.Button, onClick = onToggle))
            .semantics {
                heading()
                stateDescription = if (expanded) "Expanded" else "Collapsed"
            }
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoruIcon(if (expanded) MoruIconName.ChevronDown else MoruIconName.ChevronRight, size = 12.dp,
            tint = MoruTheme.colors.textSecondary)
        MoruIcon(icon, size = 13.dp, tint = MoruTheme.colors.textSecondary)
        MoruText(
            text = title.uppercase(),
            modifier = Modifier.weight(1f),
            style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
            color = MoruTheme.colors.textSecondary,
            maxLines = 1,
        )
        trailing?.invoke()
        MoruText(count, style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary, maxLines = 1)
    }
}
