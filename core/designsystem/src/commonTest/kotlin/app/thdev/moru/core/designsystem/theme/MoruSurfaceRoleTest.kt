package app.thdev.moru.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class MoruSurfaceRoleTest {
    @Test
    fun panelRoleMapsToBaseSurfaceWithoutRadius() {
        val style = MoruSurfaceRole.Panel.resolveCkSurfaceStyle(darkCkColors)

        assertEquals(darkCkColors.surface, style.background)
        assertEquals(darkCkColors.textPrimary, style.content)
        assertEquals(darkCkColors.border, style.border)
        assertEquals(defaultCkStroke.thin, style.borderWidth)
        assertEquals(defaultCkRadius.none, style.radius)
    }

    @Test
    fun selectedRolesMapToCurrentSelectionTreatment() {
        val card = MoruSurfaceRole.CardSelected.resolveCkSurfaceStyle(darkCkColors)
        val row = MoruSurfaceRole.RowSelected.resolveCkSurfaceStyle(darkCkColors)

        assertEquals(darkCkColors.primary.copy(alpha = 0.14f), card.background)
        assertEquals(darkCkColors.primary.copy(alpha = 0.45f), card.border)
        assertEquals(darkCkColors.primary.copy(alpha = 0.22f), row.background)
        assertEquals(Color.Transparent, row.border)
        assertEquals(defaultCkStroke.none, row.borderWidth)
    }

    @Test
    fun warningRoleMapsToWarningTintAndWarningBorder() {
        val style = MoruSurfaceRole.Warning.resolveCkSurfaceStyle(darkCkColors)

        assertEquals(darkCkColors.warning.copy(alpha = 0.08f), style.background)
        assertEquals(darkCkColors.warning, style.content)
        assertEquals(darkCkColors.warning.copy(alpha = 0.35f), style.border)
        assertEquals(defaultCkRadius.medium, style.radius)
    }

    @Test
    fun successRoleMapsToSuccessTintAndSuccessBorder() {
        val style = MoruSurfaceRole.Success.resolveCkSurfaceStyle(darkCkColors)

        assertEquals(darkCkColors.success.copy(alpha = 0.08f), style.background)
        assertEquals(darkCkColors.success, style.content)
        assertEquals(darkCkColors.success.copy(alpha = 0.35f), style.border)
        assertEquals(defaultCkRadius.medium, style.radius)
    }

    @Test
    fun roleSpecificRawColorsStayInsideDesignSystemMapping() {
        val darkCode = MoruSurfaceRole.Code.resolveCkSurfaceStyle(darkCkColors)
        val darkTerminal = MoruSurfaceRole.Terminal.resolveCkSurfaceStyle(darkCkColors)
        val darkTerminalSelected = MoruSurfaceRole.TerminalSelected.resolveCkSurfaceStyle(darkCkColors)
        val darkControl = MoruSurfaceRole.Control.resolveCkSurfaceStyle(darkCkColors)

        assertEquals(darkCkColors.background, darkCode.background)
        assertEquals(darkCkColors.background, darkTerminal.background)
        assertEquals(darkCkColors.textPrimary, darkTerminal.content)
        assertEquals(darkCkColors.background, darkTerminalSelected.background)
        assertEquals(darkCkColors.primary.copy(alpha = 0.45f), darkTerminalSelected.border)
        assertEquals(darkCkColors.background, darkControl.background)
        assertEquals(defaultCkRadius.small, darkControl.radius)

        val lightTerminalSelected = MoruSurfaceRole.TerminalSelected.resolveCkSurfaceStyle(lightCkColors)
        assertEquals(lightCkColors.primary.copy(alpha = 0.08f), lightTerminalSelected.background)
        assertEquals(lightCkColors.textPrimary, lightTerminalSelected.content)
        assertEquals(lightCkColors.primary.copy(alpha = 0.45f), lightTerminalSelected.border)
    }
}
