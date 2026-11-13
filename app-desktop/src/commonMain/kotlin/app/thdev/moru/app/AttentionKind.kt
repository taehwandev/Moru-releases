package app.thdev.moru.app

/** Why a Task is worth interrupting the user for. */
internal enum class AttentionKind {
    /** One of its terminals failed: a program exited with an error. */
    Failed,

    /** A program on its screen asks the user a question, such as an agent asking to run a command. */
    Asking,

    /** An agent stopped printing, which for an agent that redraws while it works means its turn is over. */
    Finished,
}
