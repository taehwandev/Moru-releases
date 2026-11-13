package app.thdev.moru.feature.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** Permission labels must wrap rather than hiding the scope of an approval behind ellipsis. */
@Composable
internal fun SessionPermissionChoiceButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    MoruText(text, style = MoruTheme.typography.caption,
        modifier = modifier.ckSurface(MoruSurfaceRole.Group)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp))
}
