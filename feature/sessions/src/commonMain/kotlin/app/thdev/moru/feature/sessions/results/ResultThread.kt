package app.thdev.moru.feature.sessions.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.session.ConversationTurn
import app.thdev.moru.feature.sessions.i18n.sessionsStrings
import kotlin.math.roundToInt

/**
 * The exchanges that led to the result, oldest first, in one reading column. The page opens at the start of the
 * newest exchange, so the result is what the user reads first and what came before is a scroll up.
 */
@Composable
internal fun ResultThread(item: ResultItem, turns: List<ConversationTurn>, modifier: Modifier = Modifier) {
    val agent = sessionsStrings().agentName(item.row.agent)
    val scroll = rememberScrollState()
    var latestTop by remember { mutableStateOf<Int?>(null) }
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(latestTop) {
        val top = latestTop ?: return@LaunchedEffect
        if (!placed) { scroll.scrollTo(top); placed = true }
    }
    Column(
        modifier.fillMaxSize().verticalScroll(scroll)
            .padding(horizontal = MoruTheme.spacing.xLarge, vertical = MoruTheme.spacing.large)
            .testTag("result-thread:${item.row.sessionId}"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.widthIn(max = ResultReadingWidth).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xLarge),
        ) {
            turns.forEachIndexed { index, turn ->
                val latest = index == turns.lastIndex
                key(index) {
                    ResultTurn(turn, agent, latest,
                        Modifier.then(if (latest && index > 0) Modifier.onPlaced { latestTop = it.positionInParent().y.roundToInt() } else Modifier))
                }
            }
        }
    }
}
