package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruGlassRole
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.LocalMoruNativeMaterial
import app.thdev.moru.core.designsystem.theme.resolveGlassStyle

/** Native material shows through the foreground; windowless and other-platform surfaces use theme rendering. */
@Composable
fun Modifier.moruGlassSurface(role: MoruGlassRole = MoruGlassRole.Card, shadow: Boolean = true): Modifier {
    if (MoruTheme.materialStyle == MoruMaterialStyle.Standard) {
        if (role == MoruGlassRole.Sheet) {
            val shape = RoundedCornerShape(MoruTheme.radius.medium)
            return clip(shape).background(MoruTheme.colors.surface)
                .border(MoruTheme.stroke.thin, MoruTheme.colors.border, shape)
        }
        return ckSurface(when (role) {
            MoruGlassRole.Navigation -> MoruSurfaceRole.Panel
            MoruGlassRole.Card, MoruGlassRole.Sheet -> MoruSurfaceRole.Card
            MoruGlassRole.SelectedCard -> MoruSurfaceRole.CardSelected
            MoruGlassRole.Control -> MoruSurfaceRole.Control
        })
    }
    val style = role.resolveGlassStyle(MoruTheme.colors)
    val shape = RoundedCornerShape(style.radius)
    if (LocalMoruNativeMaterial.current) {
        val transparent = role == MoruGlassRole.Navigation || role == MoruGlassRole.Sheet
        val fill = if (transparent) Color.Transparent else MoruTheme.colors.surface.copy(alpha = 0.22f)
        val edge = if (role == MoruGlassRole.Sheet) Color.Transparent else style.lowerRim.copy(alpha = 0.25f)
        return clip(shape).background(fill).border(0.5.dp, edge, shape)
    }
    return shadow(if (shadow) style.elevation else 0.dp, shape, clip = false)
        .clip(shape)
        // A separate popup has no installed material. Its backing must not bleed through the sheet.
        .background(if (role == MoruGlassRole.Sheet) MoruTheme.colors.surface else style.fill)
        .background(Brush.verticalGradient(listOf(style.tint, Color.Transparent)))
        .border(0.75.dp, Brush.verticalGradient(listOf(style.rim, style.lowerRim)), shape)
}
