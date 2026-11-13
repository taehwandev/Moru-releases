package app.thdev.moru.app

/**
 * What quitting Moru does to the terminals. With Remember Last State on they are only let go and keep running in the
 * terminal host for the next launch. With it off they end, but when any is running the user is asked first and may
 * keep them that one time.
 */
internal enum class QuitPlan { Detach, EndAll, Ask }

internal fun quitPlan(rememberLastState: Boolean, runningTerminals: Int): QuitPlan = when {
    rememberLastState -> QuitPlan.Detach
    runningTerminals > 0 -> QuitPlan.Ask
    else -> QuitPlan.EndAll
}
