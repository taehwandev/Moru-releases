package app.thdev.moru.feature.sessions.i18n

import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.SessionActivity

/** Text of the sessions the sidebar shows under each Task and in its list of every session. */
internal interface SessionsStrings {
    val title: String
    fun summary(total: Int, working: Int, needsCheck: Int): String
    val empty: String
    val unattachedGroup: String
    val scopeAll: String
    val scopeThisProject: String
    fun exitCode(code: Int): String

    /** A plain terminal whose program runs without printing, such as a server; an agent that stops is done instead. */
    val runningProgram: String

    /** The link to a local server the session runs, named by its address so it says what it opens. */
    fun openPreview(address: String): String
    val terminal: String
    val webPreview: String
    fun activityLabel(activity: SessionActivity): String
    val permissionRequestLabel: String

    /** Explains a state that is only a guess; null when the state is certain. */
    fun activityHint(activity: SessionActivity): String?
    fun rowDescription(agent: String, title: String, activity: String): String

    /** Status board sections distinguish observed activity from a finished provider turn. */
    val boardNeedsAttention: String
    /** Finished turns that ended with a question to the user. */
    val boardAwaitingReply: String
    val awaitingReplyLabel: String
    val boardRunning: String
    val boardCompleted: String
    val boardReady: String
    val boardNothingNeedsAttention: String
    val boardNothingRunning: String
    val boardNothingCompleted: String
    val boardNothingReady: String

    /** The Results view: what needs the user's answer, then what finished, by Project. */
    val resultsAsking: String
    val resultsDone: String
    fun resultsSeen(count: Int): String
    val resultsEmpty: String
    val resultsYou: String
    val resultsTitle: String
    val resultsStatusDone: String
    val resultsStatusAsking: String
    fun resultsSummary(asking: Int, done: Int): String
    val resultsReplyPlaceholder: String
    val resultsContinuePlaceholder: String
    val resultsSend: String
    val resultsSendFailed: String
    val resultsOpen: String
    val resultsContinue: String
    val resultsMarkSeen: String
    val resultsYes: String
    val resultsNo: String
    val resultsMore: String
    val resultsLess: String
    val resultsFailed: String
    /** The evidence line: uncommitted files in the session's checkout, then the checks the agent reported. */
    fun resultsUncommitted(count: Int): String
    fun resultsChecksPassed(count: Int?): String
    fun resultsChecksFailed(count: Int?): String

    fun agentName(agent: AgentRuntime): String =
        when (agent) {
            AgentRuntime.Terminal -> terminal
            AgentRuntime.WebPreview -> webPreview
            is AgentRuntime.Named -> agent.name
        }
}
