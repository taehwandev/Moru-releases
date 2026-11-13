package app.thdev.moru.core.model.session

/**
 * What a terminal session looks like from the outside, using only what any agent exposes:
 * its process and its output. [Quiet] is a guess, not a fact: the agent may be thinking,
 * waiting for input, or stuck, and the terminal alone cannot tell which. [Idle] is a shell at its prompt with
 * nothing running in it: there is nothing to wait for, so it never asks for attention. [Asking] is not a guess: the
 * screen shows a question with choices, such as an agent asking to run a command or edit a file. [Waiting] is an agent
 * whose turn ended a while ago: still running, waiting for the user's next instruction, no longer news.
 */
enum class SessionActivity {
    Starting,
    Working,
    Asking,
    Quiet,
    Waiting,
    Idle,
    Exited,
    Failed,
}
