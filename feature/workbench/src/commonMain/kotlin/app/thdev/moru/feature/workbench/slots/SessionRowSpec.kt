package app.thdev.moru.feature.workbench.slots

import androidx.compose.ui.unit.Dp
import app.thdev.moru.core.designsystem.theme.MoruIdentityAccent
import app.thdev.moru.core.model.session.AgentSessionRow

/** One session drawn under its Task: the row, what a press does, how far it sits in, its Task's accent and its page. */
data class SessionRowSpec(
    val row: AgentSessionRow,
    val onClick: () -> Unit,
    val indent: Dp,
    val accent: MoruIdentityAccent?,
    val onOpenPreview: (sessionId: String, url: String) -> Unit,
)
