package app.thdev.moru.core.model.session

/**
 * One exchange of a session: what the user asked ([ask], null when it could not be read) and the text the agent
 * wrote back in that turn.
 */
data class ConversationTurn(val ask: String?, val answer: String)
