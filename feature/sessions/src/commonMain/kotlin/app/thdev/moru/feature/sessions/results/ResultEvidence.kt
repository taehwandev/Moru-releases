package app.thdev.moru.feature.sessions.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.thdev.moru.core.designsystem.component.MoruBadge
import app.thdev.moru.core.designsystem.component.MoruBadgeType
import app.thdev.moru.feature.sessions.i18n.sessionsStrings
import app.thdev.moru.core.designsystem.theme.MoruTheme

/**
 * What changed and what proves it, ahead of the report: uncommitted files in the session's checkout ([changedFiles])
 * and the checks the report says ran. Draws nothing when neither is known; a checkout with nothing uncommitted is not
 * evidence of anything.
 */
@Composable
internal fun ResultEvidence(sessionId: String, text: String?, changedFiles: Int?) {
    val strings = sessionsStrings()
    val check = remember(text) { text?.let(::resultCheck) }
    val changes = changedFiles?.takeIf { it > 0 }
    if (check == null && changes == null) return
    Row(Modifier.testTag("result-evidence:$sessionId"), horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall)) {
        changes?.let { MoruBadge(strings.resultsUncommitted(it), type = MoruBadgeType.INFO) }
        check?.let {
            if (it.passed) MoruBadge(strings.resultsChecksPassed(it.count), type = MoruBadgeType.SUCCESS)
            else MoruBadge(strings.resultsChecksFailed(it.count), type = MoruBadgeType.ERROR)
        }
    }
}
