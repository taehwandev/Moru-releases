package app.thdev.moru.feature.sessions.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.session.ConversationTurn

/** One exchange: what the user asked, then what [agent] wrote back; only the [latest] answer is drawn whole at once. */
@Composable
internal fun ResultTurn(turn: ConversationTurn, agent: String, latest: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.medium)) {
        turn.ask?.let { ResultAskBubble(it) }
        ResultAnswer(turn.answer, agent, latest)
    }
}
