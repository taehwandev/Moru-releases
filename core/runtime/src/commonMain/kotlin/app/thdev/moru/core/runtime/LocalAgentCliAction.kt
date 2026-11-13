package app.thdev.moru.core.runtime

/** Local CLI commands intercepted before a Lab message can reach a model. */
enum class LocalAgentCliAction(val commandName: String) {
    Login("login"),
    Logout("logout"),
    Status("status"),
    Usage("usage"),
    Clear("clear"),
}
