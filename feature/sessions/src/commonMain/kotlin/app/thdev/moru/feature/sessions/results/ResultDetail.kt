package app.thdev.moru.feature.sessions.results

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.thdev.moru.core.designsystem.component.MoruDivider
import app.thdev.moru.core.model.session.ConversationTurn

/**
 * One result read in full: what it is about and its evidence, then the exchanges that led to it, newest last, then
 * the answer or next instruction typed right here. A question the composer asks again is not repeated at the end of
 * the agent's message.
 */
@Composable
internal fun ResultDetail(
    item: ResultItem,
    changedFiles: Int?,
    thread: List<ConversationTurn>,
    onOpen: () -> Unit,
    onSeen: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val question = (item.row.replyQuestion ?: item.row.question).takeIf { item.kind == ResultKind.Question }
    val turns = remember(thread, item.text, question) {
        resultTurns(thread, item.text).let { all ->
            val last = all.lastOrNull() ?: return@let all
            if (question == null) all else all.dropLast(1) + last.copy(answer = last.answer.withoutTrailingQuestion(question))
        }
    }
    Column(modifier.fillMaxHeight().testTag("result-detail:${item.row.sessionId}")) {
        ResultDetailHeader(item, changedFiles, onOpen, onSeen)
        MoruDivider()
        ResultThread(item, turns, Modifier.weight(1f))
        MoruDivider()
        ResultComposer(item, question)
    }
}
