package app.thdev.moru.feature.sessions.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.sessions.SessionPermissionActions
import app.thdev.moru.feature.sessions.i18n.sessionsStrings

/**
 * Where the user answers, under the exchanges: a permission request's own choices, the agent's [question] with a
 * reply box (and Yes/No when that is all it asks), a failure's last words, or, for a finished result, the next
 * instruction.
 */
@Composable
internal fun ResultComposer(item: ResultItem, question: String?) {
    val strings = sessionsStrings()
    val row = item.row
    Box(
        Modifier.fillMaxWidth().padding(horizontal = MoruTheme.spacing.xLarge, vertical = MoruTheme.spacing.large)
            .testTag("result-composer:${row.sessionId}"),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.widthIn(max = ResultReadingWidth).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small)) {
            when (item.kind) {
                ResultKind.Permission -> SessionPermissionActions(row.sessionId)
                ResultKind.Failed -> MoruText(row.lastLine ?: strings.resultsFailed, style = MoruTheme.typography.body,
                    color = MoruTheme.colors.error)
                ResultKind.Question -> {
                    question?.let { MoruText(it, style = MoruTheme.typography.body.copy(fontWeight = FontWeight.SemiBold)) }
                    ResultReplyField(
                        sessionId = row.sessionId,
                        placeholder = strings.resultsReplyPlaceholder,
                        quickAnswers = if (question != null && isYesNoQuestion(question)) listOf(strings.resultsYes, strings.resultsNo) else emptyList(),
                    )
                }
                ResultKind.Done -> ResultReplyField(row.sessionId, strings.resultsContinuePlaceholder)
            }
        }
    }
}
