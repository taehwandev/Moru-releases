package app.thdev.moru.feature.sessions.i18n

import app.thdev.moru.core.model.session.SessionActivity

internal object EnglishSessionsStrings : SessionsStrings {
    override val title = "Sessions"
    override fun summary(total: Int, working: Int, needsCheck: Int) =
        (if (total == 1) "1 session" else "$total sessions") + " · $working working · $needsCheck to check"
    override val empty = "No sessions are running."
    override val unattachedGroup = "Sessions without a Task"
    override val scopeAll = "All projects"
    override val scopeThisProject = "This project"
    override fun exitCode(code: Int) = "Exit code $code"
    override val runningProgram = "Running"
    override fun openPreview(address: String) = "Preview $address"
    override val terminal = "Terminal"
    override val webPreview = "Web preview"
    override fun activityLabel(activity: SessionActivity) = when (activity) {
        SessionActivity.Starting -> "Starting"
        SessionActivity.Working -> "Working"
        SessionActivity.Asking -> "Needs answer"
        SessionActivity.Quiet -> "Response ready"
        SessionActivity.Waiting -> "Waiting"
        SessionActivity.Idle -> "Shell idle"
        SessionActivity.Exited -> "Ended"
        SessionActivity.Failed -> "Failed"
    }
    override val permissionRequestLabel = "Needs approval"
    override fun activityHint(activity: SessionActivity): String? = when (activity) {
        SessionActivity.Asking -> "It is waiting for your answer."
        SessionActivity.Quiet -> "The agent stopped printing; its turn may be finished, but the task outcome is unknown."
        SessionActivity.Waiting -> "The agent finished a while ago and waits for your next instruction."
        SessionActivity.Idle -> "The shell is at its prompt; nothing is running."
        SessionActivity.Exited -> "It exited cleanly, but whether the goal was met is unknown."
        SessionActivity.Starting, SessionActivity.Working, SessionActivity.Failed -> null
    }
    override fun rowDescription(agent: String, title: String, activity: String) = "$agent, $title, $activity"
    override val boardNeedsAttention = "Needs attention"
    override val boardAwaitingReply = "Awaiting your reply"
    override val awaitingReplyLabel = "Awaiting reply"
    override val boardRunning = "Running"
    override val boardCompleted = "Response ready"
    override val boardReady = "Waiting"
    override val boardNothingNeedsAttention = "Nothing needs attention."
    override val boardNothingRunning = "Nothing is running."
    override val boardNothingCompleted = "No finished turns yet."
    override val boardNothingReady = "No sessions are ready."
    override val resultsAsking = "Needs your answer"
    override val resultsDone = "Finished"
    override fun resultsSeen(count: Int) = "Seen · $count"
    override val resultsEmpty = "Nothing has finished yet. Results gather here when an agent completes its work."
    override val resultsYou = "You"
    override val resultsTitle = "Moru"
    override val resultsStatusDone = "Done"
    override val resultsStatusAsking = "Waiting for you"
    override fun resultsSummary(asking: Int, done: Int) = listOfNotNull(
        "$asking to answer".takeIf { asking > 0 }, "$done done".takeIf { done > 0 },
    ).joinToString(" · ")
    override val resultsReplyPlaceholder = "Type an answer, press Enter"
    override val resultsContinuePlaceholder = "What next? Press Enter"
    override val resultsSend = "Send"
    override val resultsSendFailed = "Could not send. Open the session to check it."
    override val resultsOpen = "Open"
    override val resultsContinue = "Continue"
    override val resultsMarkSeen = "Done"
    override val resultsYes = "Yes"
    override val resultsNo = "No"
    override val resultsMore = "Show more"
    override val resultsLess = "Show less"
    override val resultsFailed = "Failed"
    override fun resultsUncommitted(count: Int) = if (count == 1) "1 uncommitted file" else "$count uncommitted files"
    override fun resultsChecksPassed(count: Int?) = if (count == null) "Checks passed" else "$count tests passed"
    override fun resultsChecksFailed(count: Int?) = if (count == null) "Checks failed" else "$count failed"
}
