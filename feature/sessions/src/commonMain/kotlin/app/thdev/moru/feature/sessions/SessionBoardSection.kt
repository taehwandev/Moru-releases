package app.thdev.moru.feature.sessions

/**
 * The columns of the status board: sessions doing something now, and every other open session, which waits for
 * the user in some way (an answer, a finished turn, a prompt, an ending).
 */
enum class SessionBoardSection { NeedsAttention, AwaitingReply, Running, Completed, Ready }
