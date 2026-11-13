package app.thdev.moru.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/** Stable provider identity shared by Workbench and Lab, independent of capacity warnings. */
fun moruProviderAccent(providerId: String, colors: MoruColors): Color = when (providerId.lowercase()) {
    "claude" -> MoruIdentityAccent.Fourth.color(colors)
    "codex" -> MoruIdentityAccent.Fifth.color(colors)
    "antigravity", "agy" -> MoruIdentityAccent.Second.color(colors)
    else -> colors.textSecondary
}
