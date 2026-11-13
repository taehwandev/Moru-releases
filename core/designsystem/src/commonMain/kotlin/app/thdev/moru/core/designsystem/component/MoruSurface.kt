package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruGlassRole
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import app.thdev.moru.core.designsystem.theme.MoruTheme

@Composable
fun Modifier.ckSurface(role: MoruSurfaceRole): Modifier {
    // Terminals stay solid in Apple style: a see-through backdrop behind terminal text reads worse, not better.
    if (MoruTheme.materialStyle == MoruMaterialStyle.Apple) when (role) {
        MoruSurfaceRole.Page -> return moruAmbientBackground()
        MoruSurfaceRole.Panel -> return moruGlassSurface(MoruGlassRole.Navigation)
        MoruSurfaceRole.Card, MoruSurfaceRole.Code -> return moruGlassSurface(MoruGlassRole.Card)
        MoruSurfaceRole.CardSelected -> return moruGlassSurface(MoruGlassRole.SelectedCard)
        MoruSurfaceRole.Control -> return moruGlassSurface(MoruGlassRole.Control)
        else -> Unit
    }
    val style = MoruTheme.surfaceStyle(role)
    val shape = RoundedCornerShape(style.radius)
    return clip(shape)
        .background(style.background)
        .border(style.borderWidth, style.border, shape)
}

@Composable
fun MoruSurface(
    role: MoruSurfaceRole,
    modifier: Modifier = Modifier,
    padding: Dp? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .ckSurface(role)
            .padding(padding ?: MoruTheme.spacing.medium),
        content = content,
    )
}

@Composable
fun MoruSurfaceColumn(
    role: MoruSurfaceRole,
    modifier: Modifier = Modifier,
    padding: Dp? = null,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .ckSurface(role)
            .padding(padding ?: MoruTheme.spacing.medium),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}
