package app.thdev.moru.app

import app.thdev.moru.app.i18n.AppStrings

/** One line naming the Task and why it needs the user; the notification says the same. */
internal fun AppStrings.attentionMessage(candidate: AttentionCandidate): String =
    when (candidate.kind) {
        AttentionKind.Failed -> attentionFailed(candidate.title)
        AttentionKind.Asking -> attentionAsking(candidate.title, candidate.agent, candidate.question.orEmpty())
        AttentionKind.Finished -> attentionFinished(candidate.title, candidate.agent.orEmpty())
    }
