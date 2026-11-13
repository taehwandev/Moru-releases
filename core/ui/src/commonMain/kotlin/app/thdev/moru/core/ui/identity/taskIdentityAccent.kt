package app.thdev.moru.core.ui.identity

import app.thdev.moru.core.designsystem.theme.MoruIdentityAccent

/** Follows the Task across rename, reorder, project filtering, and session activity changes. */
fun taskIdentityAccent(taskId: String): MoruIdentityAccent =
    MoruIdentityAccent.entries[(taskId.hashCode() and Int.MAX_VALUE) % MoruIdentityAccent.entries.size]
