package app.thdev.moru.app

import app.thdev.moru.core.designsystem.theme.*
import app.thdev.moru.core.model.AppTheme

/** System appearance follows the device; explicit choices stay fixed. */
internal fun AppTheme.resolveColors(systemDark: Boolean): MoruColors = when (this) {
    AppTheme.System -> if (systemDark) darkCkColors else lightCkColors
    AppTheme.Light -> lightCkColors
    AppTheme.Dark -> darkCkColors
    AppTheme.FreshMint -> freshMintCkColors
    AppTheme.GitHubLight -> githubLightCkColors
    AppTheme.GitHubDark -> githubDarkCkColors
    AppTheme.CatppuccinLatte -> catppuccinLatteCkColors
    AppTheme.CatppuccinMocha -> catppuccinMochaCkColors
    AppTheme.TokyoNight -> tokyoNightCkColors
}
