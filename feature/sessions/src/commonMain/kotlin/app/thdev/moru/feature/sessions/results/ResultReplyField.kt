package app.thdev.moru.feature.sessions.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonSize
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruCompactTextField
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.terminal.SessionResults
import app.thdev.moru.feature.sessions.i18n.sessionsStrings

/**
 * Answers the session right on its card: Enter sends what was typed as the user's next message, the same as typing it
 * in the terminal or the Lab. [quickAnswers] are one-click answers sent the same way.
 */
@Composable
internal fun ResultReplyField(
    sessionId: String,
    placeholder: String,
    quickAnswers: List<String> = emptyList(),
    send: (sessionId: String, text: String) -> Boolean = SessionResults::send,
) {
    val strings = sessionsStrings()
    var text by remember(sessionId) { mutableStateOf("") }
    var failed by remember(sessionId) { mutableStateOf(false) }
    val submit: (String) -> Unit = { answer ->
        if (answer.isNotBlank()) {
            failed = !send(sessionId, answer)
            if (!failed) text = ""
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall)) {
        if (quickAnswers.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall)) {
            quickAnswers.forEach { answer ->
                MoruButton(answer, onClick = { submit(answer) }, type = MoruButtonType.SECONDARY, size = MoruButtonSize.Small,
                    modifier = Modifier.testTag("result-quick-answer:$sessionId:$answer"))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall),
            verticalAlignment = Alignment.CenterVertically) {
            MoruCompactTextField(
                value = text,
                onValueChange = { text = it; failed = false },
                placeholder = placeholder,
                modifier = Modifier.weight(1f).testTag("result-reply:$sessionId").onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter && !event.isShiftPressed) {
                        submit(text); true
                    } else false
                },
            )
            MoruButton(strings.resultsSend, onClick = { submit(text) }, enabled = text.isNotBlank(),
                size = MoruButtonSize.Small, modifier = Modifier.testTag("result-send:$sessionId"))
        }
        if (failed) MoruText(strings.resultsSendFailed, style = MoruTheme.typography.caption, color = MoruTheme.colors.error)
    }
}
