package app.thdev.moru.core.ui.identity

import app.thdev.moru.core.designsystem.theme.MoruIdentityAccent

/** Consistent identity accent color for a Project by its id or name. */
fun projectIdentityAccent(projectIdOrName: String): MoruIdentityAccent =
    MoruIdentityAccent.entries[(projectIdOrName.hashCode() and Int.MAX_VALUE) % MoruIdentityAccent.entries.size]
